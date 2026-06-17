package service

import (
	"strings"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
	"gorm.io/gorm"
)

func TestXpRequiredForNextLevel(t *testing.T) {
	cases := []struct {
		level int
		want  int
	}{
		{1, 300},
		{2, 400},
		{10, 1200},
		{11, 1300},
	}
	for _, tc := range cases {
		if got := xpRequiredForNextLevel(tc.level); got != tc.want {
			t.Errorf("xpRequiredForNextLevel(%d) = %d, want %d", tc.level, got, tc.want)
		}
	}
}

func TestCalculateRewardsFollowsDesignStoneTable(t *testing.T) {
	sessionID := uuid.New()
	svc := &StudyService{}

	rewards, stones, gold, xp := svc.calculateRewards(sessionID, 60*60, 0, 0, 1, true)

	if stones != 115 {
		t.Fatalf("stones = %d, want 115", stones)
	}
	if gold != 0 {
		t.Fatalf("gold = %d, want 0", gold)
	}
	if xp != 360 {
		t.Fatalf("xp = %d, want 360", xp)
	}

	got := rewardAmountsByType(rewards)
	want := map[string]int{
		"stones":             30,
		"stones_bonus_30":    10,
		"stones_bonus_60":    25,
		"stones_bonus_daily": 50,
		"xp":                 360,
	}
	for typ, amount := range want {
		if got[typ] != amount {
			t.Fatalf("reward %s = %d, want %d; rewards=%v", typ, got[typ], amount, got)
		}
	}
}

func TestCompleteStudyGrantsDailyBonusOnlyWhenCrossingTwoHours(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := newStudyServiceForTest(db)
	userID := createStudyTestUser(t, db, nil)
	startedAt := time.Date(2026, 6, 4, 9, 0, 0, 0, time.UTC)

	_, err := svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:            startedAt,
		EndedAt:              startedAt.Add(90 * time.Minute),
		DurationSeconds:      90 * 60,
		DifficultyMultiplier: 1.0,
		IsCompleted:          true,
	})
	if err != nil {
		t.Fatalf("first CompleteStudy failed: %v", err)
	}

	resp, err := svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:            startedAt.Add(2 * time.Hour),
		EndedAt:              startedAt.Add(2*time.Hour + 30*time.Minute),
		DurationSeconds:      30 * 60,
		DifficultyMultiplier: 1.0,
		IsCompleted:          true,
	})
	if err != nil {
		t.Fatalf("second CompleteStudy failed: %v", err)
	}

	got := rewardAmountsByType(resp.Rewards)
	if got["stones_bonus_daily"] != 50 {
		t.Fatalf("daily bonus = %d, want 50; rewards=%v", got["stones_bonus_daily"], got)
	}

	resp, err = svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:            startedAt.Add(3 * time.Hour),
		EndedAt:              startedAt.Add(3*time.Hour + 10*time.Minute),
		DurationSeconds:      10 * 60,
		DifficultyMultiplier: 1.0,
		IsCompleted:          true,
	})
	if err != nil {
		t.Fatalf("third CompleteStudy failed: %v", err)
	}
	got = rewardAmountsByType(resp.Rewards)
	if got["stones_bonus_daily"] != 0 {
		t.Fatalf("daily bonus after already reached = %d, want 0; rewards=%v", got["stones_bonus_daily"], got)
	}
}

func TestCompleteStudyStoresGenreIDFromCategoryUUIDForCompatibility(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := newStudyServiceForTest(db)
	userID := createStudyTestUser(t, db, nil)
	genreID := uuid.New()
	category := genreID.String()
	startedAt := time.Date(2026, 6, 4, 9, 0, 0, 0, time.UTC)

	resp, err := svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:            startedAt,
		EndedAt:              startedAt.Add(10 * time.Minute),
		DurationSeconds:      10 * 60,
		Category:             &category,
		DifficultyMultiplier: 1.0,
		IsCompleted:          true,
	})
	if err != nil {
		t.Fatalf("CompleteStudy failed: %v", err)
	}

	var session model.StudySession
	if err := db.First(&session, "id = ?", resp.SessionID).Error; err != nil {
		t.Fatalf("session lookup failed: %v", err)
	}
	if session.GenreID == nil || *session.GenreID != genreID {
		t.Fatalf("genre_id = %v, want %s", session.GenreID, genreID)
	}
	if session.Category == nil || *session.Category != category {
		t.Fatalf("category = %v, want %s", session.Category, category)
	}
}

func TestCompleteStudyAcceptsZeroDifficultyMultiplierForBackwardCompat(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := newStudyServiceForTest(db)
	userID := createStudyTestUser(t, db, nil)
	startedAt := time.Date(2026, 6, 4, 9, 0, 0, 0, time.UTC)

	// DifficultyMultiplier が 0（旧クライアントのゼロ値）でもエラーにならず、
	// calculateRewards で 1.0 に正規化されて通常の経験値が付与されることを確認。
	resp, err := svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:       startedAt,
		EndedAt:         startedAt.Add(10 * time.Minute),
		DurationSeconds: 10 * 60,
		IsCompleted:     true,
	})
	if err != nil {
		t.Fatalf("CompleteStudy with zero DifficultyMultiplier failed: %v", err)
	}

	xp := rewardAmountsByType(resp.Rewards)["xp"]
	if xp != 60 {
		t.Fatalf("xp = %d, want 60 (duration 600s / 10s per xp)", xp)
	}
}

func TestCompleteStudyRejectsInvalidRewardInputs(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := newStudyServiceForTest(db)
	userID := createStudyTestUser(t, db, nil)
	startedAt := time.Date(2026, 6, 4, 9, 0, 0, 0, time.UTC)

	cases := []struct {
		name    string
		req     CompleteStudyRequest
		wantErr string
	}{
		{
			name: "negative defeat normal count",
			req: CompleteStudyRequest{
				StartedAt:         startedAt,
				EndedAt:           startedAt.Add(10 * time.Minute),
				DurationSeconds:   10 * 60,
				DefeatNormalCount: -1,
				IsCompleted:       true,
			},
			wantErr: "討伐数",
		},
		{
			name: "difficulty multiplier too high",
			req: CompleteStudyRequest{
				StartedAt:            startedAt,
				EndedAt:              startedAt.Add(10 * time.Minute),
				DurationSeconds:      10 * 60,
				DifficultyMultiplier: 10,
				IsCompleted:          true,
			},
			wantErr: "難易度倍率",
		},
		{
			name: "normal defeats exceed limit",
			req: CompleteStudyRequest{
				StartedAt:            startedAt,
				EndedAt:              startedAt.Add(10 * time.Minute),
				DurationSeconds:      10 * 60,
				DefeatNormalCount:    1000,
				DifficultyMultiplier: 1.0,
				IsCompleted:          true,
			},
			wantErr: "通常敵",
		},
		{
			name: "boss defeats exceed limit",
			req: CompleteStudyRequest{
				StartedAt:            startedAt,
				EndedAt:              startedAt.Add(10 * time.Minute),
				DurationSeconds:      10 * 60,
				DefeatBossCount:      10,
				DifficultyMultiplier: 1.0,
				IsCompleted:          true,
			},
			wantErr: "ボス",
		},
		{
			name: "boss defeats exceed normal defeats",
			req: CompleteStudyRequest{
				StartedAt:            startedAt,
				EndedAt:              startedAt.Add(30 * time.Minute),
				DurationSeconds:      30 * 60,
				DefeatNormalCount:    1,
				DefeatBossCount:      2,
				DifficultyMultiplier: 1.0,
				IsCompleted:          true,
			},
			wantErr: "ボス討伐数が通常敵",
		},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := svc.CompleteStudy(userID, tc.req)
			if err == nil {
				t.Fatalf("expected error containing %q, got nil", tc.wantErr)
			}
			if !strings.Contains(err.Error(), tc.wantErr) {
				t.Fatalf("error = %q, want containing %q", err.Error(), tc.wantErr)
			}
		})
	}
}

func TestCompleteStudyAdvancesSelectedDungeonProgress(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := newStudyServiceForTest(db)
	dungeonID := uuid.New()
	userID := createStudyTestUser(t, db, &dungeonID)
	startedAt := time.Date(2026, 6, 4, 9, 0, 0, 0, time.UTC)

	resp, err := svc.CompleteStudy(userID, CompleteStudyRequest{
		StartedAt:            startedAt,
		EndedAt:              startedAt.Add(20 * time.Minute),
		DurationSeconds:      20 * 60,
		DifficultyMultiplier: 1.0,
		IsCompleted:          true,
	})
	if err != nil {
		t.Fatalf("CompleteStudy failed: %v", err)
	}

	var session model.StudySession
	if err := db.First(&session, "id = ?", resp.SessionID).Error; err != nil {
		t.Fatalf("session lookup failed: %v", err)
	}
	if session.DungeonID == nil || *session.DungeonID != dungeonID {
		t.Fatalf("session dungeon_id = %v, want %s", session.DungeonID, dungeonID)
	}

	var progress model.UserDungeonProgress
	if err := db.First(&progress, "user_id = ? AND dungeon_id = ?", userID, dungeonID).Error; err != nil {
		t.Fatalf("progress lookup failed: %v", err)
	}
	if progress.CurrentStage != 3 {
		t.Fatalf("current_stage = %d, want 3", progress.CurrentStage)
	}
	if progress.MaxClearedStage != 2 {
		t.Fatalf("max_cleared_stage = %d, want 2", progress.MaxClearedStage)
	}
}

func rewardAmountsByType(rewards []model.StudyReward) map[string]int {
	out := map[string]int{}
	for _, r := range rewards {
		out[r.RewardType] += r.Amount
	}
	return out
}

func newStudyServiceForTest(db *gorm.DB) *StudyService {
	return NewStudyService(
		db,
		repository.NewUserRepository(db),
		repository.NewStudyRepository(db),
		repository.NewCharacterRepository(db),
		repository.NewPartyRepository(db),
		repository.NewDungeonProgressRepository(db),
	)
}

func createStudyTestUser(t *testing.T, db *gorm.DB, selectedDungeonID *uuid.UUID) uuid.UUID {
	t.Helper()
	user := &model.User{
		ID:                uuid.New(),
		DisplayName:       "study-test",
		SelectedDungeonID: selectedDungeonID,
	}
	if err := db.Create(user).Error; err != nil {
		t.Fatalf("user create failed: %v", err)
	}
	return user.ID
}
