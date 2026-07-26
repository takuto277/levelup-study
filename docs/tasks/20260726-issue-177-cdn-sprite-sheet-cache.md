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

## 9. 実装ステップ

### Phase 1: API とモデル

1. migration 追加
2. `MasterCharacter.SpriteSheetURL` 追加
3. `seed.sql` の INSERT カラム追加
4. `openapi.yaml` 追加
5. mobile DTO / domain 追加
6. backend / mobile の既存テスト更新

### Phase 2: shared cache

1. `SpriteSheetCache` expect/actual 追加
2. metadata 保存・読込・URL hash 実装
3. `SpriteSheetLoader` 追加
4. Koin `SharedModule` に `single { SpriteSheetCache() }` と loader を登録
5. commonTest で cache hit/miss 判定、URL変更、NoUrl、サイズ超過をテスト

### Phase 3: Android UI

1. `SpriteSheet.kt` に file decode を追加
2. `PlayerSprite` または新規 `CharacterSprite` に sheet source を渡せるようにする
3. ホーム / パーティ詳細から適用
4. ダウンロード中は既存バンドル表示

### Phase 4: iOS UI

1. Swift helper で sprite sheet crop 実装
2. KMP loader から file path を取得
3. ホーム / パーティ詳細から適用
4. ダウンロード中は既存バンドル表示

### Phase 5: 戦闘画面適用

1. `StudyQuest` のメインキャラクター表示へ sheet source を渡す
2. phaseTick と mode に合わせて cached sheet の frame を表示
3. Android / iOS の見た目差分を確認

## 10. 受け入れ条件

- `sprite_sheet_url` が設定されたキャラクターは、初回表示後に CDN からスプライトシートを取得してローカル保存される。
- 2回目以降の表示ではネットワークなしでもキャッシュ済みシートを使える。
- `sprite_sheet_url` が未設定、ダウンロード中、失敗時は既存のバンドル済みデフォルトスプライトが表示される。
- URL が変更された場合、古いキャッシュではなく新しい URL のシートを取得する。
- Android / iOS で同じ frame index 定義を使い、idle / walk / prep / attack / rest が崩れない。
- CI の Backend test、Mobile Android compile、Mobile unit test、iOS build が通る。

## 11. 検証計画

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

## 12. リスクと対策

| リスク | 対策 |
|--------|------|
| URL 変更後も古い画像が表示される | `{characterId}-{urlHash}.png` と metadata で cache hit を判定する |
| 複数画面で同時に同じ画像を落とす | characterId + urlHash 単位の single-flight を入れる |
| 壊れた PNG を保存してしまう | 一時ファイル保存、decode 成功確認後に正式保存する |
| iOS の cache directory が OS に削除される | miss として再ダウンロードし、UI はフォールバックする |
| CDN URL が漏れる | ログに URL を出さず、公開 CDN 前提の URL だけを扱う |
| 実装範囲が広すぎる | Phase 1/2 を先に入れ、UI 適用はホーム/パーティから段階導入する |

## 13. オープン事項

1. Supabase Storage の bucket / path 命名規則
   - 例: `character-sprites/{characterId}/sheet-v1.png`
2. CDN URL を public URL にするか、署名付き URL にするか
   - まずは public read を推奨。署名付き URL は期限切れとキャッシュキー設計が複雑になる。
3. Android の保存先を `filesDir` に固定するか `cacheDir` にするか
   - Issue 案どおり `filesDir` で開始し、容量問題が出たら clear / LRU を追加する。
4. 最初に適用する UI
   - 推奨はホームとパーティ詳細。戦闘画面は Phase 5 で適用する。

## 14. 結論

実装は一気に全画面へ入れず、まず API / モデル / shared cache を固める。そのうえでホーム・パーティに cached sheet 表示を導入し、最後に戦闘画面へ広げる。

保存名は `{characterId}.png` ではなく `{characterId}-{urlHash}.png` にする。これが今回いちばん大事な設計判断で、CDN 差し替え時の stale cache を避けられる。
