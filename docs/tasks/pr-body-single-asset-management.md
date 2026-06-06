## Issues

- Close #53

## Why

画像アセットの管理元が `source/` / `manifest.yaml` と OS 別の `drawable/` / `Assets.xcassets/` に分散しており、片方の OS だけに画像を追加・更新してしまう余地があった。管理元を 1 箇所に集約し、OS 側の orphan 画像を CI で検出可能にする。

## Summary

`manifest.yaml` をアセットの唯一の正本にし、`validate_assets.py` に OS 側 orphan 画像の検出機能を追加。`enemy_sprite_keys` を `EnemySpriteAssets.kt` の `bundledKeys` と同期し、`generate_enemy_sprite_assets.py` で `bundledKeys` も生成対象に含めた。`sync_battle_assets.py` に drawable-nodpi 対応と `--clean` オプションを追加。

## Changes

- **manifest.yaml**: `enemy_sprite_keys` を 25→60 に拡張（`EnemySpriteAssets.kt` の `bundledKeys` と同期）。`os_specific_assets` セクションを追加し AppIcon / AccentColor / launcher icon を例外定義
- **validate_assets.py**: OS 側に存在するが source/manifest に未登録の orphan PNG 検出を追加。`bundledKeys` と `enemy_sprite_keys` の整合性チェックを追加
- **generate_enemy_sprite_assets.py**: `bundledKeys` も manifest から生成するよう拡張
- **sync_battle_assets.py**: 敵スプライトを `drawable-nodpi/` に出力するよう変更。`--clean` オプション追加（orphan PNG の一括削除）
- **lib/manifest.py**: 定数とヘルパー関数を追加（`ANDROID_DRAWABLE_NODPI`, `collect_managed_filenames`, `enumerate_android_pngs`, など）
- **EnemySpriteAssets.kt**: `bundledKeys` を manifest から再生成（アルファベット順にソート）
- **README.md**: 管理方針・入稿 4 ステップ・例外定義の表を追記

## Verification

```
$ ./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py --check
EnemySpriteAssets.kt is up to date.

$ ./scripts/assets/run.sh scripts/assets/sync_battle_assets.py --check
Done. 18 file(s) processed.

$ ./scripts/assets/run.sh scripts/assets/validate_assets.py
Asset validation passed.
```
