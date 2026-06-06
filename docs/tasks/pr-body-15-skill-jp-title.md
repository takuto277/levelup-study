## Issues

- Close #15

## 背景

#53 (#54) の実装により、バトルアセットの sync・manifest 整合・CI 検証が完了した。#15 の目的（sync 実行、manifest と source の整合、CI 通過、bundledKeys 一致）は達成済み。

## 概要

#15 の全チェック項目を最終確認し、全項目パスを確認。合わせて PR 作成スキルに日本語タイトルのルールを追加。

## 変更内容

- **`.cursor/skills/github-pr-create/SKILL.md`**: PR 本文テンプレートの見出しを日本語化（Why→背景、Summary→概要、Changes→変更内容、Checklist→確認項目、Details→詳細）。LevelUp Study 向けに「PR タイトルは日本語で」ルールを追加
- **`AGENTS.md`**: GitHub CLI セクションに PR タイトル日本語ルールを追加

## 確認結果（#15 の受け入れ条件）

```bash
# 全 assets チェック通過確認
$ ./scripts/assets/run.sh scripts/assets/validate_assets.py
Asset validation passed.

$ ./scripts/assets/run.sh scripts/assets/sync_battle_assets.py --check
Done. 18 file(s) processed.

$ ./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py --check
EnemySpriteAssets.kt is up to date.

# アセット統計
Android drawable:       18 PNG (source 全同期済み)
Android drawable-nodpi: 60 PNG (全 enemy カバレッジ)
iOS imagesets:          73 (battle + enemy)
source PNG:             18 (正本)
manifest enemy_keys:    60 (EnemySpriteAssets.kt bundledKeys と一致)
```

## 確認項目

- [x] フォーマット
- [x] リント / 型チェック
- [x] テスト（assets validate/sync/generate 全パス）
