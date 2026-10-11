# マスタ画像（Supabase Storage + DB image_url）

**バトル同梱スプライト**（`apps/mobile/assets/source/`）とは別系統です。  
ここで管理する画像は **アプリ再リリースなし** で差し替え可能（API が返す `image_url` を更新するだけ）。

## クイックスタート

1. PNG を `source/{entity}/{uuid}.png` に配置
2. `manifest.yaml` の `assets` にエントリ追加
3. 対象環境の Supabase で bucket `game-assets`（public）を作成
4. `backend/.env` に環境別の値を設定（下記）
5. 実行（環境ごとにコマンドが分かれている）:

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

### 環境別の `.env` 設定

| 環境 | DB | Supabase | bucket |
|------|-----|----------|--------|
| dev | `DEV_DATABASE_URL`（無ければ `DATABASE_URL`） | `DEV_SUPABASE_URL` / `DEV_SUPABASE_SERVICE_ROLE_KEY`（任意） | `DEV_SUPABASE_STORAGE_BUCKET` |
| stg | `STG_DATABASE_URL` | `STG_SUPABASE_URL` / `STG_SUPABASE_SERVICE_ROLE_KEY` | `STG_SUPABASE_STORAGE_BUCKET` |
| prod | `PROD_DATABASE_URL` | `PROD_SUPABASE_URL` / `PROD_SUPABASE_SERVICE_ROLE_KEY` | `PROD_SUPABASE_STORAGE_BUCKET` |

- dev は Storage を使いません。`DEV_ASSET_BASE_URL` を設定すると、その URL を基点に
  `image_url` を更新します（未設定なら DB 更新スキップ）。
- `--env` を指定しない従来の `make master-images-upload` も残っています（フラット変数）。

詳細: [`docs/assets/02_Supabase_Master_Images.md`](../../../docs/assets/02_Supabase_Master_Images.md)
