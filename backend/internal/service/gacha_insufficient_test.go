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

// TestPull_InsufficientStones calls Pull() directly and verifies
// that insufficient stones trigger an error with no items/history created.
func TestPull_InsufficientStones(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := createGachaSvcForTest(t, db)
	user := createGachaTestUser(t, db, 0) // no stones
	charID := uuid.New()
	createGachaTestChar(t, db, charID)
	bannerID := createGachaTestBanner(t, db, charID)

	_, err := svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err == nil {
		t.Fatal("expected error for insufficient stones, got nil")
	}

	var count int64
	db.Model(&model.GachaHistory{}).Where("user_id = ?", user.ID).Count(&count)
	if count != 0 {
		t.Errorf("history = %d, want 0 (rollback)", count)
	}
	db.Model(&model.UserCharacter{}).Where("user_id = ?", user.ID).Count(&count)
	if count != 0 {
		t.Errorf("chars = %d, want 0 (rollback)", count)
	}
}

// TestRowsAffectedZeroCausesRollback calls the same deductStonesTx
// helper that GachaService.Pull uses internally.
func TestRowsAffectedZeroCausesRollback(t *testing.T) {
	db := testutil.SetupTestDB(t)
	user := &model.User{DisplayName: "test", Stones: 0}
	db.Create(user)

	err := db.Transaction(func(tx *gorm.DB) error {
		return deductStonesTx(tx, user.ID, costPerPull)
	})

	if err == nil {
		t.Fatal("RowsAffected==0 should return error")
	}

	var u model.User
	db.First(&u, user.ID)
	if u.Stones != 0 {
		t.Errorf("stones = %d, want 0 (rolled back)", u.Stones)
	}
}

// TestPull_Normal pulls with sufficient stones and verifies items created.
func TestPull_Normal(t *testing.T) {
	db := testutil.SetupTestDB(t)
	svc := createGachaSvcForTest(t, db)
	user := createGachaTestUser(t, db, 200)
	charID := uuid.New()
	createGachaTestChar(t, db, charID)
	bannerID := createGachaTestBanner(t, db, charID)

	resp, err := svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err != nil {
		t.Fatalf("pull: %v", err)
	}
	if len(resp.Results) != 1 {
		t.Errorf("results = %d, want 1", len(resp.Results))
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
