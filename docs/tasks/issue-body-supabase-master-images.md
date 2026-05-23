# Issue: Supabase Storage でマスタ画像を管理（アプリ再リリース不要）

## 背景

バトル同梱スプライト（`apps/mobile/assets/source/`）は APK/IPA 更新が必要。一方、立ち絵・武器・ダンジョンバナー等の **マスタ画像** は既に DB の `image_url` カラムと API 経由でクライアントが参照しているが、Supabase Storage への入稿フローが未整備で placehold.co 依存のまま。

## 目的

画像追加・差し替えを **Supabase Storage + PostgreSQL `image_url` 更新** だけで完結させ、アプリストア再リリースなしで反映できる運用を確立する。

## スコープ

- [ ] `backend/assets/master/` に manifest + source 置き場
- [ ] `scripts/master-images/`（upload / validate）
- [ ] `docs/assets/02_Supabase_Master_Images.md`
- [ ] `backend/.env.example` に Supabase Storage 用 env
- [ ] Makefile ターゲット
- [ ] CI validate

## スコープ外

- モバイルアプリコード変更（既存 `AsyncImage` / Coil が `image_url` を読む）
- バトル同梱スプライトパイプライン（別 PR #3）

## 受け入れ条件

1. manifest + validate が通る
2. `--dry-run` でアップロード先 URL と UPDATE SQL が確認できる
3. 入稿手順がドキュメント化されている

## 参考

- `docs/architecture/02_Database_Schema.md` §画像の保管方針
- `docs/assets/01_Asset_Ingestion_Workflow.md` §2
