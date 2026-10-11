## 概要

ガチャ立ち絵（Supabase 配信のマスタ画像）用に生成したキャラクター画像を、配信しやすい形（512×512・背景透過・複数フレーム命名）でリポジトリに追加する。Supabase への実投入は後続で実施するため、本 PR は画像と生成・加工まわりの整備を暫定的に main へ入れるもの。

## 背景

生成AIで作成したキャラ画像は 1254×1254 の PNG・黒背景のままだった。配信（Supabase）を想定すると容量・透過・命名を整える必要がある。また、1キャラが複数モーション（idle/prep/attack/rest/walk）を持つため、ファイル名でモードとフレームを一意に識別できる必要がある。

## 実装の意図・技術選定

- **512×512 に統一**: 生成AIは指定サイズを無視して 1254px 前後で出力するため、後処理で 512 に縮小。表示解像度（Retina 2x 想定）に対して十分で、容量も抑えられる。
- **背景透過スクリプトを拡張 (`--kind character`)**: 既存の `make_player_sprites_transparent.py` はモバイル同梱の player/enemy のみ対象だった。同じフラッドフィル方式を流用し、`backend/assets/master/source/characters/` を対象にする `character` モードを追加。アルゴリズムを二重実装しない。
- **命名 `character_{code}_{mode}_{frame}.png`**: アプリでキャラを切り替えた際に画像を機械的に特定できるよう、キャラ番号（5桁ゼロ埋め）とモード・フレームを含める。`{code}` は将来 `m_characters.code` と対応させる（スキーマ追加は別途）。
- **512統一のドキュメント反映**: `PROMPTS.md` / `SPRITES_README.md` / `docs/assets/01` のサイズ記述を 512 に揃え、生成手順に「縮小→透過」を追記。

## 変更内容

- `apps/mobile/assets/source/PROMPTS.md`: 生成プロンプトを 512×512 に統一。Post-Generation に縮小・`--kind character` を追記。
- `apps/mobile/SPRITES_README.md` / `docs/assets/01_Asset_Ingestion_Workflow.md`: 推奨サイズを 512×512 に更新。
- `scripts/assets/make_player_sprites_transparent.py`: `--kind character` を追加（`backend/assets/master/source/characters/` 対象）。
- `backend/assets/master/source/characters/character_00001_*.png`: 512×512・透過済みの12フレーム（idle×2 / prep×2 / attack×5 / rest×1 / walk×2）。

## 確認項目

- [x] フォーマット（該当ツール変更なし）
- [x] リント / 型チェック（`python3 -m py_compile`）
- [x] アセット検証（`scripts/assets/validate_assets.py`）
- [x] 画像確認（12枚すべて 512×512・四隅 alpha=0・透過率 55〜66%）

## 詳細（任意）

- Supabase への実投入と、複数フレームを配信するためのスキーマ/パイプライン（`character_sprites` テーブル等）は後続の別作業。
- `{code}` と `m_characters.id` の対応付けも後続で行う。
