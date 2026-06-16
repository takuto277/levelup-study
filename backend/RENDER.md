# Render デプロイフロー

## 環境一覧

| 環境 | Render Service | API URL | DEV_MODE | DEBUG_API_LOG |
|------|---------------|---------|----------|---------------|
| dev  | ローカル Docker (`docker compose up`) | localhost:8080 | true | true |
| stg  | `levelup-study-api-stg` | api-stg.onrender.com | false | true |
| prod | `levelup-study-api` | api.onrender.com | false | false |

## デプロイ手順

1. ローカルで実装・検証 (`docker compose up`)
2. PR 作成 → レビュー → マージ → `main` ブランチ更新
3. Render が `main` push を検知して stg に自動デプロイ
4. stg で E2E 確認（モバイルアプリの BASE_URL を stg に向ける）
5. 問題なければ Render Dashboard から prod にも手動デプロイ

## モバイルアプリの向き先切替

`apps/mobile/shared/.../ApiRoutes.kt` の `BASE_URL` を環境に応じて切り替え:
- dev: `http://localhost:8080` (iOS simulator) / `http://10.0.2.2:8080` (Android emulator)
- stg: `https://levelup-study-api-stg.onrender.com`
- prod: `https://levelup-study-api.onrender.com`

## 環境変数

| 変数 | dev | stg | prod |
|------|-----|-----|------|
| `DEV_MODE` | true | false | false |
| `DEBUG_API_LOG` | true | true | false |
| `ENV` | dev | staging | production |
| `JWT_SECRET` | 開発用固定値 | Supabase JWT Secret | Supabase JWT Secret |
| `API_KEY` | 開発用固定値 | ランダム文字列 | ランダム文字列 |
