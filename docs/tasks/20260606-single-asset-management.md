# 実行計画書: モバイル画像アセットの単一管理元化

**Issue**: [#53](https://github.com/takuto277/levelup-study/issues/53)  
**Branch**: `feat/53-single-asset-management`  
**Date**: 2026-06-06

## 概要

画像アセットの正本を `apps/mobile/assets/source/` + `manifest.yaml` に集約し、Android / iOS の asset は sync の生成物として扱う。不要な OS 別画像の検出・例外の明示化・CI 検証の強化を行う。

## 現状分析

現在のパイプライン:
- `source/` (18 PNG) → `sync_battle_assets.py` → Android `drawable/` + iOS `*.imageset/`
- `manifest.yaml` が asset レジストリ
- `validate_assets.py` が source↔sync↔Kotlin の整合性を検証
- Android `drawable-nodpi/` に 60 の敵スプライト PNG（`generate_enemy_sprite_assets.py` で生成と推測）
- CI で generate --check, sync --check, validate を実行

不足している点:
1. OS 側に source/manifest 未定義の PNG が存在しても検出されない（orphan 画像）
2. AppIcon / AccentColor 等の例外が明文化されていない
3. manifest の `enemy_sprite_keys` (25) と `EnemySpriteAssets.kt` の `bundledKeys` (60) が一致していない
4. 非 PNG ファイル（XML, json）が例外定義なしに混在

## 実装タスク

### 1. manifest.yaml 拡張
- `os_specific_assets` セクション追加: AppIcon, AccentColor, ic_launcher* を明示
- `enemy_sprite_keys` を `EnemySpriteAssets.kt` の `bundledKeys` に合わせて拡張

### 2. validate_assets.py 拡張
- orphan 画像検出: OS 側に存在するが source/manifest に登録されていない PNG を検出（例外除く）
- `bundledKeys` と `enemy_sprite_keys` の整合性チェック
- `monster_slug_to_sprite_key` から `enemy_sprite_keys` への参照整合性（既存）

### 3. sync_battle_assets.py 拡張
- `--clean` オプション: source/manifest にない OS 側 PNG を削除（例外除く）
- drawable-nodpi も管理対象に含める

### 4. lib/manifest.py 拡張
- `ANDROID_DRAWABLE_NODPI` 定数追加
- manifest から管理対象ファイル名一覧を取得するヘルパー追加（battle_assets + enemy_sprite_keys）

### 5. CI
- validate_assets.py の拡張のみで対応（新增検出は既存の validate ステップで実行される）

### 6. ドキュメント更新
- README.md: 例外定義を追記
- SPRITES_README.md: 必要に応じて更新

## 受け入れ条件

- [x] 共通管理対象の画像は `apps/mobile/assets/source/` + `manifest.yaml` が正本
- [x] Android / iOS の画像 asset は sync script の生成物として再作成可能
- [x] 片方の OS にだけ画像を追加すると validate が失敗する
- [x] 1 つの source PNG 更新で両 OS に反映される
- [x] 例外が manifest.yaml に明記されている
- [x] 入稿手順に「管理元は 1 箇所」「OS 別 asset は直接編集しない」が明記済み
