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
//   5. パーティキャラの XP 加算
//   6. ダンジョンステージ進行
//   7. トランザクション COMMIT
// ============================================================

// CompleteStudyRequest — クライアントから送られる勉強完了リクエスト
type CompleteStudyRequest struct {
	StartedAt       time.Time `json:"started_at"`
	EndedAt         time.Time `json:"ended_at"`
	DurationSeconds int       `json:"duration_seconds"`
	Category        *string   `json:"category"`
	IsCompleted     bool      `json:"is_completed"`
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
		rewards, totalStones, totalGold, totalXP := s.calculateRewards(session.ID, req.DurationSeconds, userID)
		if err := s.studyRepo.CreateRewards(tx, rewards); err != nil {
			return fmt.Errorf("報酬保存に失敗: %w", err)
		}
		resp.Rewards = rewards

		// --- 4. ユーザー通貨加算 ---
		if err := s.userRepo.IncrementCurrencies(tx, userID, totalStones, totalGold, req.DurationSeconds); err != nil {
			return fmt.Errorf("通貨加算に失敗: %w", err)
		}

		// --- 5. パーティキャラの XP 加算 ---
		if totalXP > 0 {
			if err := s.addXPToParty(tx, userID, totalXP); err != nil {
				return fmt.Errorf("XP加算に失敗: %w", err)
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
// docs/database/01_Database_Schema.md §5.1 に基づく
func (s *StudyService) calculateRewards(sessionID uuid.UUID, durationSec int, userID uuid.UUID) ([]model.StudyReward, int, int, int) {
	var rewards []model.StudyReward
	totalStones := 0
	totalGold := 0
	totalXP := 0
	minutes := durationSec / 60

	// --- ガチャ石: 10分ごとに +5 ---
	stonesBase := (minutes / 10) * 5
	if stonesBase > 0 {
		totalStones += stonesBase
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones",
			Amount:     stonesBase,
		})
	}

	// --- 30分連続ボーナス: +10 ---
	if minutes >= 30 {
		totalStones += 10
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones_bonus_30",
			Amount:     10,
		})
	}

	// --- 60分連続ボーナス: +25 ---
	if minutes >= 60 {
		totalStones += 25
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones_bonus_60",
			Amount:     25,
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

	// --- 経験値: 1分ごとに +2 ---
	xpBase := minutes * 2
	if xpBase > 0 {
		totalXP += xpBase
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "xp",
			Amount:     xpBase,
		})
	}

	return rewards, totalStones, totalGold, totalXP
}

// ListSessions — ユーザーの勉強セッション一覧を取得する
func (s *StudyService) ListSessions(userID uuid.UUID, limit, offset int) ([]model.StudySession, error) {
	return s.studyRepo.ListSessionsByUser(userID, limit, offset)
}

// addXPToParty — パーティメンバー全員に経験値を分配する
func (s *StudyService) addXPToParty(tx *gorm.DB, userID uuid.UUID, totalXP int) error {
	slots, err := s.partyRepo.GetByUser(userID)
	if err != nil || len(slots) == 0 {
		return nil // パーティ未編成の場合はスキップ
	}

	// パーティメンバーに均等配分（端数切り捨て）
	xpPerChar := totalXP / len(slots)
	if xpPerChar <= 0 {
		return nil
	}

	for _, slot := range slots {
		if err := s.charRepo.AddXP(tx, slot.UserCharacterID, xpPerChar); err != nil {
			return err
		}
	}
	return nil
}
