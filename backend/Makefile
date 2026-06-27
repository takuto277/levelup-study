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

.PHONY: help setup run test build lint db-up db-down db-reset env migrate-up migrate-down migrate-create migrate-version seed seed-remote seed-stg seed-prod db-fresh mint-dev-jwt master-images-validate master-images-upload

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

seed: ## シードをローカル Docker Postgres（levelup-study-db）へ投入。Supabase へは make seed-remote
	@echo "🌱 シードデータを投入中（ローカル Docker）..."
	docker exec -i levelup-study-db psql -U postgres -d levelup_study < db/seed.sql
	@echo "✅ シードデータの投入が完了しました"

# .env の DATABASE_URL 先へ db/seed.sql を流す（Supabase 等）。
# 前提: テーブルが既にあること（少なくとも一度 make run で AutoMigrate 済み）。
# 優先: ホストの psql（brew install libpq）。無ければ Docker の postgres:16 で実行。
# --ipv4 は psql 15+ のみ対応。古いクライアントでは付けずに実行（IPv6 で失敗する場合は brew upgrade libpq か Session pooler URI を試す）。
seed-remote: ## シードを .env の DATABASE_URL 先へ投入（Supabase 用）
	@test -n "$(DATABASE_URL)" || (echo '✖ DATABASE_URL が空です。.env を確認してください。' && exit 1)
	@echo "🌱 シードを DATABASE_URL の接続先へ投入中..."
	@echo "   → この後の make run も同じ backend/.env の DATABASE_URL で。別DBにシードすると API とアプリの user id が噛み合わない。"
	@if command -v psql >/dev/null 2>&1; then \
		if psql --help 2>&1 | grep -q -- '--ipv4'; then \
			psql --ipv4 "$(DATABASE_URL)" -v ON_ERROR_STOP=1 -f db/seed.sql; \
		else \
			psql "$(DATABASE_URL)" -v ON_ERROR_STOP=1 -f db/seed.sql; \
		fi; \
	else \
		echo "(psql が無いため Docker postgres:16 で実行します)"; \
		docker run --rm -i \
			-e DATABASE_URL="$(DATABASE_URL)" \
			-v "$(CURDIR)/db/seed.sql:/seed.sql:ro" \
			postgres:16 \
			sh -c 'if psql --help 2>&1 | grep -q -- --ipv4; then psql --ipv4 "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f /seed.sql; else psql "$$DATABASE_URL" -v ON_ERROR_STOP=1 -f /seed.sql; fi'; \
	fi
	@echo "✅ リモート DB へのシードが完了しました"

seed-stg: ## シードを stg 環境 (STG_DATABASE_URL) へ投入
	@test -n "$(STG_DATABASE_URL)" || (echo '✖ STG_DATABASE_URL が空です。.env の STG_DATABASE_URL に Render stg サービスの DATABASE_URL を設定してください。' && exit 1)
	@echo "🌱 stg 環境へシード投入中..."
	@$(MAKE) seed-remote DATABASE_URL="$(STG_DATABASE_URL)"

seed-prod: ## シードを prod 環境 (PROD_DATABASE_URL) へ投入
	@test -n "$(PROD_DATABASE_URL)" || (echo '✖ PROD_DATABASE_URL が空です。.env の PROD_DATABASE_URL に Render prod サービスの DATABASE_URL を設定してください。' && exit 1)
	@echo "🌱 prod 環境へシード投入中...（本番 DB を上書きします。注意！）"
	@$(MAKE) seed-remote DATABASE_URL="$(PROD_DATABASE_URL)"

db-fresh: db-reset ## DB完全リセット → migrate up → AutoMigrate → seed
	@echo "⏳ PostgreSQL 起動待ち..."
	@sleep 3
	@$(MAKE) migrate-up
	@echo "⏳ テーブル作成のため API を一時起動（GORM AutoMigrate）..."
	@timeout 12 $(MAKE) run 2>/dev/null || true
	@$(MAKE) seed
	@echo "✅ DB完全リフレッシュ完了（migrate + AutoMigrate + seed）"

# --- アプリケーション ---
mint-dev-jwt: ## seed の userId 向け HS256 JWT を 1 行で出力（apps/mobile/local.properties の dev.jwt に貼る）
	go run ./cmd/mintdevjwt

run: ## サーバーを起動（ホットリロードなし）
	go run ./cmd/api

build: ## バイナリをビルド
	go build -o bin/api ./cmd/api

# --- テスト ---
test: ## 全テストを実行（SQLite in-memory）
	CGO_ENABLED=1 go test ./... -v -count=1

test-short: ## テストを簡易実行
	CGO_ENABLED=1 go test ./... -count=1

# --- Supabase マスタ画像 ---
master-images-validate: ## manifest / source の整合性を検証
	@../scripts/master-images/run.sh ../scripts/master-images/validate.py

master-images-upload: ## Supabase Storage へ upload + image_url 更新（DRY_RUN=1 で確認）
	@../scripts/master-images/run.sh ../scripts/master-images/upload.py

# --- コード品質 ---
lint: ## go vet を実行
	go vet ./...
