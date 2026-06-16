package repository

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
)

func TestGachaRepositoryListByBannerPaginates(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := NewGachaRepository(db)

	userID := uuid.New()
	bannerID := uuid.New()
	otherBannerID := uuid.New()

	now := time.Now().UTC()
	// bannerID に 5 件、otherBannerID に 3 件作成
	for i := 1; i <= 5; i++ {
		if err := db.Create(&model.GachaHistory{
			ID:           uuid.New(),
			UserID:       userID,
			BannerID:     bannerID,
			ResultType:   "character",
			ResultItemID: uuid.New(),
			PityCount:    i,
			CreatedAt:    now.Add(time.Duration(i) * time.Second),
		}).Error; err != nil {
			t.Fatalf("create history failed: %v", err)
		}
	}
	for i := 1; i <= 3; i++ {
		if err := db.Create(&model.GachaHistory{
			ID:           uuid.New(),
			UserID:       userID,
			BannerID:     otherBannerID,
			ResultType:   "weapon",
			ResultItemID: uuid.New(),
			PityCount:    i,
			CreatedAt:    now.Add(-time.Duration(i) * time.Second),
		}).Error; err != nil {
			t.Fatalf("create history for other banner failed: %v", err)
		}
	}

	// limit=2 のページング
	page1, err := repo.ListByBanner(userID, bannerID, 2, 0)
	if err != nil {
		t.Fatalf("ListByBanner page1 failed: %v", err)
	}
	if len(page1) != 2 {
		t.Fatalf("page1 len = %d, want 2", len(page1))
	}

	page2, err := repo.ListByBanner(userID, bannerID, 2, 2)
	if err != nil {
		t.Fatalf("ListByBanner page2 failed: %v", err)
	}
	if len(page2) != 2 {
		t.Fatalf("page2 len = %d, want 2", len(page2))
	}

	page3, err := repo.ListByBanner(userID, bannerID, 2, 4)
	if err != nil {
		t.Fatalf("ListByBanner page3 failed: %v", err)
	}
	if len(page3) != 1 {
		t.Fatalf("page3 len = %d, want 1", len(page3))
	}

	// offset を超えると空
	page4, err := repo.ListByBanner(userID, bannerID, 2, 6)
	if err != nil {
		t.Fatalf("ListByBanner page4 failed: %v", err)
	}
	if len(page4) != 0 {
		t.Fatalf("page4 len = %d, want 0", len(page4))
	}

	// banner_id 指定なしの ListByUser も件数が変わっていないことを確認（8件）
	all, err := repo.ListByUser(userID, 100, 0)
	if err != nil {
		t.Fatalf("ListByUser failed: %v", err)
	}
	if len(all) != 8 {
		t.Fatalf("ListByUser all = %d, want 8", len(all))
	}
}

func TestGachaRepositoryListByUserPaginates(t *testing.T) {
	db := testutil.SetupTestDB(t)
	repo := NewGachaRepository(db)

	userID := uuid.New()
	otherUserID := uuid.New()
	bannerID := uuid.New()

	now := time.Now().UTC()
	for i := 1; i <= 5; i++ {
		if err := db.Create(&model.GachaHistory{
			ID:           uuid.New(),
			UserID:       userID,
			BannerID:     bannerID,
			ResultType:   "character",
			ResultItemID: uuid.New(),
			PityCount:    i,
			CreatedAt:    now.Add(time.Duration(i) * time.Second),
		}).Error; err != nil {
			t.Fatalf("create history failed: %v", err)
		}
	}
	// 他ユーザーの履歴が混ざっても取得されないことを確認
	if err := db.Create(&model.GachaHistory{
		ID:           uuid.New(),
		UserID:       otherUserID,
		BannerID:     bannerID,
		ResultType:   "character",
		ResultItemID: uuid.New(),
		PityCount:    99,
		CreatedAt:    now,
	}).Error; err != nil {
		t.Fatalf("create other user history failed: %v", err)
	}

	page, err := repo.ListByUser(userID, 2, 0)
	if err != nil {
		t.Fatalf("ListByUser failed: %v", err)
	}
	if len(page) != 2 {
		t.Fatalf("page len = %d, want 2", len(page))
	}

	// 他ユーザーの pity_count=99 が含まれていないことを確認
	for _, h := range page {
		if h.PityCount == 99 {
			t.Fatal("other user's history leaked into result")
		}
	}
}
