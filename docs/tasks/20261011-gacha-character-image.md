# ガチャ結果にキャラクター画像を表示する

作成日: 2026-10-11
ブランチ: `feat/gacha-character-image`

## 目的

ガチャを引いた結果に **キャラクターの画像**（と名前）を表示する。まずは character 00002（`闇の魔王ゼファー`）が引かれたときに、名前＋立ち絵が出る状態にする。

## 背景

- ガチャ `POST /api/v1/users/{userID}/gacha/pull` は既にキャラを抽選して `{ item_id, name, rarity, is_new, pity_count }` を返すが、**画像URLを返していない**。
- モバイルの結果カードは汎用アイコン＋名前＋星のみ。ただし `GachaResultItem` には**未使用の `imageUrl` フィールドが既にある**。
- `m_characters` には `image_url` がある（00002 = `a0000000-...-0002`, 現在 placehold.co）。

## 方針（決定事項）

- **API は画像URL（メタ）を返す**。画像の**実体は Storage から直DL**（Render は経由しない）。base64埋め込みやAPIでのバイト配信はしない（帯域・キャッシュ効率の観点）。
- 今回の対象は「ガチャ結果の表示」。複数フレーム配信（`character_{code}_{mode}_{frame}`）は将来対応とし、今回は代表1枚（`image_url`）を表示する。

## 実装詳細（変更内容と理由）

### Backend (`backend/internal/service/gacha_service.go`)
- `GachaPullResult` に `ImageURL string \`json:"image_url"\`` を追加。
- 抽選ループで `name`/`rarity` と同様にマスタの `ImageURL` を取得してセットする。3分岐すべて（character=`mc.ImageURL`, costume=`mc.ImageURL`, weapon=`mw.ImageURL`）に対応（モデルは全て `ImageURL` を持つ）。
- **理由**: 既存の name/rarity 解決と同じ経路で完結し、追加クエリなし。APIはメタのみ返す方針に一致。

### Mobile (KMP shared)
- `domain/model/Gacha.kt`: `GachaResult` に `imageUrl: String = ""` を追加。
- `data/remote/dto/GachaDto.kt`: `GachaPullResultResponse` に `@SerialName("image_url") imageUrl` を追加し、`toDomain()` でマップ。
- `features/gacha/GachaViewModel.kt`: `GachaResultItem(imageUrl = result.imageUrl, ...)` をセット（フィールドは既存・未使用を有効化）。
- **理由**: 既存の未使用フィールドを活かすためUI側の変更が軽い。

### UI
- Android `GachaScreenView.kt`: `SingleResultCard` / `MultiResultCard` で `imageUrl` があれば `AsyncImage`（Coil導入済）、無ければ従来アイコンにフォールバック。
- iOS `GachaScreenView.swift`: `ResultItem` に `imageUrl` を追加、`convertResult` でマップ、カードで `AsyncImage`（無ければアイコン）。
- **理由**: 画像があっても壊れないようフォールバックを維持。

## 手順

```bash
# backend
cd backend && go build ./... && go test ./...
# mobile
cd apps/mobile && ./gradlew :shared:compileKotlinMetadata   # 可能なら
```

## 完了条件

- [ ] `POST /gacha/pull` のレスポンスに `image_url` が含まれる
- [ ] character 00002 を引くと名前＋画像が表示される（画像が無ければアイコンfallback）
- [ ] 単発・10連の両カードで表示

## テスト用データ（00002）

ガチャで 00002 を必ず表示し画像取得を確認するため、以下を反映（テスト用・検証後に戻す）:

- **Storage**: `master/characters/character_00002_*.webp`（blue mage 12枚）をアップ
- **m_characters `...0002`**: `闇の魔王ゼファー` → `青魔導士` に変更し、`image_url` を `character_00002_idle_1.webp` の公開URLへ
- **m_gacha_banners `f...0001`**: 排出テーブルを `...0002` のみ（rate 1.0）にして常に 00002 を排出
- 変更は `backend/db/seed.sql` と **stg DB** の両方に反映

## 影響範囲

- backend: `internal/service/gacha_service.go`
- mobile: `GachaDto.kt`, `Gacha.kt`, `GachaViewModel.kt`, `GachaScreenView.kt`(Android), `GachaScreenView.swift`(iOS)

## 後続（未対応）

- character 00002 以降の実画像を Storage に用意し、`m_characters.image_url` を実URLへ更新。
- `m_characters.code` と複数フレーム（`character_{code}_{mode}_{frame}.webp`）配信・アプリ領域キャッシュ。
