package testutil

import (
	"testing"

	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// ============================================================
// SetupTestDB — テスト用のインメモリ SQLite データベースを作成する
//
// 各テスト関数で呼び出すことで、テスト間のデータ干渉を防ぐ。
// テスト終了時に自動で破棄される。
// ============================================================

func SetupTestDB(t *testing.T) *gorm.DB {
	t.Helper()

	db, err := gorm.Open(sqlite.Open(":memory:"), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Silent),
	})
	if err != nil {
		t.Fatalf("テスト用DB接続に失敗: %v", err)
	}

	// 全テーブルをマイグレーション
	if err := db.AutoMigrate(model.AllModels()...); err != nil {
		t.Fatalf("テスト用マイグレーションに失敗: %v", err)
	}

	return db
}
