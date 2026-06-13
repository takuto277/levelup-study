package service

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
	"gorm.io/gorm"
)

func TestPull_RaceConditionRollback(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := createGachaSvcForTest(t, db)

	user := createGachaTestUser(t, db, costPerPull)
	charID := uuid.New()
	createGachaTestChar(t, db, charID)
	bannerID := createGachaTestBanner(t, db, charID)

	// Normal pull with enough stones → success
	resp, err := svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err != nil {
		t.Fatalf("first pull failed: %v", err)
	}
	if resp.StonesSpent != costPerPull {
		t.Errorf("spent = %d, want %d", resp.StonesSpent, costPerPull)
	}

	var charCount int64
	db.Model(&model.UserCharacter{}).Where("user_id = ?", user.ID).Count(&charCount)
	if charCount != 1 {
		t.Errorf("char count = %d, want 1", charCount)
	}

	// Pull with insufficient stones → error, no items
	db.Model(&model.User{}).Where("id = ?", user.ID).Update("stones", 0)

	_, err = svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err == nil {
		t.Fatal("expected error for insufficient stones, got nil")
	}

	db.Model(&model.UserCharacter{}).Where("user_id = ?", user.ID).Count(&charCount)
	if charCount != 1 {
		t.Errorf("char count = %d, want 1 (no items from failed pull)", charCount)
	}

	var historyCount int64
	db.Model(&model.GachaHistory{}).Where("user_id = ?", user.ID).Count(&historyCount)
	if historyCount != 1 {
		t.Errorf("history count = %d, want 1", historyCount)
	}
}

func TestStoneUpdateRowsAffectedZeroRollsBack(t *testing.T) {
	db := testutil.SetupTestDB(t)

	user := &model.User{DisplayName: "test", Stones: 0}
	if err := db.Create(user).Error; err != nil {
		t.Fatalf("create user: %v", err)
	}

	err := db.Transaction(func(tx *gorm.DB) error {
		result := tx.Model(&model.User{}).
			Where("id = ? AND stones >= ?", user.ID, costPerPull).
			Update("stones", gorm.Expr("stones - ?", costPerPull))
		if result.RowsAffected == 0 {
			return gorm.ErrRecordNotFound
		}
		return nil
	})

	if err == nil {
		t.Fatal("expected error (RowsAffected==0), got nil")
	}

	var updated model.User
	db.First(&updated, user.ID)
	if updated.Stones != 0 {
		t.Errorf("stones = %d, want 0 (transaction rolled back)", updated.Stones)
	}
}

func createGachaSvcForTest(t *testing.T, db *gorm.DB) *GachaService {
	ur := repository.NewUserRepository(db)
	gr := repository.NewGachaRepository(db)
	mr := repository.NewMasterRepository(db)
	cr := repository.NewCharacterRepository(db)
	wr := repository.NewWeaponRepository(db)
	return NewGachaService(db, ur, gr, mr, cr, wr)
}

func createGachaTestUser(t *testing.T, db *gorm.DB, stones int) *model.User {
	u := &model.User{DisplayName: "test", Stones: stones}
	if err := repository.NewUserRepository(db).Create(u); err != nil {
		t.Fatalf("create user: %v", err)
	}
	return u
}

func createGachaTestChar(t *testing.T, db *gorm.DB, id uuid.UUID) {
	if err := db.Create(&model.MasterCharacter{
		ID: id, Name: "test", Rarity: 3, ImageURL: "http://x.com/c.png",
	}).Error; err != nil {
		t.Fatalf("create master char: %v", err)
	}
}

func createGachaTestBanner(t *testing.T, db *gorm.DB, charID uuid.UUID) uuid.UUID {
	id := uuid.New()
	rate := `[{"item_id":"` + charID.String() + `","result_type":"character","rarity":3,"rate":1.0}]`
	if err := db.Create(&model.MasterGachaBanner{
		ID: id, Name: "test", RateTable: []byte(rate),
		StartAt: time.Now().Add(-1 * time.Hour),
		EndAt:   time.Now().Add(1 * time.Hour),
		IsActive: true,
	}).Error; err != nil {
		t.Fatalf("create banner: %v", err)
	}
	return id
}
