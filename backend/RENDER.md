# Render デプロイ（LevelUp Study API）

[Render](https://render.com/) の **無料 Web Service**（スリープあり）で Go API を動かす手順です。リポジトリルートの **`render.yaml`**（[Blueprint](https://render.com/docs/infrastructure-as-code)）でサービス定義しています。

## 自動デプロイ

GitHub リポジトリを接続すると、**`main` に push したあと**（かつ `buildFilter` の `backend/**` に該当する変更のとき）、Render がビルド・デプロイします。  
GitHub Actions の **`backend-ci.yml` はテストのみ**（デプロイは Render 側）。

## 初回セットアップ

1. [Render Dashboard](https://dashboard.render.com/) → **New** → **Blueprint** を選ぶ。  
2. **この GitHub リポジトリ**（`levelup-study`）を接続し、**`render.yaml` を検出**させる。  
3. ウィザードで **`DATABASE_URL` / `JWT_SECRET` / `API_KEY`** の値を入力する（`sync: false` のためダッシュボードで聞かれる）。  
4. デプロイ完了後、**`https://levelup-study-api.onrender.com`** のような URL が表示される（名前は `render.yaml` の `name` に依存）。

手動で Web Service だけ作る場合は、[Docker on Render](https://render.com/docs/docker) に従い、**Root Directory = `backend`**、**Dockerfile = そのディレクトリの `Dockerfile`** に合わせる。

## 無料枠の注意（スリープ）

無料 Web Service は **一定時間アクセスがないとスピンダウン**し、次のリクエストで **冷めた起動（コールドスタート）** が起きます。個人開発・少人数なら許容しやすい挙動です。

## 環境変数

| 変数 | 説明 |
|------|------|
| `DATABASE_URL` | Supabase 等の PostgreSQL URI |
| `JWT_SECRET` | Supabase JWT Secret |
| `API_KEY` | モバイルの `X-API-Key` |
| `DEV_MODE` | 本番では `false`（Blueprint で固定） |
| `DEBUG_API_LOG` | 本番では `false`（Blueprint で固定） |

`PORT` は Render が注入する。API は `os.Getenv("PORT")` を読む実装になっている。

## Render で `network is unreachable`（IPv6）になるとき

ログに **`dial tcp [2406:...]:5432`** のように **IPv6** が出て失敗する場合、**直結ホスト `db.xxx.supabase.co:5432`** が Render から届いていないことがあります。

**対処**: Supabase ダッシュボード → **Project Settings → Database** → **Connection string** のうち、**Connection pooling**（**Session** または **Transaction**）の URI をコピーし、Render の **`DATABASE_URL`** をそれに置き換える。ホストは多くの場合 **`*.pooler.supabase.com`**、ポートは **`6543`** です（[Supabase: Connect to your database](https://supabase.com/docs/guides/database/connecting-to-postgres)）。

ローカルの `psql` で IPv6 問題が出たときと同じ考え方で、`backend/README.md` の Session pooler の記述も参照してください。

## モノレポ

`rootDir: backend` により、[Root Directory](https://render.com/docs/monorepo-support#setting-a-root-directory) が `backend` になり、`dockerfilePath` / `dockerContext` は **そのディレクトリ基準**の `./Dockerfile` と `.` です。

## モバイル

デプロイ後の **HTTPS のオリジン**（例: `https://levelup-study-api.onrender.com`）を、モバイルの **`ApiRoutes.BASE_URL`**（`apps/mobile/shared/.../ApiRoutes.kt`）に合わせる。Render のサービス名を変えた場合はダッシュボードの URL に合わせて修正する。

## 関連ドキュメント

| ドキュメント | 内容 |
|-------------|------|
| [`docs/planning/02_Backend_Architecture.md`](../docs/planning/02_Backend_Architecture.md) | API 一覧・Tech Stack・データ同期 |
| [`docs/planning/01_Features_and_Roadmap.md`](../docs/planning/01_Features_and_Roadmap.md) | 機能ロードマップ・実装ステータス |
| [`docs/architecture/01_Overview.md`](../docs/architecture/01_Overview.md) | モノレポ全体構成（KMP / Go） |
| [`render.yaml`](../render.yaml) | Render Blueprint 定義 |

## ローカル開発

API のローカル実行は Docker Compose（PostgreSQL）+ `make run`。詳細は [`README.md`](README.md) の「ローカル開発セットアップ」。

## 関連ファイル

| パス | 内容 |
|------|------|
| `/render.yaml`（リポジトリルート） | Blueprint 定義 |
| `backend/Dockerfile` | コンテナイメージ |
