## Issues

- Close #53

## 背景

#54 で manifest の単一管理元化（orphan 検出・例外定義・bundledKeys 同期）を実施したが、OS 側にしか存在しなかった 55 の敵スプライト PNG が `source/` に移行されていなかった。#53 の完了には物理的な画像移行も必要。

## 概要

Android `drawable-nodpi/` にのみ存在していた 55 の敵スプライト PNG を `apps/mobile/assets/source/` にコピーし、sync スクリプトで iOS `Assets.xcassets` にも反映。source/ が全 73 PNG の単一正本になった。

## 変更内容

- **`apps/mobile/assets/source/`**: 55 の敵スプライト PNG を追加（18 → 73）
- **`iosApp/Assets.xcassets/`**: 55 の `Contents.json` を sync により更新（imageset 構造の標準化）

## 確認結果

```bash
# source/ の PNG 数
$ ls apps/mobile/assets/source/*.png | wc -l
73

# 全チェック通過
$ ./scripts/assets/run.sh scripts/assets/sync_battle_assets.py --check
Done. 73 file(s) processed.

$ ./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py --check
EnemySpriteAssets.kt is up to date.

$ ./scripts/assets/run.sh scripts/assets/validate_assets.py
Asset validation passed.
```

## 確認項目

- [x] フォーマット
- [x] リント / 型チェック
- [x] テスト（assets validate/sync/generate 全パス）
- [x] orphan 画像ゼロ（全 OS 側 PNG が source/manifest 管理下）
