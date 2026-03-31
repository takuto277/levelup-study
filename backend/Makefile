# ============================================================
# LevelUp Study Backend — Makefile
# ============================================================

# デフォルトの .env ファイルを読み込む
ifneq (,$(wildcard .env))
	include .env
	export
endif

.PHONY: help setup run test build lint db-up db-down db-reset env

# --- ヘルプ ---
help: ## このヘルプを表示
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# --- セットアップ ---
setup: env db-up ## 初回セットアップ（.env作成 + DB起動）
	@echo "✅ セットアップ完了！ make run でサーバーを起動できます"

env: ## .env.example から .env を作成（既存なら何もしない）
	@test -f .env || (cp .env.example .env && echo "📝 .env を作成しました。必要に応じて値を編集してください")

# --- データベース ---
db-up: ## PostgreSQL を Docker で起動
	docker compose up -d
	@echo "⏳ PostgreSQL の起動を待機中..."
	@sleep 2
	@echo "✅ PostgreSQL 起動完了 (localhost:5432)"

db-down: ## PostgreSQL を停止
	docker compose down

db-reset: ## PostgreSQL を停止 + データ削除して再起動
	docker compose down -v
	docker compose up -d
	@echo "🔄 データベースをリセットしました"

# --- アプリケーション ---
run: ## サーバーを起動（ホットリロードなし）
	go run ./cmd/api

build: ## バイナリをビルド
	go build -o bin/api ./cmd/api

# --- テスト ---
test: ## 全テストを実行（SQLite in-memory）
	CGO_ENABLED=1 go test ./... -v -count=1

test-short: ## テストを簡易実行
	CGO_ENABLED=1 go test ./... -count=1

# --- コード品質 ---
lint: ## go vet を実行
	go vet ./...
