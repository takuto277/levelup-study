# アセット入稿 — クイックリファレンス

**正本**: このディレクトリ（`source/` + `manifest.yaml`）

## 入稿 3 ステップ

1. PNG を `source/` に置く（命名: `SPRITES_README.md` 参照）
2. `manifest.yaml` を更新
3. 以下を実行して PR

```bash
./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

## 詳細

- 全体フロー: [`docs/assets/01_Asset_Ingestion_Workflow.md`](../../docs/assets/01_Asset_Ingestion_Workflow.md)
- 命名・サイズ: [`../SPRITES_README.md`](../SPRITES_README.md)

## ディレクトリ

| パス | 用途 |
|------|------|
| `source/` | バトル同梱 PNG（正本） |
| `manifest.yaml` | アセットレジストリ・slug 対応 |
| `master/` | 将来: CDN 投入前のマスタ画像置き場 |

**注意**: `composeApp/.../drawable/` と `Assets.xcassets/` は sync の出力先です。直接編集しないでください。
