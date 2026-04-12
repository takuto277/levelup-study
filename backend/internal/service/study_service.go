package service

import (
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

// ============================================================
// StudyService — 勉強セッション完了時のビジネスロジック
//
// 処理の流れ（docs/database/01_Database_Schema.md §5.3 準拠）:
//   1. リクエスト検証
//   2. study_sessions INSERT
//   3. 報酬計算 → study_rewards INSERT
//   4. users 通貨加算
//   5. 冒険キャラ（リクエストの user_character_id またはパーティ先頭）へ経験値付与・レベルアップ
//   6. ダンジョンステージ進行
//   7. トランザクション COMMIT
// ============================================================

// CompleteStudyRequest — クライアントから送られる勉強完了リクエスト
type CompleteStudyRequest struct {
	StartedAt       time.Time  `json:"started_at"`
	EndedAt         time.Time  `json:"ended_at"`
	DurationSeconds int        `json:"duration_seconds"`
	Category        *string    `json:"category"`
	IsCompleted     bool       `json:"is_completed"`
	UserCharacterID *uuid.UUID `json:"user_character_id,omitempty"` // 冒険に出した所持キャラ（省略時はパーティ先頭スロット）
	// 冒険クエスト用: 通常敵・ボス（最終フロア撃破）討伐数。旧クライアントは 0 のまま。
	DefeatNormalCount int `json:"defeat_normal_count"`
	DefeatBossCount   int `json:"defeat_boss_count"`
	// ダンジョン難易度倍率（経験値のみ）。0 以下は 1 扱い。
	DifficultyMultiplier float64 `json:"difficulty_multiplier"`
}

// CompleteStudyResponse — 勉強完了レスポンス
type CompleteStudyResponse struct {
	SessionID   uuid.UUID           `json:"session_id"`
	Rewards     []model.StudyReward `json:"rewards"`
	UpdatedUser model.User          `json:"updated_user"`
}

type StudyService struct {
	db          *gorm.DB
	userRepo    *repository.UserRepository
	studyRepo   *repository.StudyRepository
	charRepo    *repository.CharacterRepository
	partyRepo   *repository.PartyRepository
	dungeonRepo *repository.DungeonProgressRepository
}

func NewStudyService(
	db *gorm.DB,
	userRepo *repository.UserRepository,
	studyRepo *repository.StudyRepository,
	charRepo *repository.CharacterRepository,
	partyRepo *repository.PartyRepository,
	dungeonRepo *repository.DungeonProgressRepository,
) *StudyService {
	return &StudyService{
		db:          db,
		userRepo:    userRepo,
		studyRepo:   studyRepo,
		charRepo:    charRepo,
		partyRepo:   partyRepo,
		dungeonRepo: dungeonRepo,
	}
}

// CompleteStudy — 勉強セッションを完了し、報酬を確定する
func (s *StudyService) CompleteStudy(userID uuid.UUID, req CompleteStudyRequest) (*CompleteStudyResponse, error) {
	// --- 1. リクエスト検証 ---
	if err := s.validateRequest(req); err != nil {
		return nil, err
	}

	var resp CompleteStudyResponse

	// --- トランザクション開始 ---
	err := s.db.Transaction(func(tx *gorm.DB) error {
		// --- 2. セッション保存 ---
		session := model.StudySession{
			UserID:          userID,
			Category:        req.Category,
			StartedAt:       req.StartedAt,
			EndedAt:         req.EndedAt,
			DurationSeconds: req.DurationSeconds,
			IsCompleted:     req.IsCompleted,
		}
		if err := s.studyRepo.CreateSession(tx, &session); err != nil {
			return fmt.Errorf("セッション保存に失敗: %w", err)
		}
		resp.SessionID = session.ID

		// --- 3. 報酬計算 ---
		rewards, totalStones, totalGold, totalXP := s.calculateRewards(
			session.ID,
			req.DurationSeconds,
			req.DefeatNormalCount,
			req.DefeatBossCount,
			req.DifficultyMultiplier,
		)
		if err := s.studyRepo.CreateRewards(tx, rewards); err != nil {
			return fmt.Errorf("報酬保存に失敗: %w", err)
		}
		resp.Rewards = rewards

		// --- 4. ユーザー通貨加算 ---
		if err := s.userRepo.IncrementCurrencies(tx, userID, totalStones, totalGold, req.DurationSeconds); err != nil {
			return fmt.Errorf("通貨加算に失敗: %w", err)
		}

		// --- 5. 冒険に出したキャラへ経験値付与（レベルアップ・必要XPはレベルごとに +100 ずつ増加） ---
		if totalXP > 0 {
			if err := s.grantStudyExperienceToAdventurer(tx, userID, req.UserCharacterID, totalXP); err != nil {
				return fmt.Errorf("経験値付与に失敗: %w", err)
			}
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	// --- 更新後のユーザー情報を取得してレスポンスに含める ---
	user, err := s.userRepo.GetByID(userID)
	if err != nil {
		return nil, fmt.Errorf("ユーザー取得に失敗: %w", err)
	}
	resp.UpdatedUser = *user

	return &resp, nil
}

// validateRequest — リクエストの妥当性を検証する
func (s *StudyService) validateRequest(req CompleteStudyRequest) error {
	now := time.Now().UTC()

	// 開始時刻が未来でないか
	if req.StartedAt.After(now) {
		return fmt.Errorf("開始時刻が未来です")
	}
	// 終了時刻が開始時刻より後か
	if !req.EndedAt.After(req.StartedAt) {
		return fmt.Errorf("終了時刻が開始時刻より前です")
	}
	// 勉強時間が妥当か（最大24時間=86400秒）
	if req.DurationSeconds <= 0 || req.DurationSeconds > 86400 {
		return fmt.Errorf("勉強時間が不正です: %d秒", req.DurationSeconds)
	}
	return nil
}

// calculateRewards — 報酬を計算する
//
// 経験値: 10秒ごと +1、通常敵撃破 +10、ボス撃破 +50（いずれも難易度倍率を乗算。デフォルト等倍 1.0）
// ダイヤ（stones）: 2分（120秒）ごとに +1
// ゴールド: 10分ごとに +10（従来どおり）
func (s *StudyService) calculateRewards(
	sessionID uuid.UUID,
	durationSec int,
	defeatNormal int,
	defeatBoss int,
	difficultyMult float64,
) ([]model.StudyReward, int, int, int) {
	var rewards []model.StudyReward
	totalStones := 0
	totalGold := 0
	totalXP := 0
	minutes := durationSec / 60

	if difficultyMult <= 0 {
		difficultyMult = 1
	}
	if defeatNormal < 0 {
		defeatNormal = 0
	}
	if defeatBoss < 0 {
		defeatBoss = 0
	}

	// --- ダイヤ（知識の結晶 / stones）: 2分ごとに +1 ---
	stonesFromTime := durationSec / 120
	if stonesFromTime > 0 {
		totalStones += stonesFromTime
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones",
			Amount:     stonesFromTime,
		})
	}

	// --- ゴールド: 10分ごとに +10 ---
	goldBase := (minutes / 10) * 10
	if goldBase > 0 {
		totalGold += goldBase
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "gold",
			Amount:     goldBase,
		})
	}

	// --- 経験値: 時間 + 討伐（ボスは最終フロア撃破）× 難易度 ---
	xpFromTime := durationSec / 10
	xpFromKills := defeatNormal*10 + defeatBoss*50
	rawXP := xpFromTime + xpFromKills
	totalXP = int(float64(rawXP) * difficultyMult)
	if totalXP < 0 {
		totalXP = 0
	}
	if totalXP > 0 {
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "xp",
			Amount:     totalXP,
		})
	}

	return rewards, totalStones, totalGold, totalXP
}

// ListSessions — ユーザーの勉強セッション一覧を取得する
func (s *StudyService) ListSessions(userID uuid.UUID, limit, offset int) ([]model.StudySession, error) {
	return s.studyRepo.ListSessionsByUser(userID, limit, offset)
}

// xpRequiredForNextLevel — 現在レベル L から L+1 に上げるのに必要な経験値。
// L1→2: 300, L10→11: 300+9*100=1200, L11→12: 1300 … レベルが1上がるごとに必要量が +100 される。
func xpRequiredForNextLevel(currentLevel int) int {
	if currentLevel < 1 {
		currentLevel = 1
	}
	return 300 + (currentLevel-1)*100
}

// grantStudyExperienceToAdventurer — 指定キャラ（本人所有のものに限る）へ全XPを付与。未指定ならパーティ先頭。
func (s *StudyService) grantStudyExperienceToAdventurer(tx *gorm.DB, userID uuid.UUID, explicit *uuid.UUID, totalXP int) error {
	if totalXP <= 0 {
		return nil
	}
	var targetID uuid.UUID
	if explicit != nil && *explicit != uuid.Nil {
		if _, err := s.charRepo.GetByIDForUserTx(tx, *explicit, userID); err == nil {
			targetID = *explicit
		}
	}
	if targetID == uuid.Nil {
		slots, err := s.partyRepo.GetByUserTx(tx, userID)
		if err != nil || len(slots) == 0 {
			return nil
		}
		targetID = slots[0].UserCharacterID
	}
	return applyStudyExperience(tx, s.charRepo, targetID, totalXP)
}

// applyStudyExperience — current_xp を「次レベルまでの進捗」として加算し、必要量を満たすたびにレベルアップ（上限なし）
func applyStudyExperience(tx *gorm.DB, charRepo *repository.CharacterRepository, userCharID uuid.UUID, grant int) error {
	var uc model.UserCharacter
	if err := tx.First(&uc, "id = ?", userCharID).Error; err != nil {
		return err
	}
	xp := uc.CurrentXP + grant
	lvl := uc.Level
	for xp >= xpRequiredForNextLevel(lvl) {
		xp -= xpRequiredForNextLevel(lvl)
		lvl++
	}
	return charRepo.UpdateLevelAndXP(tx, userCharID, lvl, xp)
}
