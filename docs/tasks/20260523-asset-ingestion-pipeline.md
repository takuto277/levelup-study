# タスク: アセット入稿パイプライン確立

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-05-23 |
| ステータス | 完了 |
| Issue | （GitHub Issue 番号を PR で Fixes #n に紐付け） |

## 概要

敵・キャラ・ダンジョン背景の画像を `apps/mobile/assets/` を正本として入稿し、Android / iOS へ sync、CI で validate するフローを確立する。

## 要件

- [x] `manifest.yaml` + `source/` を正本として新設
- [x] sync / generate / validate スクリプト
- [x] `assets-ci.yml`
- [x] `docs/assets/01_Asset_Ingestion_Workflow.md`
- [x] `feature-start.sh` + Cursor Skill / Rule

## 実装詳細

### 1. 正本ディレクトリ `apps/mobile/assets/`

- **変更内容**: `source/` に PNG、`manifest.yaml` に battle_assets / enemy_sprite_keys / monster_slug_to_sprite_key / master_images を定義。
- **理由**: Android drawable と iOS imageset の二重管理を解消し、slug 対応を manifest で一元化するため。

### 2. `scripts/assets/*.py`

- **sync_battle_assets.py**: source → drawable + imageset（Contents.json 自動生成）
- **generate_enemy_sprite_assets.py**: manifest から `EnemySpriteAssets.fallbackByLogical` を生成
- **validate_assets.py**: 必須ファイル・プラットフォーム同期・seed slug 対応を検証
- **理由**: 人手コピーと Kotlin 手書きの不整合を防ぎ、PR 前に機械的に検証するため。

### 3. CI + ドキュメント + AI ガイド

- **assets-ci.yml**: PR 時 validate
- **feature-delivery Skill**: Issue → ブランチ → 実装 → PR の定型
- **assets Rule**: アセット編集時に ingestion フローを参照

## テスト計画

- [x] `./scripts/assets/run.sh scripts/assets/validate_assets.py`
- [x] CI workflow 定義

## 結果・振り返り

- 既存 PNG を source に集約し sync 済み
- マスタ画像 CDN アップロードは将来タスク
