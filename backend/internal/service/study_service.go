package service

import (
	"errors"
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
	GenreID         *uuid.UUID `json:"genre_id,omitempty"`
	DungeonID       *uuid.UUID `json:"dungeon_id,omitempty"`
	IsCompleted     bool       `json:"is_completed"`
	UserCharacterID *uuid.UUID `json:"user_character_id,omitempty"`
	DefeatNormalCount int `json:"defeat_normal_count"`
	DefeatBossCount   int `json:"defeat_boss_count"`
	DifficultyMultiplier float64 `json:"difficulty_multiplier"`
	ClientSessionID  *string   `json:"client_session_id,omitempty"`
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
	goalRepo    *repository.GoalRepository
}

func NewStudyService(
	db *gorm.DB,
	userRepo *repository.UserRepository,
	studyRepo *repository.StudyRepository,
	charRepo *repository.CharacterRepository,
	partyRepo *repository.PartyRepository,
	dungeonRepo *repository.DungeonProgressRepository,
	goalRepo *repository.GoalRepository,
) *StudyService {
	return &StudyService{
		db:          db,
		userRepo:    userRepo,
		studyRepo:   studyRepo,
		charRepo:    charRepo,
		partyRepo:   partyRepo,
		dungeonRepo: dungeonRepo,
		goalRepo:    goalRepo,
	}
}

// CompleteStudy — 勉強セッションを完了し、報酬を確定する
func (s *StudyService) CompleteStudy(userID uuid.UUID, req CompleteStudyRequest) (*CompleteStudyResponse, error) {
	// --- 1. リクエスト検証 ---
	if err := s.validateRequest(req); err != nil {
		return nil, err
	}

	// --- 2. 冪等チェック ---
	if req.ClientSessionID != nil && *req.ClientSessionID != "" {
		var existing model.StudySession
		if err := s.db.Where("user_id = ? AND client_session_id = ?", userID, *req.ClientSessionID).
			Preload("Rewards").First(&existing).Error; err == nil {
			user, _ := s.userRepo.GetByID(userID)
			resp := &CompleteStudyResponse{
				SessionID:   existing.ID,
				Rewards:     existing.Rewards,
			}
			if user != nil {
				resp.UpdatedUser = *user
			}
			return resp, nil
		}
	}

	var resp CompleteStudyResponse

	// --- トランザクション開始 ---
	err := s.db.Transaction(func(tx *gorm.DB) error {
		var user model.User
		if err := tx.First(&user, "id = ?", userID).Error; err != nil {
			return fmt.Errorf("ユーザー取得に失敗: %w", err)
		}

		genreID := resolveStudyGenreID(req)
		dungeonID := resolveStudyDungeonID(req, &user)

		dailySecondsBefore, err := s.studyRepo.GetDailyStudySecondsTx(tx, userID, req.StartedAt)
		if err != nil {
			return fmt.Errorf("日次勉強時間の取得に失敗: %w", err)
		}
		reachedDailyBonus := dailySecondsBefore < dailyStudyBonusThresholdSeconds &&
			dailySecondsBefore+int64(req.DurationSeconds) >= dailyStudyBonusThresholdSeconds

		// --- 2. セッション保存 ---
		session := model.StudySession{
			UserID:          userID,
			ClientSessionID: req.ClientSessionID,
			Category:        req.Category,
			GenreID:         genreID,
			DungeonID:       dungeonID,
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
			reachedDailyBonus,
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

		// --- 6. ダンジョンステージ進行 ---
		if err := s.advanceStudyDungeon(tx, userID, dungeonID, req.DurationSeconds); err != nil {
			return fmt.Errorf("ダンジョン進行に失敗: %w", err)
		}

		// 目標進捗を更新
		if s.goalRepo != nil {
			if err := s.updateGoalProgress(tx, userID, req); err != nil {
				return fmt.Errorf("目標進捗更新に失敗: %w", err)
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
	// 勉強時間が妥当か（最小1秒、最大6時間=21600秒）
	if req.DurationSeconds <= 0 || req.DurationSeconds > 21600 {
		return fmt.Errorf("勉強時間が不正です: %d秒", req.DurationSeconds)
	}
	// 討伐数の符号チェック
	if req.DefeatNormalCount < 0 || req.DefeatBossCount < 0 {
		return fmt.Errorf("討伐数は0以上である必要があります")
	}
	// 難易度倍率はクライアント申告値を受け付けるが、範囲を制限。
	// 0以下の値（旧クライアントのゼロ値含む）は calculateRewards で 1.0 に正規化されるため、
	// ここでは上限超過のみを拒否する。
	if req.DifficultyMultiplier > 5.0 {
		return fmt.Errorf("難易度倍率が不正です: %.2f", req.DifficultyMultiplier)
	}
	// 討伐数の上限検証（過大申告を防ぐ）
	// 戦闘ターン周期3秒を想定し、通常敵は3秒に1体が理論上限。
	// ボスは最終フロア到達に時間を要するため、3分に1体を上限とする。
	maxNormal := req.DurationSeconds/3 + 5
	if req.DefeatNormalCount > maxNormal {
		return fmt.Errorf("通常敵討伐数が上限を超えています: %d > %d", req.DefeatNormalCount, maxNormal)
	}
	maxBoss := req.DurationSeconds/180 + 1
	if req.DefeatBossCount > maxBoss {
		return fmt.Errorf("ボス討伐数が上限を超えています: %d > %d", req.DefeatBossCount, maxBoss)
	}
	// ボスを倒すには通常敵を一定程度倒している必要がある
	if req.DefeatBossCount > 0 && req.DefeatBossCount > req.DefeatNormalCount {
		return fmt.Errorf("ボス討伐数が通常敵討伐数を超えています")
	}
	return nil
}

// calculateRewards — 報酬を計算する
//
// 経験値: 10秒ごと +1、通常敵撃破 +10、ボス撃破 +50（いずれも難易度倍率を乗算。デフォルト等倍 1.0）
// ダイヤ（stones）: 10分ごと +5、30分 / 60分 / 日次2時間ボーナス
func (s *StudyService) calculateRewards(
	sessionID uuid.UUID,
	durationSec int,
	defeatNormal int,
	defeatBoss int,
	difficultyMult float64,
	grantDailyBonus bool,
) ([]model.StudyReward, int, int, int) {
	rewards := make([]model.StudyReward, 0)
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

	// --- ダイヤ（知識の結晶 / stones）: 10分ごとに +5 ---
	stonesFromTime := (durationSec / 600) * 5
	if stonesFromTime > 0 {
		totalStones += stonesFromTime
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones",
			Amount:     stonesFromTime,
		})
	}

	if minutes >= 30 {
		const bonus = 10
		totalStones += bonus
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones_bonus_30",
			Amount:     bonus,
		})
	}

	if minutes >= 60 {
		const bonus = 25
		totalStones += bonus
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones_bonus_60",
			Amount:     bonus,
		})
	}

	if grantDailyBonus {
		const bonus = 50
		totalStones += bonus
		rewards = append(rewards, model.StudyReward{
			SessionID:  sessionID,
			RewardType: "stones_bonus_daily",
			Amount:     bonus,
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

const (
	dailyStudyBonusThresholdSeconds = int64(2 * 60 * 60)
	studySecondsPerDungeonStage     = 10 * 60
)

func resolveStudyGenreID(req CompleteStudyRequest) *uuid.UUID {
	if req.GenreID != nil && *req.GenreID != uuid.Nil {
		return req.GenreID
	}
	if req.Category == nil || *req.Category == "" {
		return nil
	}
	parsed, err := uuid.Parse(*req.Category)
	if err != nil {
		return nil
	}
	return &parsed
}

func resolveStudyDungeonID(req CompleteStudyRequest, user *model.User) *uuid.UUID {
	if req.DungeonID != nil && *req.DungeonID != uuid.Nil {
		return req.DungeonID
	}
	return user.SelectedDungeonID
}

func (s *StudyService) advanceStudyDungeon(tx *gorm.DB, userID uuid.UUID, dungeonID *uuid.UUID, durationSec int) error {
	if dungeonID == nil || *dungeonID == uuid.Nil {
		return nil
	}
	stageAdvance := durationSec / studySecondsPerDungeonStage
	if stageAdvance <= 0 {
		return nil
	}

	progress, err := s.dungeonRepo.GetByUserAndDungeonTx(tx, userID, *dungeonID)
	if err != nil {
		if !errors.Is(err, gorm.ErrRecordNotFound) {
			return err
		}
		newStage := 1 + stageAdvance
		return s.dungeonRepo.Upsert(tx, &model.UserDungeonProgress{
			UserID:          userID,
			DungeonID:       *dungeonID,
			CurrentStage:    newStage,
			MaxClearedStage: newStage - 1,
		})
	}

	return s.dungeonRepo.AdvanceStage(tx, progress.ID, progress.CurrentStage+stageAdvance)
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

func (s *StudyService) updateGoalProgress(tx *gorm.DB, userID uuid.UUID, req CompleteStudyRequest) error {
	activeGoals, err := s.goalRepo.ListActiveByUser(userID)
	if err != nil || len(activeGoals) == 0 {
		return nil
	}
	for _, goal := range activeGoals {
		var increment int
		switch goal.GoalType {
		case "pomodoro_count":
			if req.IsCompleted {
				increment = 1
			}
		case "study_minutes":
			increment = req.DurationSeconds / 60
		case "genre_study_minutes":
			if goal.GenreID != nil && req.GenreID != nil && *goal.GenreID == *req.GenreID {
				increment = req.DurationSeconds / 60
			}
		}
		if increment > 0 {
			newValue := goal.CurrentValue + increment
			isCompleted := newValue >= goal.TargetValue
			if err := s.goalRepo.UpdateProgressTx(tx, goal.ID, newValue, isCompleted); err != nil {
				return err
			}
		}
	}
	return nil
}
