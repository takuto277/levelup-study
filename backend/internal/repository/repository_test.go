package repository_test

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
)

// ============================================================
// UserRepository のテスト
// ============================================================

func TestUserRepository_CreateAndGetByID(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := repository.NewUserRepository(db)

	// ユーザーを作成
	user := &model.User{
		ID:          uuid.New(),
		DisplayName: "テスト太郎",
	}
	if err := repo.Create(user); err != nil {
		t.Fatalf("ユーザー作成に失敗: %v", err)
	}

	// 取得して確認
	got, err := repo.GetByID(user.ID)
	if err != nil {
		t.Fatalf("ユーザー取得に失敗: %v", err)
	}
	if got.DisplayName != "テスト太郎" {
		t.Errorf("DisplayName が一致しない: got=%q, want=%q", got.DisplayName, "テスト太郎")
	}
	if got.Stones != 0 {
		t.Errorf("初期石数が0ではない: got=%d", got.Stones)
	}
	if got.Gold != 0 {
		t.Errorf("初期ゴールドが0ではない: got=%d", got.Gold)
	}
}

func TestUserRepository_Update(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := repository.NewUserRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "更新前"}
	repo.Create(user)

	// 名前を更新
	user.DisplayName = "更新後"
	if err := repo.Update(user); err != nil {
		t.Fatalf("ユーザー更新に失敗: %v", err)
	}

	got, _ := repo.GetByID(user.ID)
	if got.DisplayName != "更新後" {
		t.Errorf("更新が反映されていない: got=%q", got.DisplayName)
	}
}

func TestUserRepository_Delete(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := repository.NewUserRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "削除対象"}
	repo.Create(user)

	// 削除
	if err := repo.Delete(user.ID); err != nil {
		t.Fatalf("ユーザー削除に失敗: %v", err)
	}

	// 取得できないことを確認
	_, err := repo.GetByID(user.ID)
	if err == nil {
		t.Error("削除したユーザーが取得できてしまった")
	}
}

func TestUserRepository_AddStones(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := repository.NewUserRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "石テスト"}
	repo.Create(user)

	// 石を加算
	if err := repo.AddStones(user.ID, 50); err != nil {
		t.Fatalf("石加算に失敗: %v", err)
	}

	got, _ := repo.GetByID(user.ID)
	if got.Stones != 50 {
		t.Errorf("石の数が不正: got=%d, want=50", got.Stones)
	}

	// 追加で加算
	repo.AddStones(user.ID, 30)
	got, _ = repo.GetByID(user.ID)
	if got.Stones != 80 {
		t.Errorf("追加加算後の石が不正: got=%d, want=80", got.Stones)
	}
}

func TestUserRepository_IncrementCurrencies(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := repository.NewUserRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "一括加算テスト"}
	repo.Create(user)

	// 一括加算（トランザクション内）
	tx := db.Begin()
	if err := repo.IncrementCurrencies(tx, user.ID, 10, 20, 1500); err != nil {
		tx.Rollback()
		t.Fatalf("一括加算に失敗: %v", err)
	}
	tx.Commit()

	got, _ := repo.GetByID(user.ID)
	if got.Stones != 10 {
		t.Errorf("石: got=%d, want=10", got.Stones)
	}
	if got.Gold != 20 {
		t.Errorf("ゴールド: got=%d, want=20", got.Gold)
	}
	if got.TotalStudySeconds != 1500 {
		t.Errorf("勉強秒数: got=%d, want=1500", got.TotalStudySeconds)
	}
}

// ============================================================
// StudyRepository のテスト
// ============================================================

func TestStudyRepository_CreateSessionAndRewards(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	studyRepo := repository.NewStudyRepository(db)

	// テストユーザー作成
	user := &model.User{ID: uuid.New(), DisplayName: "勉強テスト"}
	userRepo.Create(user)

	// セッション作成
	now := time.Now().UTC()
	session := &model.StudySession{
		UserID:          user.ID,
		StartedAt:       now.Add(-30 * time.Minute),
		EndedAt:         now,
		DurationSeconds: 1800,
		IsCompleted:     true,
	}
	tx := db.Begin()
	if err := studyRepo.CreateSession(tx, session); err != nil {
		tx.Rollback()
		t.Fatalf("セッション作成に失敗: %v", err)
	}

	// 報酬作成
	rewards := []model.StudyReward{
		{SessionID: session.ID, RewardType: "stones", Amount: 15},
		{SessionID: session.ID, RewardType: "gold", Amount: 30},
		{SessionID: session.ID, RewardType: "xp", Amount: 60},
	}
	if err := studyRepo.CreateRewards(tx, rewards); err != nil {
		tx.Rollback()
		t.Fatalf("報酬作成に失敗: %v", err)
	}
	tx.Commit()

	// セッション取得（報酬含む）
	got, err := studyRepo.GetSessionByID(session.ID)
	if err != nil {
		t.Fatalf("セッション取得に失敗: %v", err)
	}
	if got.DurationSeconds != 1800 {
		t.Errorf("勉強秒数: got=%d, want=1800", got.DurationSeconds)
	}
	if len(got.Rewards) != 3 {
		t.Errorf("報酬数: got=%d, want=3", len(got.Rewards))
	}
}

func TestStudyRepository_ListSessionsByUser(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	studyRepo := repository.NewStudyRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "一覧テスト"}
	userRepo.Create(user)

	// 3件のセッションを作成
	now := time.Now().UTC()
	for i := 0; i < 3; i++ {
		s := &model.StudySession{
			UserID:          user.ID,
			StartedAt:       now.Add(time.Duration(-i) * time.Hour),
			EndedAt:         now.Add(time.Duration(-i)*time.Hour + 25*time.Minute),
			DurationSeconds: 1500,
			IsCompleted:     true,
		}
		db.Create(s)
	}

	sessions, err := studyRepo.ListSessionsByUser(user.ID, 10, 0)
	if err != nil {
		t.Fatalf("一覧取得に失敗: %v", err)
	}
	if len(sessions) != 3 {
		t.Errorf("セッション数: got=%d, want=3", len(sessions))
	}
}

func TestStudyRepository_GetDailyStudySeconds(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	studyRepo := repository.NewStudyRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "日次テスト"}
	userRepo.Create(user)

	today := time.Now().UTC()
	// 今日のセッションを2件作成
	for _, dur := range []int{1800, 3600} {
		s := &model.StudySession{
			UserID:          user.ID,
			StartedAt:       today,
			EndedAt:         today.Add(time.Duration(dur) * time.Second),
			DurationSeconds: dur,
			IsCompleted:     true,
		}
		db.Create(s)
	}

	total, err := studyRepo.GetDailyStudySeconds(user.ID, today)
	if err != nil {
		t.Fatalf("日次秒数取得に失敗: %v", err)
	}
	if total != 5400 {
		t.Errorf("日次合計: got=%d, want=5400", total)
	}
}

// ============================================================
// CharacterRepository のテスト
// ============================================================

func TestCharacterRepository_CreateAndList(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	charRepo := repository.NewCharacterRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "キャラテスト"}
	userRepo.Create(user)

	// マスタキャラ作成
	mc := &model.MasterCharacter{
		ID: uuid.New(), Name: "勇者", Rarity: 5,
		BaseHP: 100, BaseATK: 50, BaseDEF: 30,
		ImageURL: "https://example.com/hero.png", IsActive: true,
	}
	db.Create(mc)

	// ユーザーにキャラ付与
	uc := &model.UserCharacter{
		UserID:      user.ID,
		CharacterID: mc.ID,
		Level:       1,
		CurrentXP:   0,
		ObtainedAt:  time.Now().UTC(),
	}
	tx := db.Begin()
	if err := charRepo.Create(tx, uc); err != nil {
		tx.Rollback()
		t.Fatalf("キャラ付与に失敗: %v", err)
	}
	tx.Commit()

	// 一覧取得
	list, err := charRepo.ListByUser(user.ID)
	if err != nil {
		t.Fatalf("キャラ一覧取得に失敗: %v", err)
	}
	if len(list) != 1 {
		t.Fatalf("キャラ数: got=%d, want=1", len(list))
	}
	if list[0].Level != 1 {
		t.Errorf("レベル: got=%d, want=1", list[0].Level)
	}
}

func TestCharacterRepository_AddXP(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	charRepo := repository.NewCharacterRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "XPテスト"}
	userRepo.Create(user)
	mc := &model.MasterCharacter{ID: uuid.New(), Name: "魔法使い", Rarity: 3,
		BaseHP: 80, BaseATK: 70, BaseDEF: 20, ImageURL: "x", IsActive: true}
	db.Create(mc)

	uc := &model.UserCharacter{
		UserID: user.ID, CharacterID: mc.ID,
		Level: 1, CurrentXP: 0, ObtainedAt: time.Now().UTC(),
	}
	db.Create(uc)

	// XP 加算
	tx := db.Begin()
	charRepo.AddXP(tx, uc.ID, 100)
	tx.Commit()

	got, _ := charRepo.GetByID(uc.ID)
	if got.CurrentXP != 100 {
		t.Errorf("XP: got=%d, want=100", got.CurrentXP)
	}
}

// ============================================================
// PartyRepository のテスト
// ============================================================

func TestPartyRepository_UpsertAndGet(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	partyRepo := repository.NewPartyRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "パーティテスト"}
	userRepo.Create(user)

	mc := &model.MasterCharacter{ID: uuid.New(), Name: "戦士", Rarity: 4,
		BaseHP: 120, BaseATK: 60, BaseDEF: 50, ImageURL: "x", IsActive: true}
	db.Create(mc)
	uc := &model.UserCharacter{
		UserID: user.ID, CharacterID: mc.ID,
		Level: 5, CurrentXP: 200, ObtainedAt: time.Now().UTC(),
	}
	db.Create(uc)

	// スロット1にキャラを配置
	slot := &model.UserPartySlot{
		UserID: user.ID, SlotPosition: 1, UserCharacterID: uc.ID,
	}
	if err := partyRepo.Upsert(slot); err != nil {
		t.Fatalf("パーティ配置に失敗: %v", err)
	}

	// 取得
	slots, err := partyRepo.GetByUser(user.ID)
	if err != nil {
		t.Fatalf("パーティ取得に失敗: %v", err)
	}
	if len(slots) != 1 {
		t.Fatalf("スロット数: got=%d, want=1", len(slots))
	}
	if slots[0].SlotPosition != 1 {
		t.Errorf("スロット位置: got=%d, want=1", slots[0].SlotPosition)
	}
}

// ============================================================
// WeaponRepository のテスト
// ============================================================

func TestWeaponRepository_CreateAndList(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	weaponRepo := repository.NewWeaponRepository(db)

	user := &model.User{ID: uuid.New(), DisplayName: "武器テスト"}
	userRepo.Create(user)

	mw := &model.MasterWeapon{ID: uuid.New(), Name: "炎の剣", Rarity: 4,
		BaseATK: 80, ImageURL: "x", IsActive: true}
	db.Create(mw)

	uw := &model.UserWeapon{
		UserID: user.ID, WeaponID: mw.ID,
		Level: 1, ObtainedAt: time.Now().UTC(),
	}
	tx := db.Begin()
	weaponRepo.Create(tx, uw)
	tx.Commit()

	list, err := weaponRepo.ListByUser(user.ID)
	if err != nil {
		t.Fatalf("武器一覧取得に失敗: %v", err)
	}
	if len(list) != 1 {
		t.Errorf("武器数: got=%d, want=1", len(list))
	}
}
