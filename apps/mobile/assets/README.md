# アセット入稿 — クイックリファレンス

**正本**: このディレクトリ（`source/` + `manifest.yaml`）

## 管理方針

- **管理元は 1 箇所**: 画像アセットの正本は `source/` + `manifest.yaml` のみ
- **OS 別 asset は生成物**: `composeApp/.../res/drawable*/` と `Assets.xcassets/` は sync スクリプトの出力先。直接編集しない
- **例外**: AppIcon、AccentColor、launcher icon など OS 固有アセットは `manifest.yaml` の `os_specific_assets` に定義

## 入稿 4 ステップ

1. PNG を `source/` に置く（命名: `SPRITES_README.md` 参照）
2. `manifest.yaml` を更新（`battle_assets` または `enemy_sprite_keys` にエントリ追加）
3. 生成・同期を実行
4. 検証して PR

```bash
./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

## クリーンアップ

```bash
# source/manifest にない OS 側 PNG を一括削除（例外除く）
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py --clean
```

## 詳細

- 全体フロー: [`docs/assets/01_Asset_Ingestion_Workflow.md`](../../docs/assets/01_Asset_Ingestion_Workflow.md)
- 命名・サイズ: [`../SPRITES_README.md`](../SPRITES_README.md)

## ディレクトリ

| パス | 用途 |
|------|------|
| `source/` | バトル同梱 PNG（正本） |
| `manifest.yaml` | アセットレジストリ・slug 対応・例外定義 |
| `master/` | 将来: CDN 投入前のマスタ画像置き場 |

## 例外（OS 固有アセット）

以下のアセットは `manifest.yaml` の `os_specific_assets` に定義され、source/manifest 管理外:

| プラットフォーム | アセット | 理由 |
|-----------------|---------|------|
| Android | `ic_launcher*` (PNG/XML) | アプリアイコン |
| iOS | `AppIcon.appiconset` | アプリアイコン |
| iOS | `AccentColor.colorset` | アクセントカラー |

**注意**: `composeApp/.../res/drawable*/` と `Assets.xcassets/` は sync の出力先です。直接編集しないでください。
