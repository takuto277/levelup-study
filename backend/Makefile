# ============================================================
# LevelUp Study Backend — Makefile
# ============================================================

# デフォルトの .env ファイルを読み込む
ifneq (,$(wildcard .env))
	include .env
	export
endif

MIGRATE_IMAGE ?= migrate/migrate:v4.18.2
# Docker からホストの Postgres へ（Linux は host-gateway で host.docker.internal を解決）
MIGRATE_DATABASE_URL ?= postgres://postgres:postgres@host.docker.internal:5432/levelup_study?sslmode=disable

.PHONY: help setup run test build lint db-up db-down db-reset env migrate-up migrate-down migrate-create migrate-version seed db-fresh

# --- ヘルプ ---
help: ## このヘルプを表示
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# --- セットアップ ---
setup: env db-up migrate-up ## 初回セットアップ（.env + DB起動 + golang-migrate baseline）
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

migrate-up: ## golang-migrate で未適用の .up.sql を適用（Docker 利用、ローカル DB 前提）
	@echo "📂 マイグレーション適用中... ($(MIGRATE_IMAGE))"
	docker run --rm \
		--add-host=host.docker.internal:host-gateway \
		-v "$(CURDIR)/db/migrations:/migrations" \
		$(MIGRATE_IMAGE) \
		-path=/migrations \
		-database "$(MIGRATE_DATABASE_URL)" \
		up
	@echo "✅ migrate up 完了"

migrate-down: ## マイグレーションを 1 段階戻す（.down.sql）
	docker run --rm \
		--add-host=host.docker.internal:host-gateway \
		-v "$(CURDIR)/db/migrations:/migrations" \
		$(MIGRATE_IMAGE) \
		-path=/migrations \
		-database "$(MIGRATE_DATABASE_URL)" \
		down 1
	@echo "✅ migrate down 1 完了"

migrate-version: ## 現在のマイグレーションバージョンを表示
	docker run --rm \
		--add-host=host.docker.internal:host-gateway \
		-v "$(CURDIR)/db/migrations:/migrations" \
		$(MIGRATE_IMAGE) \
		-path=/migrations \
		-database "$(MIGRATE_DATABASE_URL)" \
		version

migrate-create: ## 新規 SQL マイグレーションを生成（例: make migrate-create NAME=add_user_skills）
	@test -n "$(NAME)" || (echo 'Usage: make migrate-create NAME=add_user_skills' && exit 1)
	docker run --rm \
		-v "$(CURDIR)/db/migrations:/migrations" \
		$(MIGRATE_IMAGE) \
		create -ext sql -dir /migrations -seq $(NAME)
	@echo "✅ db/migrations/ にファイルを生成しました"

seed: ## シードデータを投入（マスタ + テストユーザー）
	@echo "🌱 シードデータを投入中..."
	docker exec -i levelup-study-db psql -U postgres -d levelup_study < db/seed.sql
	@echo "✅ シードデータの投入が完了しました"

db-fresh: db-reset ## DB完全リセット → migrate up → AutoMigrate → seed
	@echo "⏳ PostgreSQL 起動待ち..."
	@sleep 3
	@$(MAKE) migrate-up
	@echo "⏳ テーブル作成のため API を一時起動（GORM AutoMigrate）..."
	@timeout 12 $(MAKE) run 2>/dev/null || true
	@$(MAKE) seed
	@echo "✅ DB完全リフレッシュ完了（migrate + AutoMigrate + seed）"

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
