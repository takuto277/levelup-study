package database

import (
	"fmt"
	"log"
	"os"

	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// ============================================================
// Connect — PostgreSQL（Supabase）への接続を確立する
//
// 環境変数:
//   DATABASE_URL — PostgreSQL 接続文字列
//     例: "host=localhost user=postgres password=pass dbname=levelup port=5432 sslmode=disable"
// ============================================================

func Connect() (*gorm.DB, error) {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		return nil, fmt.Errorf("DATABASE_URL 環境変数が設定されていません")
	}

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		return nil, fmt.Errorf("データベース接続に失敗: %w", err)
	}

	log.Println("✅ データベースに接続しました")
	return db, nil
}

// ============================================================
// AutoMigrate — 全テーブルのスキーマを自動マイグレーションする
//
// 開発用。本番では golang-migrate 等のツールを使うことを推奨。
// ============================================================

func AutoMigrate(db *gorm.DB) error {
	log.Println("🔄 テーブルのマイグレーションを実行中...")
	if err := db.AutoMigrate(model.AllModels()...); err != nil {
		return fmt.Errorf("マイグレーション失敗: %w", err)
	}
	log.Println("✅ マイグレーション完了")
	return nil
}
