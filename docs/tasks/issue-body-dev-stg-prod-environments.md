# Issue: dev / stg / 本番の3環境構成（Docker ローカル → Render stg → Render 本番）

## 背景

現状、バックエンドは **ローカル Docker Compose（PostgreSQL）+ `make run`** と、Render 上の **単一 Web Service 1 本**（`levelup-study-api.onrender.com`）のみ。厳密な stg / 本番の分離はなく、モバイルの `ApiRoutes.BASE_URL` も本番相当 URL 固定。

開発フローとしては次のイメージを想定している:

1. **ローカル（dev）** — Docker を立ち上げ、DB・API をコンテナ／Compose 上で実装・検証
2. **ステージング（stg）** — Docker で実装が固まったら Render にデプロイし、本番に近い環境で結合確認・修正
3. **本番（prod）** — stg で問題なければ本番 Render にリリース

API の向き先（モバイルの `BASE_URL`、Supabase プロジェクト、API Key 等）を環境ごとに切り替える方針でよいか検討する。

## 目的

- ローカル Docker を **日常の実装環境** として確立する
- Render 上に **stg 用サービス** を用意し、本番リリース前の確認場所を作る
- 将来 **本番用サービス** を stg と分離し、安全にリリースできるようにする

## スコープ（本イシュー）

### インフラ（Render）

- [ ] `render.yaml` に stg / prod 用 Web Service を定義（または Blueprint 2 サービス）
  - 例: `levelup-study-api-stg` / `levelup-study-api-prod`
- [ ] 環境ごとの `DATABASE_URL`（Supabase: stg 用 DB / prod 用 DB の分離方針）
- [ ] 環境ごとの `API_KEY` / `JWT_SECRET` / `DEV_MODE` 設定
- [ ] stg は `DEBUG_API_LOG` オン可、prod はオフ固定 等のポリシー

### ローカル（Docker）

- [ ] `docker-compose.yml` で API コンテナも起動できる構成（現状は PostgreSQL のみ）
- [ ] `make` ターゲットで dev 一式起動（db + api + seed）のドキュメント化
- [ ] `.env.example` に dev 用の変数テンプレート

### モバイル（KMP）

- [ ] `ApiRoutes.BASE_URL` をビルドフレーバー / `local.properties` / xcconfig で切替
  - dev: `http://localhost:8080` または LAN IP
  - stg: `https://levelup-study-api-stg.onrender.com`
  - prod: `https://levelup-study-api.onrender.com`
- [ ] Supabase プロジェクト（Auth / JWT）も stg / prod で分けるか方針決定

### ドキュメント

- [ ] `backend/README.md` / `RENDER.md` に dev → stg → prod フローを追記
- [ ] `.rulesync/rules/server.md` に 3 環境の定義を反映

## 現状メモ

| 項目 | 現状 |
|------|------|
| ローカル DB | `docker compose`（PostgreSQL 16） |
| ローカル API | ホスト上 `make run`（コンテナ外） |
| Render | サービス 1 本、`main` push でデプロイ |
| モバイル API URL | `ApiRoutes.kt` に Render URL ハードコード |
| 本番環境 | 厳密な prod 分離なし（実質 1 本が本番兼用） |

## 受け入れ条件

- ローカル Docker だけで API + DB の開発が完結できる
- stg Render URL にモバイルを向けて E2E 確認できる
- prod は stg とは別サービス・別 DB（または別 Supabase プロジェクト）で運用方針がドキュメント化されている
- 開発者が「今どの環境を向いているか」を README から判断できる

## 優先度

**後回し** — 設計・起票のみ。実装は別 PR で行う。

## 参照

- `render.yaml`
- `backend/docker-compose.yml`
- `apps/mobile/shared/.../ApiRoutes.kt`
- `docs/planning/02_Backend_Architecture.md`
