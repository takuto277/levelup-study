# マスタ画像（CDN 用）

バトル同梱スプライト（`../source/`）とは別系統です。

| サブディレクトリ | 用途 | DB |
|------------------|------|-----|
| `characters/` | ガチャキャラ立ち絵 | `m_characters.image_url` |
| `weapons/` | 武器アイコン | `m_weapons.image_url` |
| `dungeons/` | ダンジョンバナー | `m_dungeons.image_url` |

現行は seed の placehold.co / 空 URL を使用。CDN アップロード自動化は今後追加予定。

詳細: [`docs/assets/01_Asset_Ingestion_Workflow.md`](../../../docs/assets/01_Asset_Ingestion_Workflow.md)
