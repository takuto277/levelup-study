# キャラ画像 WebP 化 & stg Storage 入稿

作成日: 2026-10-11
ブランチ: `feat/character-webp-assets`

## 目的

ガチャ立ち絵用に生成済みのキャラ複数フレーム画像（`character_00001_*`、12枚）を配信向けに WebP 化し、stg 環境の Supabase Storage へ入稿する。

## 背景・制約

- 生成AIの出力は 512×512 PNG（透過済み）で、12枚合計 約4.78MB。配信効率が悪い。
- 既存の入稿ツール `scripts/master-images/upload.py` は manifest の `entity` + UUID `id` を前提とし、`m_characters.image_url`（UUID 1列）を更新する。複数フレームや `code`（`character_00001`）には対応していない。
- `m_characters` に複数フレームを紐付けるスキーマ（`character_sprites` 等の新設）は未整備。今回は DB 更新を行わず **Storage のみ** に入稿する。

## 方針

1. `scripts/assets/.venv`（Pillow / webp 対応）で PNG → WebP(q=85) に変換し、`source/characters/` を `.webp` に置換。
2. Storage 専用の入稿スクリプト `scripts/master-images/upload_storage.py` を追加。環境変数の解決は `upload.py` の `--env` と同じロジックを再利用し、DB 更新は行わない。
3. Makefile に `master-images-upload-storage-{dev,stg,prod}` を追加（prod は `CONFIRM_PROD_UPLOAD=yes` 必須）。

## 実装詳細（変更内容と理由）

- **WebP q=85（lossy）**: 透過スプライトで実測 `4.78MB → 502KB（約11%）`。可逆は容量が大きくなるため、視認差の小さい q=85 を採用。
- **`.webp` に置換**: manifest / `file_extension()` は webp を許可済みで、パイプライン変更なしで扱える。元 PNG は削除。
- **`upload_storage.py` を新設（upload.py にモード追加しない）**: `upload.py` は「manifest エントリ（UUID）→ Storage + `image_url` UPDATE」が責務。多フレーム画像は UUID を持たず `UPDATE ... WHERE id=<uuid>` が成立しないため、DB 非依存の別スクリプトにして責務を分離。`resolve_config` / `upload_file` / `content_type_for` は import して重複実装を避ける。
- **キーは `master/{prefix}/{filename}`**: 既存の `master/{entity}/{id}.{ext}` と一貫させつつ、ファイル名にモード・フレームを含める。

## 手順

```bash
# 1. WebP 変換（scripts/assets/.venv / Pillow）
#    実測: 4.78MB -> 502KB
# 2. 確認
make master-images-upload-storage-stg \
  DIR=backend/assets/master/source/characters PREFIX=master/characters DRY_RUN=1
# 3. 入稿
make master-images-upload-storage-stg \
  DIR=backend/assets/master/source/characters PREFIX=master/characters
```

## 完了条件

- [x] 12枚を WebP 化し `.webp` に置換
- [x] stg Storage（bucket `game-assets`, prefix `master/characters/`）へ入稿
- [x] 公開 URL が HTTP 200 / `content-type: image/webp` を返す

## 後続（未対応）

- `character_sprites` 等のスキーマ整備と、アプリからの複数フレーム参照。
- `character_00001`（`code`）と `m_characters` の対応付け。

## 実機確認

- `https://njjmhkmaxyceplecifwz.supabase.co/storage/v1/object/public/game-assets/master/characters/character_00001_idle_1.webp` → 200 / image/webp / 36814 bytes
- 同 attack_3 (48938) / walk_2 (42256) も 200。
