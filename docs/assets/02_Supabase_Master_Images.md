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

```bash
# 既存
DATABASE_URL=postgresql://postgres.[ref]:[password]@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres

# 追加（Settings > API）
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJ...   # service_role — モバイルに絶対入れない
SUPABASE_STORAGE_BUCKET=game-assets
```

| キー | 用途 |
|------|------|
| `SUPABASE_URL` | Storage API のベース URL |
| `SUPABASE_SERVICE_ROLE_KEY` | upload スクリプト専用（RLS バイパス） |
| `DATABASE_URL` | `image_url` UPDATE 用（postgres:// 形式） |

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

```bash
cd backend
make master-images-validate
make master-images-upload DRY_RUN=1
make master-images-upload
```

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
