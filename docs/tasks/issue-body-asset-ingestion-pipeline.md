# Issue: ゲームアセット入稿パイプラインの確立

## 背景

敵・キャラ・ダンジョン背景などの画像アセットが、Android drawable / iOS Assets.xcassets / DB `image_url` に分散しており、入稿フロー・命名の正本・プラットフォーム間同期・CI 検証が未整備です。現状は PNG 実体がほぼ未投入で、絵文字フォールバックが主です。

## 目的

アーティスト／開発者が **1 か所に PNG を置き、manifest 更新 → sync → validate → PR** するだけで、Android / iOS 両方に反映されるパイプラインを確立する。

## スコープ（本イシュー）

- [ ] `apps/mobile/assets/` を単一ソース・オブ・トゥルースとして新設（`manifest.yaml` + `source/`）
- [ ] `scripts/assets/sync_battle_assets.py` — source → Android drawable + iOS imageset 同期
- [ ] `scripts/assets/validate_assets.py` — manifest・実ファイル・slug 対応の検証
- [ ] CI（`assets-ci.yml`）で PR 時に validate を実行
- [ ] `docs/assets/01_Asset_Ingestion_Workflow.md` — 入稿ガイド（マスタ画像 CDN 方針含む）
- [ ] `scripts/feature-start.sh` — イシュー起票 + ブランチ作成の定型スクリプト
- [ ] Cursor Skill（機能開発フロー）+ Rule（アセット編集時）を追加

## スコープ外（将来）

- R2 / Supabase へのマスタ画像アップロード自動化
- `StudyQuestViewModel` の敵カタログを API 化
- スクリーンショット差分 CI

## 受け入れ条件

1. 既存 5 枚の PNG が `assets/source/` にあり、`make assets-sync`（または `./scripts/assets/sync_battle_assets.py`）で Android / iOS に同期できる
2. `./scripts/assets/validate_assets.py` が exit 0
3. 入稿手順が `docs/assets/` に文書化されている
4. AI が毎回指示なしで従える Skill / Rule が `.cursor/` にある

## 参考

- `apps/mobile/SPRITES_README.md`
- `docs/tasks/20260410-battle-animation-system.md`
- `docs/architecture/02_Database_Schema.md`（CDN 方針）
