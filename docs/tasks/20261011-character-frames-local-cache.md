# キャラの複数フレーム表示 & 入手時のローカル保存

作成日: 2026-10-11
ブランチ: `feat/character-frames-local-cache`

## 目的

- ガチャで手に入れたキャラ（例 00002）が **idle 以外（attack / prep / walk / rest）のフレーム**で動くようにする。
- **入手した瞬間にフレーム一式を Storage から取得し、アプリ領域へ保存**（オフライン用）。
- ホーム/戦闘/rest のプレイヤー描画を、そのキャラのフレームで再生する。

## 背景

- 現状はマスタ `image_url`（代表1枚 = `character_00002_idle_1.webp`）だけを表示していた。
- Storage には `character_{code}_{mode}_{frame}.webp` の12枚が既にある（00002 はアップ済み）。
- アプリはフレーム導出・ローカル保存の仕組みを持っていなかった。

## 方針（決定事項）

- **フレームURLは `image_url` から導出**（バックエンド変更なし）。`character_00002_idle_1.webp` → ベース `character_00002` → `_{mode}_{frame}.webp` を組み立て。
- **入手時にアプリ領域へ明示保存**（KMP の expect/actual ＋ okio）。
- 描画は **ローカル保存ファイル優先、無ければリモートURL**（Coil/SwiftUI `AsyncImage`）。

## 実装詳細

### Shared（KMP）
- `core/storage/AssetRootDir.kt`（expect）＋ Android=`files/asset_cache` / iOS=Application Support。
- `core/storage/AssetStore.kt`: okio で `exists/localPath/save`。
- `features/character/CharacterFrames.kt`: `baseFromImageUrl` / `frameUrls(imageUrl, mode)` / `all` / `storageKey`。mode 構成は idle×2 / prep×2 / attack×5 / rest×1 / walk×2。
- `features/character/CharacterFrameDownloader.kt`: Ktor で各フレームを GET → `AssetStore.save`（失敗は無視、取得済みはスキップ）。
- DI: `CharacterFrameDownloader` を登録。`GachaViewModel.pullGacha` で当選キャラのフレームDLを起動。

### Android
- `components/BattleSprites.kt`: `PlayerSprite(..., url)` を、mode に応じ `CharacterFrames.frameUrls` のフレームでループ再生（`PlayerFrameImage`）。モデルはローカルFile優先／無ければURL。導出不可なら単体画像→同梱スプライトへフォールバック。

### iOS
- `StudyQuestScreenView.swift`: `PlayerFrameImage` を追加。`PlayerSpriteView`/`AnimatedSpriteView` は `partyLeadImageUrl` からフレームを導出して再生。ローカルファイル優先／無ければURL。

## 依存追加

- `com.squareup.okio:okio`(3.9.1) を `shared/commonMain` に追加。

## トレードオフ

- プレイヤーはモード単位のループ再生（attack は one-shot ではなくループ）。戦闘の凝った演出は今後調整。

## 完了条件

- [ ] ガチャで 00002 を引くと、戦闘/rest でフレームが動く
- [ ] 入手時にアプリ領域へ12枚が保存される（2回目以降はオフラインでも表示）
- [ ] image_url が無いキャラは従来スプライトにフォールバック

## 影響範囲

- apps/mobile/shared（新規3＋2）, `di/SharedModule.kt`, `features/gacha/GachaViewModel.kt`
- apps/mobile/composeApp: `components/BattleSprites.kt`
- apps/mobile/iosApp: `StudyQuestScreenView.swift`
- `apps/mobile/gradle/libs.versions.toml`, `shared/build.gradle.kts`
