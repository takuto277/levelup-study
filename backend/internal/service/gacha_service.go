package service

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

// ============================================================
// GachaService — ガチャ実行のビジネスロジック
//
// 確率計算・天井判定・通貨消費・キャラ/武器付与を一括で行う。
// チート対策のため、全ロジックはサーバー側で完結する。
// ============================================================

// GachaPullRequest — ガチャ実行リクエスト
type GachaPullRequest struct {
	BannerID uuid.UUID `json:"banner_id"`
	Count    int       `json:"count"` // 1（単発）or 10（10連）
}

// GachaPullResult — ガチャ1回分の結果
type GachaPullResult struct {
	ResultType string    `json:"result_type"` // character / weapon
	ItemID     uuid.UUID `json:"item_id"`     // マスタID
	Name       string    `json:"name"`
	Rarity     int       `json:"rarity"`
	IsNew      bool      `json:"is_new"`     // 新規入手か
	PityCount  int       `json:"pity_count"` // 天井カウント
}

// GachaPullResponse — ガチャ実行レスポンス
type GachaPullResponse struct {
	Results         []GachaPullResult `json:"results"`
	StonesSpent     int               `json:"stones_spent"`
	RemainingStones int               `json:"remaining_stones"`
	UpdatedUser     *model.User       `json:"updated_user"`
}

// RateTableEntry — 排出テーブルの1エントリ
type RateTableEntry struct {
	ItemID     uuid.UUID `json:"item_id"`
	ResultType string    `json:"result_type"` // character / weapon
	Rarity     int       `json:"rarity"`
	Rate       float64   `json:"rate"` // 0.0 〜 1.0
}

const (
	costPerPull = 50 // 1回あたりの石消費量
)

type GachaService struct {
	db         *gorm.DB
	userRepo   *repository.UserRepository
	gachaRepo  *repository.GachaRepository
	masterRepo *repository.MasterRepository
	charRepo   *repository.CharacterRepository
	weaponRepo *repository.WeaponRepository
}

func NewGachaService(
	db *gorm.DB,
	userRepo *repository.UserRepository,
	gachaRepo *repository.GachaRepository,
	masterRepo *repository.MasterRepository,
	charRepo *repository.CharacterRepository,
	weaponRepo *repository.WeaponRepository,
) *GachaService {
	return &GachaService{
		db:         db,
		userRepo:   userRepo,
		gachaRepo:  gachaRepo,
		masterRepo: masterRepo,
		charRepo:   charRepo,
		weaponRepo: weaponRepo,
	}
}

// Pull — ガチャを引く
func (s *GachaService) Pull(userID uuid.UUID, req GachaPullRequest) (*GachaPullResponse, error) {
	// --- バリデーション ---
	if req.Count != 1 && req.Count != 10 {
		return nil, fmt.Errorf("回数は 1（単発）か 10（10連）のみ対応です")
	}
	totalCost := costPerPull * req.Count

	// --- ユーザーの石残高チェック ---
	user, err := s.userRepo.GetByID(userID)
	if err != nil {
		return nil, fmt.Errorf("ユーザー取得に失敗: %w", err)
	}
	if user.Stones < totalCost {
		return nil, fmt.Errorf("石が足りません（必要: %d, 所持: %d）", totalCost, user.Stones)
	}

	// --- バナー取得 ---
	banner, err := s.masterRepo.GetBanner(req.BannerID)
	if err != nil {
		return nil, fmt.Errorf("バナー取得に失敗: %w", err)
	}
	now := time.Now().UTC()
	if !banner.IsActive || now.Before(banner.StartAt) || now.After(banner.EndAt) {
		return nil, fmt.Errorf("このバナーは現在開催されていません")
	}

	// --- 排出テーブルをパース ---
	var rateTable []RateTableEntry
	if err := json.Unmarshal(banner.RateTable, &rateTable); err != nil {
		return nil, fmt.Errorf("排出テーブルの読み込みに失敗: %w", err)
	}
	if err := s.validateRateTable(rateTable); err != nil {
		return nil, err
	}
	featured, err := s.masterRepo.ListFeaturedByBannerID(req.BannerID)
	if err != nil {
		return nil, fmt.Errorf("ピックアップ情報の取得に失敗: %w", err)
	}
	effectiveRates, err := mergeFeaturedIntoRateTable(rateTable, featured)
	if err != nil {
		return nil, fmt.Errorf("排出テーブルの合成に失敗: %w", err)
	}

	// --- 現在の天井カウント取得 ---
	currentPity, err := s.gachaRepo.GetPityCount(userID, req.BannerID)
	if err != nil {
		return nil, fmt.Errorf("天井カウント取得に失敗: %w", err)
	}

	// --- トランザクション内でガチャ処理 ---
	var results []GachaPullResult

	err = s.db.Transaction(func(tx *gorm.DB) error {
		// 石を消費する（競合防止: WHERE stones >= totalCost で楽観ロック）
		result := tx.Model(&model.User{}).
			Where("id = ? AND stones >= ?", userID, totalCost).
			Update("stones", gorm.Expr("stones - ?", totalCost))
		if result.Error != nil {
			return fmt.Errorf("石の消費に失敗: %w", result.Error)
		}
		if result.RowsAffected == 0 {
			return fmt.Errorf("石が足りません（競合または残高不足。必要: %d）", totalCost)
		}

		// 所持キャラ一覧（新規/凸判定用）
		existingChars, _ := s.charRepo.ListByUser(userID)
		charMap := make(map[uuid.UUID]*model.UserCharacter)
		for i := range existingChars {
			charMap[existingChars[i].CharacterID] = &existingChars[i]
		}

		// 所持武器一覧
		existingWeapons, _ := s.weaponRepo.ListByUser(userID)
		weaponMap := make(map[uuid.UUID]*model.UserWeapon)
		for i := range existingWeapons {
			weaponMap[existingWeapons[i].WeaponID] = &existingWeapons[i]
		}

		const maxBreakthrough = 6
		const maxRefinement = 4

		for i := 0; i < req.Count; i++ {
			currentPity++

			// 排出アイテムを抽選する
			entry := s.rollGacha(effectiveRates, currentPity, banner.PityThreshold)

			// 名前とレアリティを取得
			name := ""
			rarity := 0
			isNew := false

			if entry.ResultType == "character" {
				mc, _ := s.masterRepo.GetCharacter(entry.ItemID)
				if mc != nil {
					name = mc.Name
					rarity = mc.Rarity
				}
				if existing, ok := charMap[entry.ItemID]; ok {
					if existing.BreakthroughLevel < maxBreakthrough {
						if err := tx.Model(existing).
							Update("breakthrough_level", existing.BreakthroughLevel+1).Error; err != nil {
							return fmt.Errorf("凸更新に失敗: %w", err)
						}
						existing.BreakthroughLevel++
					} else {
						// 重複 → 代償通貨（レアリティに応じて増減）
						compStones, compGold := breakthroughCompensation(rarity)
						if compStones > 0 {
							if err := tx.Model(&model.User{}).
								Where("id = ?", userID).
								Update("stones", gorm.Expr("stones + ?", compStones)).Error; err != nil {
								return fmt.Errorf("代償石付与に失敗: %w", err)
							}
						}
						if compGold > 0 {
							if err := tx.Model(&model.User{}).
								Where("id = ?", userID).
								Update("gold", gorm.Expr("gold + ?", compGold)).Error; err != nil {
								return fmt.Errorf("代償ゴールド付与に失敗: %w", err)
							}
						}
					}
				} else {
					isNew = true
					uc := model.UserCharacter{
						UserID:           userID,
						CharacterID:      entry.ItemID,
						Level:            1,
						CurrentXP:        0,
						ObtainedAt:       time.Now().UTC(),
						BreakthroughLevel: 0,
					}
					if err := s.charRepo.Create(tx, &uc); err != nil {
						return fmt.Errorf("キャラ付与に失敗: %w", err)
					}
					charMap[entry.ItemID] = &uc
				}
			} else {
				mw, _ := s.masterRepo.GetWeapon(entry.ItemID)
				if mw != nil {
					name = mw.Name
					rarity = mw.Rarity
				}
				if existing, ok := weaponMap[entry.ItemID]; ok {
					if existing.RefinementLevel < maxRefinement {
						if err := tx.Model(existing).
							Update("refinement_level", existing.RefinementLevel+1).Error; err != nil {
							return fmt.Errorf("精錬更新に失敗: %w", err)
						}
						existing.RefinementLevel++
					} else {
						// 重複 → 精錬代償通貨
						compStones, compGold := refinementCompensation(rarity)
						if compStones > 0 {
							if err := tx.Model(&model.User{}).
								Where("id = ?", userID).
								Update("stones", gorm.Expr("stones + ?", compStones)).Error; err != nil {
								return fmt.Errorf("代償石付与に失敗: %w", err)
							}
						}
						if compGold > 0 {
							if err := tx.Model(&model.User{}).
								Where("id = ?", userID).
								Update("gold", gorm.Expr("gold + ?", compGold)).Error; err != nil {
								return fmt.Errorf("代償ゴールド付与に失敗: %w", err)
							}
						}
					}
				} else {
					isNew = true
					uw := model.UserWeapon{
						UserID:          userID,
						WeaponID:        entry.ItemID,
						Level:           1,
						ObtainedAt:      time.Now().UTC(),
						RefinementLevel: 0,
					}
					if err := s.weaponRepo.Create(tx, &uw); err != nil {
						return fmt.Errorf("武器付与に失敗: %w", err)
					}
					weaponMap[entry.ItemID] = &uw
				}
			}

			// ガチャ履歴を保存する
			history := model.GachaHistory{
				UserID:       userID,
				BannerID:     req.BannerID,
				ResultType:   entry.ResultType,
				ResultItemID: entry.ItemID,
				PityCount:    currentPity,
			}
			if err := s.gachaRepo.CreateHistory(tx, &history); err != nil {
				return fmt.Errorf("ガチャ履歴保存に失敗: %w", err)
			}

			// 天井到達でリセット
			if banner.PityThreshold != nil && currentPity >= *banner.PityThreshold {
				currentPity = 0
			}

			results = append(results, GachaPullResult{
				ResultType: entry.ResultType,
				ItemID:     entry.ItemID,
				Name:       name,
				Rarity:     rarity,
				IsNew:      isNew,
				PityCount:  currentPity,
			})
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	updatedUser, _ := s.userRepo.GetByID(userID)
	remaining := 0
	if updatedUser != nil {
		remaining = updatedUser.Stones
	}

	return &GachaPullResponse{
		Results:         results,
		StonesSpent:     totalCost,
		RemainingStones: remaining,
		UpdatedUser:     updatedUser,
	}, nil
}

// validateRateTable — rate_table の各 item_id が有効なマスタを指すか検証する
func (s *GachaService) validateRateTable(entries []RateTableEntry) error {
	for _, e := range entries {
		switch strings.ToLower(e.ResultType) {
		case "character":
			c, err := s.masterRepo.GetCharacter(e.ItemID)
			if err != nil || c == nil {
				return fmt.Errorf("キャラマスタが見つかりません: %s", e.ItemID)
			}
			if !c.IsActive {
				return fmt.Errorf("キャラマスタが無効です: %s", e.ItemID)
			}
		case "weapon":
			w, err := s.masterRepo.GetWeapon(e.ItemID)
			if err != nil || w == nil {
				return fmt.Errorf("武器マスタが見つかりません: %s", e.ItemID)
			}
			if !w.IsActive {
				return fmt.Errorf("武器マスタが無効です: %s", e.ItemID)
			}
		case "costume":
			// 衣装マスタ（m_costumes）導入後にここで検証する
		default:
			return fmt.Errorf("不明な result_type: %s", e.ResultType)
		}
	}
	return nil
}

// rollGacha — 排出テーブルから1件を抽選する
// 天井到達時は最高レアリティを確定排出する
func (s *GachaService) rollGacha(table []RateTableEntry, pity int, threshold *int) RateTableEntry {
	// 天井判定: 天井に到達していれば最高レアリティを確定排出
	if threshold != nil && pity >= *threshold {
		maxRarity := 0
		var best RateTableEntry
		for _, e := range table {
			if e.Rarity > maxRarity {
				maxRarity = e.Rarity
				best = e
			}
		}
		return best
	}

	// 通常抽選: 累積確率で抽選する
	roll := rand.Float64()
	cumulative := 0.0
	for _, e := range table {
		cumulative += e.Rate
		if roll <= cumulative {
			return e
		}
	}

	// フォールバック（確率の合計が1未満の場合、最後のエントリを返す）
	return table[len(table)-1]
}

func breakthroughCompensation(rarity int) (stones int, gold int) {
	switch rarity {
	case 5:
		return 25, 0
	case 4:
		return 0, 500
	case 3:
		return 0, 100
	default:
		return 0, 100
	}
}

func refinementCompensation(rarity int) (stones int, gold int) {
	switch rarity {
	case 5:
		return 15, 0
	case 4:
		return 0, 300
	case 3:
		return 0, 50
	default:
		return 0, 50
	}
}
