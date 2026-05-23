# マスタ画像（Supabase Storage + DB image_url）

**バトル同梱スプライト**（`apps/mobile/assets/source/`）とは別系統です。  
ここで管理する画像は **アプリ再リリースなし** で差し替え可能（API が返す `image_url` を更新するだけ）。

## クイックスタート

1. PNG を `source/{entity}/{uuid}.png` に配置
2. `manifest.yaml` の `assets` にエントリ追加
3. Supabase で bucket `game-assets`（public）を作成
4. `backend/.env` に `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` を設定
5. 実行:

```bash
cd backend
make master-images-validate
make master-images-upload DRY_RUN=1   # 確認
make master-images-upload             # 本番
```

詳細: [`docs/assets/02_Supabase_Master_Images.md`](../../../docs/assets/02_Supabase_Master_Images.md)
