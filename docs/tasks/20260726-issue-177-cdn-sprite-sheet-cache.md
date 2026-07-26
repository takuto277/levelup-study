# Issue #177: CDN sprite sheet download + local cache per character 設計

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-07-26 |
| ステータス | 設計中 |
| Issue | https://github.com/takuto277/levelup-study/issues/177 |

## 1. 目的

キャラクターごとのスプライトシートを Supabase Storage CDN から取得し、端末内にキャッシュする。アプリ本体にはデフォルト表示用の最小限のスプライトだけを残し、追加キャラクターのアニメーション画像は CDN 配信に寄せる。

初回表示時にキャッシュがなければ非同期でダウンロードし、完了するまでは既存のバンドル済みデフォルトスプライトを表示する。これにより、キャラクター追加のたびにアプリを更新する運用を避ける。

## 2. 背景と現状

- Issue #176 で 12 フレーム構成のプレイヤースプライトが導入済み。
- Android には `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/components/SpriteSheet.kt` があり、6列 x 2行、96px セルのスプライトシート切り出し実装がある。
- 現在の `BattleSprites.kt` / `HomeTabContent.kt` / iOS `StudyQuestScreenView.swift` は、主にバンドル内の個別 PNG / imageset 名を参照している。
- `MasterCharacter` には `imageUrl` と `idleAnimationUrl` があるが、戦闘・ホーム共通で使う 12 フレームの `spriteSheetUrl` はまだない。
- KMP shared には Ktor `HttpClient` と `expect/actual` の保存実装例があるため、ダウンロード制御とキャッシュ管理は shared に寄せられる。

## 3. スコープ

### 対象

- DB `m_characters.sprite_sheet_url` の追加
- Backend model / OpenAPI / seed への `sprite_sheet_url` 追加
- Mobile shared の `MasterCharacter.spriteSheetUrl` 追加
- Mobile shared の CDN ダウンロード + ローカルキャッシュ制御
- Android / iOS のローカルファイル保存 actual
- Android / iOS のスプライトシート描画対応
- ダウンロード中、失敗時、URL 未設定時のフォールバック表示

### 対象外

- Supabase Storage への実ファイルアップロード自動化
- 管理画面からのキャラクター画像入稿 UI
- キャッシュ容量の高度な LRU 管理
- 既存の敵スプライト配信方式の変更
- コスチューム別スプライト切り替え

## 4. 推奨アーキテクチャ

今回の境界は次のように分ける。

| レイヤー | 配置 | 責務 |
|----------|------|------|
| DB / Backend | `backend/` | キャラクターマスタに CDN URL を保持し、API で返す |
| DTO / Domain | `shared/commonMain` | `spriteSheetUrl` をモデルとして保持する |
| Download Service | `shared/commonMain` | URL 検証、Ktor ダウンロード、cache hit/miss 判定 |
| Cache Store | `shared/commonMain` + `expect/actual` | platform のファイル領域へ保存・読込・削除 |
| Renderer | Android / iOS UI | ファイルパスから Bitmap / UIImage を作り、フレームを切り出して描画 |

ポイントは、shared は画像を「ファイルとして取得できる状態」まで責任を持ち、Bitmap / UIImage の生成や Compose / SwiftUI 表示は platform UI に閉じること。

## 5. データモデル設計

### 5.1 DB

`m_characters` に nullable な `sprite_sheet_url` を追加する。

```sql
ALTER TABLE m_characters
  ADD COLUMN sprite_sheet_url text;
```

既存データ互換のため `NOT NULL` にはしない。URL 未設定のキャラクターは既存のバンドル済みデフォルトスプライトを使う。

### 5.2 Backend model

`backend/internal/model/models.go` の `MasterCharacter` に追加する。

```go
SpriteSheetURL *string `gorm:"type:text" json:"sprite_sheet_url"`
```

`image_url` は立ち絵、`idle_animation_url` はホーム用の旧アニメーション URL として残し、`sprite_sheet_url` は戦闘・ホーム共通の 12 フレームシートとして扱う。

### 5.3 OpenAPI

`MasterCharacter` schema に nullable property を追加する。

```yaml
sprite_sheet_url:
  type: string
  format: uri
  nullable: true
```

`required` には入れない。

### 5.4 Mobile domain / DTO

`MasterCharacter` と `MasterCharacterResponse` に追加する。

```kotlin
@SerialName("sprite_sheet_url") val spriteSheetUrl: String? = null
```

`Json.ignoreUnknownKeys = true` は既に有効だが、モバイル側がこの値を使うには domain model まで通す。

## 6. キャッシュ設計

Issue 本文では `{characterId}.png` 保存案になっているが、実装では URL 変更に強い形にする。

### 6.1 保存先

- Android: `context.filesDir/sprite_sheets/`
- iOS: `NSCachesDirectory/sprite_sheets/`

iOS は OS に削除されてもよい再取得可能データなので `NSCachesDirectory` が適切。Android は Issue 案どおり `filesDir` でよいが、将来容量管理を入れるなら `cacheDir` も選択肢になる。

### 6.2 ファイル名

`{characterId}-{urlHash}.png` を推奨する。

理由:

- CDN URL が差し替わった時、古い `{characterId}.png` を誤表示しない。
- 同じ characterId でも staging / prod や CDN version が変わった時に自然に別キャッシュになる。
- metadata と照合しやすい。

`urlHash` は SHA-256 の先頭 16 文字程度で十分。

### 6.3 metadata

各 characterId ごとに metadata を保存する。

```kotlin
@Serializable
data class SpriteSheetCacheMetadata(
    val characterId: String,
    val sourceUrl: String,
    val urlHash: String,
    val fileName: String,
    val byteSize: Long,
    val cachedAtEpochMillis: Long,
    val schemaVersion: Int = 1,
)
```

cache hit 条件は「metadata の `sourceUrl` が現在の `spriteSheetUrl` と一致し、対応ファイルが存在する」こと。URL が違えば miss として再ダウンロードする。

### 6.4 expect/actual API

Issue 本文の `ByteArray` API は最小案としては成立するが、UI 連携を考えると file path を返すほうが扱いやすい。

```kotlin
data class CachedSpriteSheet(
    val characterId: String,
    val sourceUrl: String,
    val filePath: String,
    val byteSize: Long,
)

expect class SpriteSheetCache() {
    suspend fun get(characterId: String, sourceUrl: String): CachedSpriteSheet?
    suspend fun save(characterId: String, sourceUrl: String, bytes: ByteArray): CachedSpriteSheet
    suspend fun remove(characterId: String)
    suspend fun clear()
}
```

保存は一時ファイルへ書き込み、成功後に rename する。途中失敗で壊れた PNG を cache hit しないため。

## 7. ダウンロード設計

`shared/commonMain` に `SpriteSheetRepository` または `SpriteSheetLoader` を追加する。

```kotlin
sealed interface SpriteSheetLoadResult {
    data class Hit(val sheet: CachedSpriteSheet) : SpriteSheetLoadResult
    data class Downloaded(val sheet: CachedSpriteSheet) : SpriteSheetLoadResult
    data object NoUrl : SpriteSheetLoadResult
    data class Failed(val message: String) : SpriteSheetLoadResult
}
```

処理順:

1. `spriteSheetUrl` が null / blank なら `NoUrl`
2. URL が `https` でなければ `Failed`
3. `SpriteSheetCache.get(characterId, url)` があれば `Hit`
4. Ktor `HttpClient.get(url)` で `ByteArray` を取得
5. `Content-Type` とサイズを検証
6. `SpriteSheetCache.save(characterId, url, bytes)`
7. `Downloaded`

### 7.1 サイズ制限

初期値として最大 2MB 程度を設ける。96px x 12 フレームの PNG シートなら十分余裕がある。`Content-Length` が上限超過なら body を読まず失敗にする。`Content-Length` がない場合は読み込み後の `bytes.size` で弾く。

### 7.2 concurrent download

同じ characterId + URL の同時表示で重複ダウンロードしないよう、commonMain に `Mutex` ベースの single-flight を入れる。

```kotlin
private val locks = mutableMapOf<String, Mutex>()
```

キーは `"$characterId:$urlHash"`。

### 7.3 セキュリティ

- `https` のみ許可する。
- 可能なら Supabase Storage の CDN host を allowlist する。
- リダイレクト先も `https` を維持する。
- ログに URL query や署名付き token を出さない。
- 失敗時は UI に詳細 URL を出さず、フォールバック表示だけにする。

## 8. UI 設計

### 8.1 共通方針

キャラクター表示コンポーネントには `UserCharacter?` または `MasterCharacter?` を渡し、`character.spriteSheetUrl` を見て CDN キャッシュを試す。取得中・失敗・未設定の場合は既存のバンドル済みデフォルトスプライトを使う。

表示状態:

```kotlin
sealed interface CharacterSpriteSource {
    data class CachedSheet(val filePath: String) : CharacterSpriteSource
    data object BundledDefault : CharacterSpriteSource
}
```

### 8.2 Android

`SpriteSheet.kt` を以下の方向で拡張する。

- `rememberSheet()` は既存のバンドル読み込みとして残す。
- `rememberSheetFromFile(filePath: String)` を追加する。
- frame 切り出しロジックは既存の `framePainter(sheet, frameIndex)` を再利用する。
- `PlayerSprite` に将来的に `sheetFilePath: String?` または `character: MasterCharacter?` を渡せるようにする。

初回 PR では、パーティ画面やホーム画面のメインキャラクターから適用し、戦闘中の置換は次段階でもよい。全画面を一気に差し替えると回帰範囲が広い。

### 8.3 iOS

SwiftUI 側に Android と同じシート定義を持つ helper を追加する。

- 6列 x 2行
- cell 96px
- frame index:
  - idle: 0..1
  - prep: 2..3
  - attack: 4..8
  - walk: 9..10
  - rest: 11

iOS は `UIImage(contentsOfFile:)` で cache file を読み、`cgImage?.cropping(to:)` で frame を切り出す。SwiftUI の `Image(uiImage:)` へ渡す。

KMP の `SpriteSheetCache` が返す `filePath` を Swift へ渡せるよう、必要なら `KoinHelper` に以下の thin wrapper を用意する。

```kotlin
fun getSpriteSheetFilePath(characterId: String, spriteSheetUrl: String, callback: (String?) -> Unit)
```

ただし、長期的には shared ViewModel 側で表示状態を持ち、SwiftUI は state を購読するほうがよい。

## 9. 実装計画

実装は 5 PR に分ける。DB / API / shared cache を先に固め、UI 適用はホーム・パーティから始める。戦闘画面は位相アニメーションの回帰が大きいため最後に回す。

### PR 1: API / モデル / seed

目的: `sprite_sheet_url` を DB からモバイル domain model まで通す。まだ CDN ダウンロードや UI 表示は入れない。

変更ファイル候補:

- `backend/db/migrations/000002_add_character_sprite_sheet_url.up.sql`
- `backend/db/migrations/000002_add_character_sprite_sheet_url.down.sql`
- `backend/db/seed.sql`
- `backend/internal/model/models.go`
- `backend/api/openapi.yaml`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/domain/model/Character.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/data/remote/dto/CharacterDto.kt`
- 影響する backend / shared tests

実装手順:

1. migration を追加する。

   ```sql
   ALTER TABLE m_characters
     ADD COLUMN sprite_sheet_url text;
   ```

   down migration は以下。

   ```sql
   ALTER TABLE m_characters
     DROP COLUMN IF EXISTS sprite_sheet_url;
   ```

2. `backend/internal/model/models.go` の `MasterCharacter` に nullable field を追加する。

   ```go
   SpriteSheetURL *string `gorm:"type:text" json:"sprite_sheet_url"`
   ```

3. `backend/db/seed.sql` の `m_characters` INSERT に `sprite_sheet_url` カラムを追加する。
   - まずは全キャラ `NULL` でよい。
   - Supabase CDN の実URLが決まったら、対象キャラだけ URL を入れる。

4. `backend/api/openapi.yaml` の `MasterCharacter` schema に `sprite_sheet_url` を追加する。
   - `required` には入れない。
   - `nullable: true` にする。

5. mobile shared の DTO / domain に `spriteSheetUrl` を追加し、mapper で受け渡す。

   ```kotlin
   @SerialName("sprite_sheet_url") val spriteSheetUrl: String? = null
   ```

   ```kotlin
   val spriteSheetUrl: String? = null
   ```

6. JSON parse test がある場合は、`sprite_sheet_url` あり・なしの両方を確認する。

完了条件:

- Backend の master/user character response に `sprite_sheet_url` が含まれる。
- `sprite_sheet_url` 未設定でも既存 API / mobile parse が壊れない。
- この PR では UI の見た目は変えない。

### PR 2: shared cache / downloader

目的: CDN URL からスプライトシートを取得し、platform cache に保存して file path を返せるようにする。まだ主要画面への表示適用は最小限に留める。

変更ファイル候補:

- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/sprite/SpriteSheetCache.kt`
- `apps/mobile/shared/src/androidMain/kotlin/org/example/project/features/sprite/SpriteSheetCache.android.kt`
- `apps/mobile/shared/src/iosMain/kotlin/org/example/project/features/sprite/SpriteSheetCache.ios.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/sprite/SpriteSheetLoader.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/sprite/SpriteSheetModels.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/di/SharedModule.kt`
- `apps/mobile/shared/src/commonTest/kotlin/org/example/project/features/sprite/SpriteSheetLoaderTest.kt`
- `apps/mobile/shared/src/commonTest/kotlin/org/example/project/features/sprite/SpriteSheetCacheKeyTest.kt`

実装手順:

1. common model を追加する。

   ```kotlin
   data class CachedSpriteSheet(
       val characterId: String,
       val sourceUrl: String,
       val filePath: String,
       val byteSize: Long,
   )

   @Serializable
   data class SpriteSheetCacheMetadata(
       val characterId: String,
       val sourceUrl: String,
       val urlHash: String,
       val fileName: String,
       val byteSize: Long,
       val cachedAtEpochMillis: Long,
       val schemaVersion: Int = 1,
   )
   ```

2. URL hash helper を commonMain に置く。
   - SHA-256 が commonMain で扱いづらければ、最初は URL 文字列の percent-safe encode か、既存 dependency を確認してから実装する。
   - ただし保存名は必ず URL 由来の識別子を含める。

3. `expect class SpriteSheetCache()` を追加する。

   ```kotlin
   expect class SpriteSheetCache() {
       suspend fun get(characterId: String, sourceUrl: String): CachedSpriteSheet?
       suspend fun save(characterId: String, sourceUrl: String, bytes: ByteArray): CachedSpriteSheet
       suspend fun remove(characterId: String)
       suspend fun clear()
   }
   ```

4. Android actual を実装する。
   - 保存先: `context.filesDir/sprite_sheets/`
   - context 取得は既存 `KeyValueStore.android.kt` と同じ platform context 管理を使う。
   - `metadata/{characterId}.json` と `images/{characterId}-{urlHash}.png` のように分けると扱いやすい。
   - `save` は `.tmp` に書き込んでから rename する。

5. iOS actual を実装する。
   - 保存先: `NSCachesDirectory/sprite_sheets/`
   - `metadata/{characterId}.json` と `images/{characterId}-{urlHash}.png` を保存する。
   - cache directory が消えていたら再作成する。

6. `SpriteSheetLoader` を追加する。

   ```kotlin
   class SpriteSheetLoader(
       private val client: HttpClient,
       private val cache: SpriteSheetCache,
   ) {
       suspend fun load(characterId: String, spriteSheetUrl: String?): SpriteSheetLoadResult
   }
   ```

   `load` の処理順:
   - null / blank は `NoUrl`
   - `https://` 以外は `Failed`
   - cache hit なら `Hit`
   - Ktor で bytes を取得
   - 最大サイズ超過なら `Failed`
   - `cache.save`
   - `Downloaded`

7. single-flight を入れる。
   - 同一 `characterId + urlHash` の download は `Mutex` でまとめる。
   - lock 中にもう一度 cache hit を確認する。

8. `SharedModule.kt` に登録する。

   ```kotlin
   single { SpriteSheetCache() }
   singleOf(::SpriteSheetLoader)
   ```

9. commonTest を追加する。
   - `spriteSheetUrl == null` は `NoUrl`
   - `http://` は `Failed`
   - cache hit なら download しない
   - URL が変わると miss になる
   - size limit 超過は保存しない

完了条件:

- `SpriteSheetLoader.load(characterId, url)` で file path を取得できる。
- URL 変更時に古い cache を hit しない。
- shared unit test が通る。

### PR 3: Android ホーム / パーティ適用

目的: Android で cached sprite sheet を表示できる UI component を追加し、まずホームとパーティに適用する。

変更ファイル候補:

- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/components/SpriteSheet.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/components/CharacterSprite.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/home/HomeTabContent.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/party/PartyScreenView.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/home/HomeUiState.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/home/HomeViewModel.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/party/PartyViewModel.kt`

実装手順:

1. `SpriteSheet.kt` に file decode を追加する。

   ```kotlin
   @Composable
   fun rememberSheetFromFile(filePath: String?): Bitmap?
   ```

   - `filePath == null` なら null
   - decode 失敗なら null
   - `remember(filePath)` で再 decode を抑える

2. cached sheet 用の frame painter を既存 `framePainter` で使い回す。
   - sheet サイズが `576 x 192` 未満なら null fallback する。
   - 96px cell 前提を定数化する。

3. `CharacterSprite` composable を追加する。

   ```kotlin
   @Composable
   fun CharacterSprite(
       character: MasterCharacter?,
       mode: PlayerSpriteMode,
       size: Dp,
       modifier: Modifier = Modifier,
   )
   ```

   初期実装では内部で `SpriteSheetLoader` を呼ぶより、ViewModel から `filePath` を渡すほうがテストしやすい。Compose component は「filePath があれば sheet、なければ `PlayerSprite`」の描画に集中させる。

4. ViewModel 側で main character の `spriteSheetUrl` を見て loader を起動する。
   - `HomeUiState` に `mainCharacterSpriteSheetPath: String?` を追加する。
   - `PartyUiState` は詳細表示対象 / 一覧表示対象に必要な path map を持たせる。
   - 失敗時は state に error を出さず、ログだけにして default sprite 表示を維持する。

5. `HomeTabContent.kt` のメインキャラクター表示を `CharacterSprite` へ置き換える。
   - `spriteSheetUrl` 未設定なら既存 `PlayerSprite(Idle)` と同じ表示。

6. `PartyScreenView.kt` のメインカード / 詳細画面から適用する。
   - 一覧全件で一斉 download しない。
   - 最初はメインキャラと詳細表示中キャラだけ download する。

完了条件:

- Android ホームで CDN sheet が cache hit すると idle 表示へ使われる。
- ダウンロード中 / 失敗 / URLなしでは既存 default sprite が出る。
- パーティ一覧表示で最大30件の同時ダウンロードを起こさない。

### PR 4: iOS ホーム / パーティ適用

目的: iOS で cached sprite sheet を表示できる helper を追加し、ホームとパーティに適用する。

変更ファイル候補:

- `apps/mobile/iosApp/iosApp/SpriteSheetImage.swift`
- `apps/mobile/iosApp/iosApp/HomeScreenView.swift`
- `apps/mobile/iosApp/iosApp/PartyScreenView.swift`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/di/KoinHelper.kt`
- 必要に応じて shared ViewModel / UiState

実装手順:

1. Swift helper を追加する。

   ```swift
   enum SpriteSheetFrame: Int {
       case idle1 = 0
       case idle2 = 1
       case prep1 = 2
       case prep2 = 3
       case attack1 = 4
       case attack2 = 5
       case attack3 = 6
       case attack4 = 7
       case attack5 = 8
       case walk1 = 9
       case walk2 = 10
       case rest1 = 11
   }
   ```

2. `UIImage(contentsOfFile:)` と `cgImage.cropping(to:)` で frame を切り出す。
   - `scale` を考慮して pixel rect で切る。
   - sheet が小さい / decode 不能なら nil を返す。

3. `CachedCharacterSpriteView` を追加する。
   - `filePath` と mode を受け取り、取れなければ既存 `IdleSpriteView` / `PlayerSpriteView` に fallback する。

4. Swift から shared loader を呼ぶ導線を決める。
   - 推奨: Android と同じく shared ViewModel state に file path を持たせる。
   - つなぎとして `KoinHelper` に callback wrapper を追加してもよいが、画面ごとの lifecycle 管理に注意する。

5. `HomeScreenView.swift` のメインキャラクター表示に適用する。

6. `PartyScreenView.swift` のメインカード / 詳細画面に適用する。

完了条件:

- iOS ホームで CDN sheet が cache hit すると idle 表示へ使われる。
- ダウンロード中 / 失敗 / URLなしでは既存 default sprite が出る。
- SwiftUI 再描画で download が連打されない。

### PR 5: 戦闘画面適用

目的: Android / iOS の StudyQuest 戦闘表示を cached sprite sheet に対応させる。

変更ファイル候補:

- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/study/StudyQuestScreenView.kt`
- `apps/mobile/iosApp/iosApp/StudyQuestScreenView.swift`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/study/StudyQuestUiState.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/study/StudyQuestViewModel.kt`

実装手順:

1. `StudyQuestUiState` にメインキャラの `spriteSheetPath` または sprite source を追加する。

2. `StudyQuestViewModel` でパーティ先頭キャラの `spriteSheetUrl` を解決し、loader を呼ぶ。
   - quest 開始時に1回だけ試す。
   - 失敗しても quest は続行し、default sprite に fallback する。

3. Android `BattleConfrontationLayer` / `TrainingGroundLayer` / break scene で cached sheet を使えるようにする。
   - mode と frame range は既存 `PlayerSpriteMode` に合わせる。
   - cached sheet がない場合は既存 `PlayerSprite` をそのまま使う。

4. iOS `PlayerSpriteView` を cached sheet source に対応させる。
   - `frames: [UIImage]` または `filePath + mode` を受ける形に拡張する。
   - 既存 imageset fallback は残す。

5. 位相ごとの期待表示を確認する。
   - walking: walk 1/2 loop
   - idle: idle 1/2 loop
   - prep: prep 1/2 one-shot
   - attack: attack 1..5 one-shot
   - rest: rest 1

完了条件:

- Android / iOS の戦闘中プレイヤーが cached sheet で mode 別に動く。
- default sprite fallback が維持される。
- StudyQuest の既存 phase 表示が崩れない。

## 10. 実装時の判断ルール

- 最初の表示適用はホームとパーティ詳細に限定する。パーティ一覧30件の一括 download は避ける。
- `sprite_sheet_url` は nullable のまま扱う。未設定は正常系。
- cache file path は UI state に出してよいが、URL や download error の詳細は UI 表示しない。
- CDN download 失敗で画面全体を error にしない。常に default sprite fallback で継続する。
- URL が変わったら必ず別 cache として扱う。
- 破損 PNG は cache hit 扱いにしない。decode 失敗時は該当 cache を削除して再取得できる状態にする。
- KMP shared から Android `Bitmap` / iOS `UIImage` は返さない。shared は file path まで。
- SwiftUI / Compose の Composable 内で無制限に download を開始しない。ViewModel または lifecycle を制御できる箇所から呼ぶ。

## 11. 受け入れ条件

- `sprite_sheet_url` が設定されたキャラクターは、初回表示後に CDN からスプライトシートを取得してローカル保存される。
- 2回目以降の表示ではネットワークなしでもキャッシュ済みシートを使える。
- `sprite_sheet_url` が未設定、ダウンロード中、失敗時は既存のバンドル済みデフォルトスプライトが表示される。
- URL が変更された場合、古いキャッシュではなく新しい URL のシートを取得する。
- Android / iOS で同じ frame index 定義を使い、idle / walk / prep / attack / rest が崩れない。
- CI の Backend test、Mobile Android compile、Mobile unit test、iOS build が通る。

## 12. テストケース

### Backend

- `MasterCharacter` JSON に `sprite_sheet_url` が含まれる。
- `sprite_sheet_url == null` の既存 seed でも API response が成立する。
- migration up/down が通る。

### shared

- `SpriteSheetLoader.load(characterId, null)` returns `NoUrl`
- `SpriteSheetLoader.load(characterId, "")` returns `NoUrl`
- `http://...` is rejected
- cache hit の場合は HTTP client が呼ばれない
- URL A の cache があっても URL B では miss になる
- download 成功時に metadata と PNG が保存される
- download 失敗時に tmp file が残らない
- size limit 超過時に保存しない
- 同一 characterId + URL の同時 load で download が1回になる

### Android

- `rememberSheetFromFile(null)` は null
- 存在しない file path は null
- 破損 PNG は null
- 576x192 の sheet から frame index 0 / 8 / 11 を切り出せる
- cached sheet path があると `CharacterSprite` が sheet を使う
- path がないと default `PlayerSprite` へ fallback する

### iOS

- 存在しない file path は nil
- 破損 PNG は nil
- 576x192 の sheet から frame index 0 / 8 / 11 を切り出せる
- SwiftUI 再描画で loader が連打されない
- path がないと existing imageset fallback を使う

## 13. 検証計画

### Backend

```bash
cd backend && CGO_ENABLED=1 go test ./... -count=1 && go vet ./...
```

確認観点:

- migration 適用
- seed 実行
- `/api/v1/master/characters`
- `/api/v1/users/{userId}/characters`

### Mobile

```bash
cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
cd apps/mobile && ./gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid
cd apps/mobile && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

ローカル macOS では必要に応じて iOS `xcodebuild` も実行する。

### 手動確認

- 初回表示でデフォルトスプライトが出る
- ダウンロード完了後に CDN スプライトへ切り替わる
- アプリ再起動後、ネットワークなしでも cache hit する
- CDN URL を変更すると再取得される
- 不正 URL / 404 / 破損 PNG でクラッシュせずフォールバックする

## 14. リスクと対策

| リスク | 対策 |
|--------|------|
| URL 変更後も古い画像が表示される | `{characterId}-{urlHash}.png` と metadata で cache hit を判定する |
| 複数画面で同時に同じ画像を落とす | characterId + urlHash 単位の single-flight を入れる |
| 壊れた PNG を保存してしまう | 一時ファイル保存、decode 成功確認後に正式保存する |
| iOS の cache directory が OS に削除される | miss として再ダウンロードし、UI はフォールバックする |
| CDN URL が漏れる | ログに URL を出さず、公開 CDN 前提の URL だけを扱う |
| 実装範囲が広すぎる | Phase 1/2 を先に入れ、UI 適用はホーム/パーティから段階導入する |

## 15. オープン事項

1. Supabase Storage の bucket / path 命名規則
   - 例: `character-sprites/{characterId}/sheet-v1.png`
2. CDN URL を public URL にするか、署名付き URL にするか
   - まずは public read を推奨。署名付き URL は期限切れとキャッシュキー設計が複雑になる。
3. Android の保存先を `filesDir` に固定するか `cacheDir` にするか
   - Issue 案どおり `filesDir` で開始し、容量問題が出たら clear / LRU を追加する。
4. 最初に適用する UI
   - 推奨はホームとパーティ詳細。戦闘画面は Phase 5 で適用する。

## 16. 結論

実装は一気に全画面へ入れず、まず API / モデル / shared cache を固める。そのうえでホーム・パーティに cached sheet 表示を導入し、最後に戦闘画面へ広げる。

保存名は `{characterId}.png` ではなく `{characterId}-{urlHash}.png` にする。これが今回いちばん大事な設計判断で、CDN 差し替え時の stale cache を避けられる。
