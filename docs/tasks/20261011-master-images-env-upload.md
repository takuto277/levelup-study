# タスク: マスタ画像を dev/stg/prod 別コマンドで入稿する

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-10-11 |
| ステータス | 進行中 |
| Issue | （未作成） |

## 概要

現状 `make master-images-upload` は単一の `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` /
`DATABASE_URL` しか見ないため、環境（dev/stg/prod）を切り替えるには `backend/.env` を
手で書き換える必要がある。環境ごとのコマンドに分け、誤った環境へ誤爆しないようにする。

## 要件

- [ ] `make master-images-upload-dev` / `-stg` / `-prod` の3コマンドで環境を分ける
- [ ] dev はローカル DB のみ（Storage へは上げない）
- [ ] stg / prod はそれぞれの Supabase Storage + DB へ入稿
- [ ] prod は誤爆防止のため `CONFIRM_PROD_UPLOAD=yes` を必須にする（`seed-prod` と同様）
- [ ] `backend/.env` 未設定時にどの変数が足りないか明示する

## 影響範囲

- `scripts/master-images/upload.py`（`--env` 対応）
- `backend/Makefile`（環境別ターゲット追加）
- `backend/.env.example`（環境別変数の追記）
- `docs/assets/02_Supabase_Master_Images.md` / `backend/assets/master/README.md`

## 環境変数の設計

既存の `seed-stg` / `seed-prod`（`STG_DATABASE_URL` / `PROD_DATABASE_URL`）と同じ流儀で
プレフィックスを付ける。`*_DATABASE_URL` は既存を再利用する。

| 変数 | dev | stg | prod |
|------|-----|-----|------|
| DB | `DEV_DATABASE_URL` → `DATABASE_URL` | `STG_DATABASE_URL` | `PROD_DATABASE_URL` |
| Supabase URL | `DEV_SUPABASE_URL`（任意） | `STG_SUPABASE_URL` | `PROD_SUPABASE_URL` |
| service_role | `DEV_SUPABASE_SERVICE_ROLE_KEY`（任意） | `STG_SUPABASE_SERVICE_ROLE_KEY` | `PROD_SUPABASE_SERVICE_ROLE_KEY` |
| bucket | `DEV_SUPABASE_STORAGE_BUCKET`（任意） | `STG_SUPABASE_STORAGE_BUCKET`（任意） | `PROD_SUPABASE_STORAGE_BUCKET`（任意） |

### dev の挙動（`--env dev`）

- Storage を使わない前提。`DEV_SUPABASE_URL` / `DEV_SUPABASE_SERVICE_ROLE_KEY` が
  未設定なら **Storage アップロードをスキップ**し、`DEV_ASSET_BASE_URL` を基点に
  `image_url` を更新する（例: `http://localhost:8080/assets/master/{entity}/{id}.png`）。
- `DEV_ASSET_BASE_URL` も未設定なら DB 更新もスキップし、その旨を表示する。
- dev に Supabase を用意したい場合は `DEV_SUPABASE_*` を設定すれば stg/prod と同様に動く。

### stg / prod の挙動

- `*_SUPABASE_URL` と `*_SUPABASE_SERVICE_ROLE_KEY` は必須（未設定ならエラー）。
- オブジェクトキー `master/{entity}/{id}.{ext}` は全環境で共通。bucket は環境別変数 →
  `manifest.bucket` の順に解決。

## 実装手順

1. `upload.py` に `--env {dev,stg,prod}`（未指定は従来のフラット変数 = legacy）を追加し、
   環境ごとの設定解決関数 `resolve_config()` を実装する。
2. `backend/Makefile` に `master-images-upload-dev/-stg/-prod` を追加（`.PHONY` 更新込み）。
   prod は `CONFIRM_PROD_UPLOAD=yes` ガード。
3. `backend/.env.example` に環境別変数とコメントを追加。
4. `docs/assets/02_Supabase_Master_Images.md` と `backend/assets/master/README.md` を更新。

### 実装を選択する理由

- **Makefile ターゲット分離**: ユーザー要望（コマンドで分ける）。`seed-stg` と同じ粒度で
  揃えることで学習コストを下げる。
- **プレフィックス変数**: `.env` は `run.sh` が `source` で上書きするため（`scripts/master-images/run.sh:28`）、
  コマンドラインでの一時上書きが効かない。1ファイルに複数環境を併記し、ターゲットで選ぶ。
- **dev は Storage なし**: 開発に Storage を要求しない。`DEV_ASSET_BASE_URL` でローカル配信に
  差し替え可能にし、未設定なら no-op にして副作用を防ぐ。
- **prod ガード**: 本番 Storage / DB を書き換えるため、`seed-prod` と同じ確認方式で誤爆を防ぐ。

## テスト計画

- [ ] `python3 -m py_compile scripts/master-images/upload.py`
- [ ] `make master-images-validate`（manifest 0 件でも OK）
- [ ] `make master-images-upload-stg DRY_RUN=1`（要 STG_SUPABASE_* / STG_DATABASE_URL）
- [ ] `make master-images-upload-prod` が `CONFIRM_PROD_UPLOAD` 未指定で中止すること
- [ ] `make master-images-upload-dev DRY_RUN=1`（DEV_ASSET_BASE_URL 未設定時にスキップ表示）

## 結果・振り返り

（完了後に記入）
