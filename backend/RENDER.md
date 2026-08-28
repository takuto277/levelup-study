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

## Supabase（Guest 認証）の向き先

モバイルの Guest Session（Supabase Anonymous Sign-In）は API 環境に応じて別の Supabase プロジェクトを使用する。

| 環境 | Supabase 設定キー（local.properties） | 環境変数（ビルド時） |
|------|--------------------------------------|----------------------|
| dev  | `dev.supabase.url` / `dev.supabase.anon.key`（旧 `supabase.url` 互換） | `LEVELUP_SUPABASE_URL` / `LEVELUP_SUPABASE_ANON_KEY` |
| stg  | `stg.supabase.url` / `stg.supabase.anon.key` | `LEVELUP_STG_SUPABASE_URL` / `LEVELUP_STG_SUPABASE_ANON_KEY` |
| prod | `prod.supabase.url` / `prod.supabase.anon.key` | `LEVELUP_PROD_SUPABASE_URL` / `LEVELUP_PROD_SUPABASE_ANON_KEY` |

- デバッグビルドは `SupabaseConfigSelector.selectForEnvironment()` で dev/stg を切り替える
- リリースビルドは `SupabaseConfigSelector.initialize(isDebug=false)` で常に prod の Supabase を使用する
- 各環境の Supabase プロジェクトは分離する（ユーザー・ガチャ・勉強データが混ざらないように）

### iOS: Edit Scheme での環境切替

iOS は共有 Scheme が3つあり、Xcode の Edit Scheme（環境変数）で環境を切り替える。

| Scheme | ビルド構成 | LEVELUP_ENV | 接続先 |
|--------|-----------|-------------|--------|
| `iosApp` | Debug | `dev` | localhost:8080 |
| `iosApp-stg` | Debug | `stg` | api-stg.onrender.com |
| `iosApp-prod` | Release | （無し → prod 固定） | api.onrender.com |

- Scheme は `apps/mobile/iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/` に共有されている
- 手動で切り替える場合は Edit Scheme → Run → Environment Variables → `LEVELUP_ENV` に `dev` / `stg` / `prod` を設定する
- `LEVELUP_ENV` が未設定の場合、Debug ビルドは保存済み環境（デフォルト dev）、Release ビルドは prod が使われる

## 環境変数

| 変数 | dev | stg | prod |
|------|-----|-----|------|
| `DEV_MODE` | true | false | false |
| `DEBUG_API_LOG` | true | true | false |
| `ENV` | dev | staging | production |
| `JWT_SECRET` | 開発用固定値 | Supabase JWT Secret | Supabase JWT Secret |
| `API_KEY` | 開発用固定値 | ランダム文字列 | ランダム文字列 |
| `SUPABASE_URL` | ローカル未使用 | stg 用 Supabase URL | 本番用 Supabase URL |

> `SUPABASE_URL` はバックエンドが JWKS（JWT 署名鍵）を取得するために使用する（`cmd/api/main.go` の `extractSupabaseProjectRef`）。stg / prod それぞれの Render サービスに、対応する Supabase プロジェクトの URL を設定すること。
