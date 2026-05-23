# タスク: Supabase マスタ画像管理

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-05-23 |
| ステータス | 完了 |

## 概要

立ち絵・武器・ダンジョンバナー等を Supabase Storage + DB `image_url` で管理し、アプリ再リリースなしで差し替え可能にする。

## 実装詳細

### backend/assets/master/

- **理由**: マスタ画像は DB/API 経由のリモート資産。モバイル同梱 `apps/mobile/assets/` と責務を分離。

### scripts/master-images/

- **upload.py**: Storage REST API + psycopg2 で `image_url` UPDATE
- **validate.py**: manifest と seed UUID の整合性
- **理由**: 運用者が CLI だけで入稿完結。service_role はサーバー側のみ。

## テスト計画

- [x] `make master-images-validate`（assets 0 件でも OK）
- [ ] Supabase 実環境で `DRY_RUN=1` → upload（要ユーザー env）
