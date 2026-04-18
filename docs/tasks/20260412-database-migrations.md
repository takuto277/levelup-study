# タスク: golang-migrate による DB マイグレーション運用の導入

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-12 |
| ステータス | 完了 |
| 担当 | AI |

## 概要

Docker ローカル開発でアプリが通る状態から、スキーマ変更を **バージョン付き SQL** で追跡できるようにする。初回は GORM AutoMigrate と併用し、将来の「スキル」追加などは `db/migrations` に DDL を積む流れにする。

## 要件

- [x] `golang-migrate` 形式の `db/migrations/` を追加する
- [x] `Makefile` から `migrate-up` / `migrate-down` / `migrate-create` / `migrate-version` を実行できる
- [x] `make setup` / `make db-fresh` に `migrate-up` を組み込む
- [x] ドキュメント（backend README + seed 前提コメント）を更新する

## 影響範囲

- `backend/db/migrations/*`
- `backend/Makefile`
- `backend/.env.example`
- `backend/db/seed.sql`（コメント）
- `backend/internal/database/database.go`（コメント）
- `backend/README.md`

## 実装手順

1. baseline の `000001_baseline.{up,down}.sql` を追加（スキーマは触らず `SELECT 1` のみ）
2. Makefile で `migrate/migrate` Docker イメージを呼び出し、`host.docker.internal` + `host-gateway` でホスト Postgres に接続
3. `db-fresh` を `migrate-up` → 一時 `run`（AutoMigrate）→ `seed` の順に変更
4. README と seed の前提説明を更新

## 実装詳細

### baseline を空に近く保つ理由

既存プロジェクトは GORM AutoMigrate がテーブル定義のソースになっている。いきなり全 DDL を SQL に移すと二重管理と乖離リスクが大きい。まず **golang-migrate のバージョン表だけ導入**し、以降の変更（例: `user_skills` テーブル）から `000002_*.sql` で積む方針とした。

### Docker で migrate を実行する理由

Go モジュールに `migrate` をライブラリ依存として取り込まず、**公式イメージ**で揃えると CI/ローカルでバージョンが一致し、`go install` 不要で済む。

### `MIGRATE_DATABASE_URL`

`DATABASE_URL` は libpq の `key=value` 形式だが、golang-migrate は **`postgres://` URI** を推奨するため、Makefile デフォルトで `postgres://postgres:postgres@host.docker.internal:5432/levelup_study?sslmode=disable` を渡す。Supabase 等では `.env` に `MIGRATE_DATABASE_URL` を上書きする。

## 今後の作業例（スキル追加時）

```bash
cd backend
make migrate-create NAME=add_user_skills
# 生成された 000002_add_user_skills.up.sql に CREATE TABLE / ALTER TABLE を記述
# .down.sql に逆操作を記述
make migrate-up
# 対応する GORM モデルを internal/model に追加し、必要なら AutoMigrate で列を補完
```

本番で AutoMigrate を止める場合は、baseline 以降の SQL にテーブル一式を移し切ってから `SKIP_AUTOMIGRATE` のようなフラグを検討する。
