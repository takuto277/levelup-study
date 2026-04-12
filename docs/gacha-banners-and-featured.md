# ガチャバナー・排出テーブル・ピックアップ設計

LevelUp Study における **召喚バナー**（`m_gacha_banners`）と **キャラ／武器マスタ**の紐付け、および **ピックアップ**（`m_gacha_banner_featured`）の役割とサーバー実装をまとめる。  
マスタの正は常に `m_characters` / `m_weapons` であり、バナーは **期間・排出確率・天井** を束ねる「開催単位」である。

---

## 1. テーブルと責務

### 1.1 `m_gacha_banners`

| 役割 | 説明 |
|------|------|
| 開催枠 | `start_at` / `end_at` / `is_active` で一覧 API に出すか決まる。 |
| バナー種別 | `banner_type`: `character` / `weapon` / `costume` / `mixed`（クライアント表示・運用用）。 |
| 天井 | `pity_threshold` が非 null のとき、その回数で「最高レア枠」の確定抽選（実装: `GachaService.rollGacha`）。 |
| 排出プール | `rate_table`（JSONB）に **実際に抽選される行** を並べる。 |

`rate_table` の1行の推奨スキーマ（JSON）:

```json
{
  "item_id": "UUID",
  "result_type": "character | weapon | costume",
  "rarity": 5,
  "rate": 0.015
}
```

- `rate` は **相対ウェイト**（合計が 1.0 でなくてもよい。Pull 直前に正規化される）。
- `item_id` は **マスタの主キー**（`m_characters.id` または `m_weapons.id`）。

### 1.2 `m_gacha_banner_featured`

| カラム | 説明 |
|--------|------|
| `banner_id` | 対象バナー（FK）。 |
| `item_id` | ピックアップ対象のマスタ ID。 |
| `item_type` | `character` / `weapon` / `costume`（`rate_table.result_type` と一致させる）。 |
| `rate_up` | **レートアップ係数**。Pull 時に `rate_table` の該当行の `rate` に **`(1 + rate_up)` を乗算**する。 |

**キャラを随時追加する運用**では:

1. まず `m_characters` に行を追加する。  
2. そのキャラを **プールに入れる**なら `rate_table` の JSON を更新（行追加または `rate` 変更）。  
3. **ピックアップに載せるだけ**なら `m_gacha_banner_featured` に行を追加（`rate_table` に既に存在する `item_id` と突き合わせて係数が効く）。

`rate_table` に存在しない `item_id` を `featured` にだけ載せた場合、**抽選には影響しない**（表示専用）。運用上は避けるか、あとで `rate_table` に追加する。

---

## 2. Pull 時のアルゴリズム（サーバー）

実装: `backend/internal/service/gacha_service.go` + `gacha_rate_table.go`。

1. **バナー取得** … 開催期間・`is_active` を検証。  
2. **`rate_table` パース** … `[]RateTableEntry` に変換。  
3. **マスタ検証** … 各行について `result_type` に応じ `m_characters` / `m_weapons` を参照し、存在かつ `is_active` であることを確認（`costume` はマスタ未実装の間スキップ）。  
4. **ピックアップ読込** … `ListFeaturedByBannerID(banner_id)`。  
5. **ウェイト合成** … `mergeFeaturedIntoRateTable`  
   - `rate_table` の各行について、`item_id` と `item_type`（大小無視）が一致する `featured` 行すべてに対し、  
     `weight *= (1 + rate_up)` を **順に** 適用。  
6. **正規化** … 全 `rate` の合計で割り、**合計 1.0** になるようにする（累積抽選の安定化）。  
7. **抽選** … 正規化後テーブルで `rollGacha`（天井時は最高 `rarity` 行を確定）。

### 2.1 数値例

- 某キャラの元 `rate = 0.01`  
- `rate_up = 0.5` のピックアップ1行がマッチ  
→ 合成ウェイト `0.01 × (1 + 0.5) = 0.015`  
→ その後、プール全体で正規化。

---

## 3. バナー一覧 API

`GET /api/v1/master/gacha/banners`

- 各要素は **従来のバナーフィールド**に加え、`featured` 配列を含む。  
- `featured` の各要素には、サーバーがマスタを解決した **`item_name` / `rarity` / `image_url`**（キャラ・武器時）を付与する。  
- クライアント（KMP）は `featured` をドメイン `GachaBanner.featured` にマッピングし、あわせて **`featuredSummary`**（`item_name` を ` · ` で連結）を生成して iOS 等の短文表示に使う。

---

## 4. 運用チェックリスト

- [ ] 新キャラを `m_characters` に追加したら `is_active` を確認。  
- [ ] ガチャに入れるなら `rate_table` に1行追加（`result_type: character`）。  
- [ ] ピックアップ表示とレートアップをかけるなら `m_gacha_banner_featured` に追加。  
- [ ] `rate` の合計が極端に小さい／大きい場合でも **正規化で Pull は動く**が、運用では合計≈1を目安にすると検証しやすい。  
- [ ] 本番では `Pull` 前の **マスタ検証エラー**に気づけるよう、ステージングで必ず1回引く。

---

## 5. 関連コード一覧

| 領域 | パス |
|------|------|
| モデル | `backend/internal/model/models.go` — `MasterGachaBanner`, `MasterGachaBannerFeatured` |
| ピックアップ取得 | `backend/internal/repository/gacha_master_repository.go` — `ListFeaturedByBannerID(s)` |
| 一覧レスポンス組み立て | `backend/internal/handler/master_handler.go` — `ListActiveBanners`, `enrichGachaFeaturedJSON` |
| 合成・正規化 | `backend/internal/service/gacha_rate_table.go` |
| Pull | `backend/internal/service/gacha_service.go` — `validateRateTable`, `mergeFeaturedIntoRateTable` |
| シード例 | `backend/db/seed.sql` — `m_gacha_banner_featured` の INSERT |
| クライアント DTO | `apps/mobile/shared/.../dto/GachaDto.kt` |
| ドメイン | `apps/mobile/shared/.../domain/model/Gacha.kt` |

---

## 6. 将来拡張

- **衣装ガチャ**: `m_costumes` 追加後、`validateRateTable` の `costume` 分岐と `enrichGachaFeaturedJSON` を埋める。  
- **プールの正規化テーブル化**: `m_gacha_banner_pool` 等に切り出し、FK 制約で `item_id` の整合を DB 側で担保。  
- **管理画面**: バナー編集で `rate_table` JSON をエディタ or 行 UI で編集し、`featured` は別フォームで紐付け。
