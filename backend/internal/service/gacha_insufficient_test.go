package service

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
)

func TestPull_RowsAffectedRollback(t *testing.T) {
	db := testutil.SetupTestDB(t)

	userRepo := repository.NewUserRepository(db)
	gachaRepo := repository.NewGachaRepository(db)
	masterRepo := repository.NewMasterRepository(db)
	charRepo := repository.NewCharacterRepository(db)
	weaponRepo := repository.NewWeaponRepository(db)
	_ = repository.NewStudyRepository(db)

	svc := NewGachaService(db, userRepo, gachaRepo, masterRepo, charRepo, weaponRepo)

	user := &model.User{DisplayName: "test", Stones: 3 * costPerPull}
	if err := userRepo.Create(user); err != nil {
		t.Fatalf("create user: %v", err)
	}

	charID := uuid.New()
	if err := masterRepo.CreateCharacter(&model.MasterCharacter{
		ID: charID, Name: "test", Rarity: 3, ImageURL: "http://x.com/c.png",
	}); err != nil {
		t.Fatalf("create master char: %v", err)
	}

	bannerID := uuid.New()
	rateTable := `[{"item_id":"` + charID.String() + `","result_type":"character","rarity":3,"rate":1.0}]`
	if err := masterRepo.CreateBanner(&model.MasterGachaBanner{
		ID: bannerID, Name: "test", RateTable: []byte(rateTable),
		StartAt: time.Now().Add(-1 * time.Hour),
		EndAt:   time.Now().Add(1 * time.Hour),
		IsActive: true,
	}); err != nil {
		t.Fatalf("create banner: %v", err)
	}

	// First pull: should succeed
	_, err := svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err != nil {
		t.Fatalf("first pull: %v", err)
	}

	// Manually set stones to 0 to simulate race condition at transaction level
	if err := db.Model(user).Update("stones", 0).Error; err != nil {
		t.Fatalf("manual stone update: %v", err)
	}

	// Second pull: pre-check passes (read at line above was before update) but
	// transaction update at WHERE stones >= cost should have RowsAffected=0 → rollback
	_, err = svc.Pull(user.ID, GachaPullRequest{BannerID: bannerID, Count: 1})
	if err == nil {
		t.Error("expected error after RowsAffected=0, got nil")
	}

	// Verify no history was created from the failed pull
	var count int64
	db.Model(&model.GachaHistory{}).Where("user_id = ?", user.ID).Count(&count)
	if count != 1 {
		t.Errorf("expected 1 history row (first successful pull), got %d", count)
	}

	// Verify no duplicate character was created
	var charCount int64
	db.Model(&model.UserCharacter{}).Where("user_id = ?", user.ID).Count(&charCount)
	if charCount != 1 {
		t.Errorf("expected 1 user character, got %d", charCount)
	}
}
