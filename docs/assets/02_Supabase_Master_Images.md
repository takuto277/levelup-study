# Supabase マスタ画像入稿フロー

立ち絵・武器・ダンジョンバナー・敵ポートレートなど、**API の `image_url` で配信する画像**の管理手順です。  
**モバイルアプリの再リリースは不要**（既存の Coil / AsyncImage が URL を読むだけ）。

バトル同梱スプライト（敵 walk 等）は [`01_Asset_Ingestion_Workflow.md`](01_Asset_Ingestion_Workflow.md) を参照。

---

## 2系統の使い分け

| 種別 | 例 | 正本 | 反映方法 |
|------|-----|------|----------|
| **同梱（ネイティブ）** | バトルスプライト・背景 | `apps/mobile/assets/source/` | アプリ再ビルド |
| **リモート（Supabase）** | ガチャ立ち絵・武器・バナー | `backend/assets/master/` | Storage + DB 更新のみ |

**実装の中心は backend / Supabase。** モバイルは `/master/*` API が返す `image_url` を表示するだけです。

---

## アーキテクチャ

```mermaid
flowchart LR
  A[PNG in backend/assets/master/source] --> B[manifest.yaml]
  B --> C[upload script]
  C --> D[Supabase Storage public bucket]
  C --> E[PostgreSQL image_url]
  E --> F[Go API /master/*]
  F --> G[Mobile AsyncImage / Coil]
```

公開 URL 形式:

```
https://{project-ref}.supabase.co/storage/v1/object/public/game-assets/master/{entity}/{uuid}.png
```

---

## 初回セットアップ（Supabase）

### 1. Storage バケット

1. [Supabase Dashboard](https://supabase.com/dashboard) → プロジェクト → **Storage**
2. **New bucket**
   - Name: `game-assets`（`manifest.yaml` の `bucket` と一致）
   - **Public bucket**: ON（ゲーム画像は公開 CDN 相当）

### 2. 環境変数（`backend/.env`）

環境別コマンドを使う場合は、環境ごとにプレフィックス付きの変数を設定します。

```bash
# --- stg ---
STG_DATABASE_URL=postgresql://postgres.[ref]:[password]@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres
STG_SUPABASE_URL=https://xxxxxxxx.supabase.co
STG_SUPABASE_SERVICE_ROLE_KEY=eyJ...   # service_role — モバイルに絶対入れない
STG_SUPABASE_STORAGE_BUCKET=game-assets

# --- prod ---
PROD_DATABASE_URL=postgresql://postgres.[ref]:[password]@aws-0-....pooler.supabase.com:6543/postgres
PROD_SUPABASE_URL=https://yyyyyyyy.supabase.co
PROD_SUPABASE_SERVICE_ROLE_KEY=eyJ...
PROD_SUPABASE_STORAGE_BUCKET=game-assets

# --- dev（ローカル DB のみ。DEV_SUPABASE_* は任意） ---
DEV_DATABASE_URL=host=localhost user=postgres password=postgres dbname=levelup_study port=5432 sslmode=disable
DEV_ASSET_BASE_URL=http://localhost:8080/assets
# DEV_SUPABASE_URL= / DEV_SUPABASE_SERVICE_ROLE_KEY=（Storage も使う場合のみ）

# --- 従来（--env 未指定の make master-images-upload 用） ---
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJ...
SUPABASE_STORAGE_BUCKET=game-assets
```

| キー | 用途 |
|------|------|
| `{ENV}_SUPABASE_URL` | Storage API のベース URL |
| `{ENV}_SUPABASE_SERVICE_ROLE_KEY` | upload スクリプト専用（RLS バイパス） |
| `{ENV}_DATABASE_URL` | `image_url` UPDATE 用（postgres:// 形式） |
| `{ENV}_SUPABASE_STORAGE_BUCKET` | 省略時 `manifest.bucket` |

### 3. バケットポリシー（public バケットなら通常不要）

Public バケットは匿名 GET が可能。アップロードは **service_role キーのみ**（スクリプトから）。

---

## 入稿手順

### 1. 画像を配置

```
backend/assets/master/source/
  characters/a0000000-0000-0000-0000-000000000001.png
  weapons/b0000000-0000-0000-0000-000000000001.png
  dungeons/d0000000-0000-0000-0000-000000000001.png
  monsters/7e000000-0000-0000-0000-000000000001.png
```

UUID は `backend/db/seed.sql` の該当マスタ ID と一致させる。

### 2. `manifest.yaml` を更新

```yaml
assets:
  - entity: characters
    id: a0000000-0000-0000-0000-000000000001
    file: source/characters/a0000000-0000-0000-0000-000000000001.png
    note: 光の勇者アリア
```

### 3. 検証 → dry-run → 本番アップロード

対象環境ごとにコマンドを分けています。`backend/.env` に環境別の値を設定してください。

```bash
cd backend
make master-images-validate

# dev: ローカル DB のみ（Storage には上げない）
make master-images-upload-dev DRY_RUN=1
make master-images-upload-dev

# stg: STG_SUPABASE_* + STG_DATABASE_URL へ入稿
make master-images-upload-stg DRY_RUN=1
make master-images-upload-stg

# prod: PROD_SUPABASE_* + PROD_DATABASE_URL へ入稿（確認必須）
CONFIRM_PROD_UPLOAD=yes make master-images-upload-prod DRY_RUN=1
CONFIRM_PROD_UPLOAD=yes make master-images-upload-prod
```

| 環境 | DB | Supabase URL / service_role | Storage |
|------|-----|------------------------------|---------|
| dev | `DEV_DATABASE_URL` → `DATABASE_URL` | `DEV_SUPABASE_URL` / `DEV_SUPABASE_SERVICE_ROLE_KEY`（任意） | `DEV_ASSET_BASE_URL` を基点に `image_url` 更新。未設定ならスキップ |
| stg | `STG_DATABASE_URL` | `STG_SUPABASE_URL` / `STG_SUPABASE_SERVICE_ROLE_KEY` | Storage へアップロード |
| prod | `PROD_DATABASE_URL` | `PROD_SUPABASE_URL` / `PROD_SUPABASE_SERVICE_ROLE_KEY` | Storage へアップロード（`CONFIRM_PROD_UPLOAD=yes` 必須） |

- bucket は `{ENV}_SUPABASE_STORAGE_BUCKET`（無ければ `manifest.bucket` = `game-assets`）。
- オブジェクトキー `master/{entity}/{id}.{ext}` は全環境で共通。
- `--env` を指定しない従来の `make master-images-upload` も残っています（フラット変数 `SUPABASE_URL` 等）。
- アップロードは **ローカル実行** で Render を経由しません（`docs/ENVIRONMENT.md` 相当の方針）。

### Storage のみ入稿（複数フレーム等・DB 更新なし）

複数フレームのキャラ画像（`character_{code}_{mode}_{frame}.webp` 等）のように `m_*` テーブルへ 1:1 対応しない画像は、**Storage のみ**へ入稿します（`image_url` は更新しません）。`upload.py` と同じ環境変数解決を再利用します。

```bash
# dry-run → 本番（stg の例）
make master-images-upload-storage-stg \
  DIR=backend/assets/master/source/characters PREFIX=master/characters DRY_RUN=1
make master-images-upload-storage-stg \
  DIR=backend/assets/master/source/characters PREFIX=master/characters
```

- `DIR` はリポジトリルート相対（`backend/.env` は `run.sh` が読み込み済み）。対象は png/webp/jpg。
- キーは `{PREFIX}/{filename}`（例 `master/characters/character_00001_idle_1.webp`）。
- `dev` / `prod` も `master-images-upload-storage-{dev,prod}`（prod は `CONFIRM_PROD_UPLOAD=yes` 必須）。

### 4. 動作確認

- API: `GET /master/characters` 等で `image_url` が Supabase URL になっているか
- モバイル: ガチャ・記録画面で画像が表示されるか（キャッシュクリアで確認）

---

## ベストプラクティス

1. **service_role キーは backend / CI のみ** — モバイル APK に含めない
2. **オブジェクトキーは UUID 固定** — `master/{entity}/{uuid}.png` で seed と 1:1
3. **上書きは upsert** — 同じキーで差し替え可能（アプリ更新不要）
4. **バトルスプライトは同梱のまま** — オフライン・低レイテンシが必要なものは `apps/mobile/assets/`
5. **PR では manifest + validate** — 実 PNG は容量次第で LFS または Supabase のみでも可
6. **キャッシュ** — URL を変えず上書きする場合、クライアント CDN キャッシュに注意（必要なら `?v=2` 等を将来検討）

---

## トラブルシューティング

| 症状 | 対処 |
|------|------|
| `Upload failed 400` | bucket 名・Public 設定・service_role を確認 |
| `DATABASE_URL は postgres://` | Supabase URI を `.env` に設定 |
| seed に無い UUID | 先に seed / マイグレーションでマスタ追加 |
| 画像が表示されない | API レスポンスの `image_url`、バケット Public、実機ネットワーク |

---

## 関連

- [`backend/assets/master/README.md`](../../backend/assets/master/README.md)
- [`docs/architecture/02_Database_Schema.md`](../architecture/02_Database_Schema.md)
- 同梱スプライト: [`01_Asset_Ingestion_Workflow.md`](01_Asset_Ingestion_Workflow.md)
