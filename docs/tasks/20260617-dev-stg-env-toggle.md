# 実行計画: デバッグ画面に dev/stg 環境切り替えトグル

- **対象 Issue**: [#158](https://github.com/takuto277/levelup-study/issues/158) `feat(mobile): デバッグ画面に dev/stg 環境切り替えトグルを追加`
- **Issue 本文控え**: `docs/tasks/issue-body-dev-stg-env-toggle.md`
- **作成日**: 2026-06-17
- **更新日**: 2026-06-18

## 背景と目的

現在、モバイルの API 接続先は `ApiRoutes.BASE_URL` に本番 URL 固定で定義されている。ローカル Docker 開発環境や stg を確認したい場合、コードを書き換えて再ビルドする必要がある。

本 Issue では、デバッグビルドの設定画面から dev / stg を切り替え、次の API 通信から選択した環境へ向けられる状態を作る。prod 切り替えやビルドフレーバー整備は別 Issue の範囲とし、今回の UI では dev / stg の2択に限定する。

## 今回のスコープ

### やること

- `ApiRoutes.BASE_URL` を実行時に現在の環境 URL を返すプロパティに変更する。
- dev / stg / prod を表す共通の環境モデルを追加する。
- デバッグビルド起動時は保存済みの dev / stg 選択を復元し、未保存なら dev を使う。
- リリースビルド起動時は保存値を無視し、prod URL 固定にする。
- `ApiClient` がリクエストごとに現在の base URL を参照するようにして、既存の Koin single Gateway / Repository を作り直さずに切り替えを反映する。
- `UserSessionStore` の `KeyValueStore` 永続化に、デバッグ用の選択環境を保存する。
- `SettingsUiState` / `SettingsViewModel` に選択環境と切り替え intent を追加する。
- Android `SettingsScreenDialog` のデバッグセクション末尾に dev / stg 切替 UI を追加する。
- iOS `SettingsScreenView` の `debugSection` 末尾に同等の segmented control を追加する。

### やらないこと

- prod への手動切り替え UI。
- stg / prod の API key、JWT secret、Supabase プロジェクト分離。
- Android `local.properties` や iOS xcconfig による URL 注入。
- Koin 全体の再起動、既存 ViewModel の強制再生成。
- 環境切り替え時のキャッシュ全消去やログアウト。

## 既存コード調査結果

### `ApiRoutes.kt`

- `const val BASE_URL = "https://levelup-study-api.onrender.com"` で本番 URL 固定。
- `const val` は実行時に変更できないため、`val BASE_URL: String get() = ...` へ変更する。
- 各 endpoint path は `fun user(userId)` など相対パスで定義されているため、base URL の動的化だけで既存 gateway の呼び出しは保てる。

### `ApiClient.kt`

- `fun create(baseUrl: String = ApiRoutes.BASE_URL)` で `baseUrl` を作成時に捕捉し、`defaultRequest { url(baseUrl) }` に渡している。
- このままだと `HttpClient` 作成後の環境変更が反映されない。
- `HttpClient` 自体を再生成する案もあるが、現在の DI では Gateway / Repository が `single` のため、`HttpClient` だけ `factory` にしても既存 single は古い client を握り続ける。
- よって今回の設計では、`defaultRequest { url(ApiRoutes.BASE_URL) }` のようにリクエストごとに現在値を読む形へ変更する。

### `SharedModule.kt`

- `HttpClient`、Gateway、Repository、`HomeViewModel`、`SettingsViewModel` は `single`。
- `factory { ApiClient.create() }` へ変えるだけでは、既存の `singleOf(::UserGateway)` 等は再生成されない。
- Koin unload/reload は影響範囲が大きく、SwiftUI 側の ViewModel 保持とも相性が悪い。
- 今回は DI 定義を極力変えず、既存 single が持つ `HttpClient` のリクエスト時 base URL だけを動的化する。

### `UserSessionStore.kt`

- `KeyValueStore` で `user_id` / `auth_token` を永続化している。
- Issue 要件どおり、デバッグ用の選択環境も同じ永続化基盤に乗せる。
- 環境選択はログインセッションではないため、`clear()` では消さない設計にする。

### `SettingsViewModel.kt` / `SettingsUiState.kt`

- `apiBaseUrl` はあるが、現在は `syncFromServer()` 成功時にだけ更新される。
- dev server が落ちている場合にも現在選択中の URL を表示したいため、`refresh()` のネットワーク呼び出し前にも環境表示フィールドを更新する。
- `selectEnvironmentFromPlatform(envName: String)` を追加し、Swift からも呼びやすい API にする。

### Android / iOS Settings UI

- Android は `if (isDebug)` ブロック内にデバッグ情報と通貨操作 UI がある。
- iOS は `#if DEBUG` の `debugSection` 内に同等の UI がある。
- Issue の「デバッグセクション末尾」要件に合わせ、既存の通貨操作説明文の下に「環境」セクションを追加する。

## 設計

### 1. 環境モデル

`core/network` に `ApiEnvironment` を追加する。

```kotlin
enum class ApiEnvironment(
    val key: String,
    val label: String,
    val baseUrl: String,
) {
    DEV("dev", "dev", "http://localhost:8080"),
    STG("stg", "stg", "https://levelup-study-api-stg.onrender.com"),
    PROD("prod", "prod", "https://levelup-study-api.onrender.com");

    companion object {
        fun fromKey(key: String?): ApiEnvironment =
            entries.firstOrNull { it.key == key } ?: DEV
    }
}
```

prod は release 固定用に enum へ含めるが、デバッグ UI の選択肢には出さない。

### 2. 現在環境の管理

`core/network` に `ApiEnvironmentStore` を追加する。永続化そのものは Issue 要件に合わせて `UserSessionStore` 経由で行う。
依存方向を単純にするため、`UserSessionStore` は環境 enum を知らず、保存済みの文字列 key だけを扱う。

```kotlin
object ApiEnvironmentStore {
    private var isDebugBuild: Boolean = false
    private var current: ApiEnvironment = ApiEnvironment.PROD

    fun initialize(isDebugBuild: Boolean) {
        this.isDebugBuild = isDebugBuild
        current = if (isDebugBuild) {
            ApiEnvironment.fromKey(UserSessionStore.getSelectedApiEnvironmentKey())
        } else {
            ApiEnvironment.PROD
        }
    }

    fun current(): ApiEnvironment = current
    fun currentBaseUrl(): String = current.baseUrl

    fun selectDebugEnvironment(env: ApiEnvironment) {
        require(isDebugBuild) { "API environment can be changed only in debug builds." }
        require(env != ApiEnvironment.PROD) { "prod is not selectable from debug settings." }
        current = env
        UserSessionStore.setSelectedApiEnvironmentKey(env.key)
    }
}
```

`UserSessionStore` には次を追加する。

```kotlin
private const val KEY_SELECTED_API_ENV = "debug_selected_api_environment"

fun getSelectedApiEnvironmentKey(): String? =
    store.getString(KEY_SELECTED_API_ENV)

fun setSelectedApiEnvironmentKey(envKey: String) {
    if (envKey == "prod") return
    store.putString(KEY_SELECTED_API_ENV, envKey)
}
```

### 3. `ApiRoutes.BASE_URL`

既存の参照箇所を壊さないため、名前は `BASE_URL` のまま維持する。ただし `const val` ではなく getter にする。

```kotlin
object ApiRoutes {
    val BASE_URL: String
        get() = ApiEnvironmentStore.currentBaseUrl()
}
```

### 4. `ApiClient`

`create(baseUrl: String = ...)` の引数捕捉をやめるか、少なくともデフォルト実装では現在値を毎回読む。

```kotlin
fun create(): HttpClient {
    return HttpClient {
        defaultRequest {
            url(ApiRoutes.BASE_URL)
            contentType(ContentType.Application.Json)
            // API key / Authorization は既存どおり
        }
    }
}
```

これにより、既存の single `HttpClient` / Gateway / Repository / ViewModel を保持したまま、次のリクエストから新しい base URL を使える。

### 5. 起動時初期化

Android:

```kotlin
initKeyValueStore(this)
val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
initApiEnvironment(isDebugBuild = isDebug)
initKoin()
setDevSession(useSeedUser = isDebug, forceSeedUserId = isDebug)
```

iOS:

```swift
#if DEBUG
KoinHelperKt.initApiEnvironment(isDebugBuild: true)
#else
KoinHelperKt.initApiEnvironment(isDebugBuild: false)
#endif
KoinHelperKt.doInitKoin()
```

Swift から呼びやすいよう、`KoinHelper.kt` に `fun initApiEnvironment(isDebugBuild: Boolean)` を追加する。

### 6. Settings state / ViewModel

`SettingsUiState`:

```kotlin
data class SettingsUiState(
    val apiBaseUrl: String = "",
    val selectedEnvironment: String = "dev",
    val canSwitchEnvironment: Boolean = false,
    // 既存 fields...
)
```

`SettingsViewModel`:

```kotlin
fun selectEnvironmentFromPlatform(envName: String) {
    val env = ApiEnvironment.fromKey(envName)
    ApiEnvironmentStore.selectDebugEnvironment(env)
    _uiState.update {
        it.copy(
            selectedEnvironment = env.key,
            apiBaseUrl = env.baseUrl,
            toast = "API環境を ${env.label} に切り替えました",
        )
    }
}
```

`refresh()` は `syncFromServer()` 前に現在環境を state に反映する。ネットワークが失敗しても URL 表示が空にならないようにする。

### 7. Android UI

`SettingsScreenDialog` の `if (isDebug)` 内の末尾に追加する。

- 見出し: `環境`
- 現在 URL: 既存の `API ベース URL` 表示を継続
- 操作: dev / stg の2択
- Compose 部品: `FilterChip` または `SingleChoiceSegmentedButtonRow`

`FilterChip` 案:

```kotlin
Text("環境", style = MaterialTheme.typography.titleSmall)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("dev", "stg").forEach { env ->
        FilterChip(
            selected = state.selectedEnvironment == env,
            onClick = { vm.selectEnvironmentFromPlatform(env) },
            label = { Text(env) },
        )
    }
}
```

### 8. iOS UI

`debugSection` の末尾に `Picker` を追加する。

```swift
Picker("環境", selection: environmentBinding) {
    Text("dev").tag("dev")
    Text("stg").tag("stg")
}
.pickerStyle(.segmented)
```

`Binding` は `state?.selectedEnvironment` を読み、setter で `vm.selectEnvironmentFromPlatform(envName:)` を呼ぶ。

### 9. release build の扱い

- Android release は `isDebug == false` なので UI は既存どおり非表示。
- iOS release は `#if DEBUG` が外れるので UI は非表示。
- `ApiEnvironmentStore.initialize(false)` で prod 固定にするため、debug 時に保存された stg が release に漏れない。

## 実装手順

1. `ApiEnvironment` / `ApiEnvironmentStore` を `core/network` に追加する。
2. `UserSessionStore` に debug API environment の永続化メソッドを追加する。
3. `ApiRoutes.BASE_URL` を `const val` から動的 getter に変更する。
4. `ApiClient.create()` の `defaultRequest` が `ApiRoutes.BASE_URL` をリクエスト時に読むようにする。
5. `KoinHelper.kt` に `initApiEnvironment(isDebugBuild: Boolean)` を追加する。
6. Android `MainActivity` で `initKoin()` 前に環境初期化を呼ぶ。
7. iOS `iOSApp` で `doInitKoin()` 前に環境初期化を呼ぶ。
8. `SettingsUiState` に `selectedEnvironment` / `canSwitchEnvironment` を追加する。
9. `SettingsViewModel` に `selectEnvironmentFromPlatform()` を追加し、`refresh()` の先頭で環境表示を反映する。
10. Android `SettingsScreenDialog` の debug セクション末尾に dev / stg 切替 UI を追加する。
11. iOS `SettingsScreenView` の `debugSection` 末尾に segmented picker を追加する。
12. 必要に応じて `SettingsViewModel` のユニットテストまたは環境ストアの commonTest を追加する。

## 検証計画

自動検証:

- `./scripts/validate-pr.sh`
- iOS UI 変更を含むため、可能なら `./scripts/validate-pr.sh --ios`
- 変更が mobile のみの場合、追加で `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest`

手動確認:

- Android debug build の設定画面に「環境」セクションが表示される。
- iOS debug build の設定画面に「環境」セクションが表示される。
- 初回起動時は dev が選択され、`apiBaseUrl` が dev URL を表示する。
- stg を選択すると `apiBaseUrl` が `https://levelup-study-api-stg.onrender.com` に変わる。
- stg 選択後にデバッグ通貨更新やユーザー情報更新が stg に向く。
- アプリ再起動後も debug build では選択した dev / stg が復元される。
- release build では環境 UI が表示されず、prod URL が使われる。

## リスクと未決事項

- **Android emulator の localhost 問題**: Issue では dev URL が `http://localhost:8080` と定義されているが、Android emulator からホスト Mac の localhost へ接続する場合は通常 `http://10.0.2.2:8080` が必要。Issue の文字列を厳密に守ると Android の手動確認が失敗する可能性がある。実装前に「Android dev URL も localhost 固定でよいか」「platform-specific dev URL にするか」を確認したい。
- **stg の API key / JWT**: 現状は `GENERATED_CLIENT_API_KEY` / `GENERATED_DEV_JWT` がビルド時固定。stg が本番と異なる API key / JWT secret を要求する場合、URL 切替だけでは通信できない。#158 では URL 切替までに留め、認証情報の環境分離は別 Issue とする。
- **既存画面の in-flight request**: 環境変更前に開始済みのリクエストは旧URLに向く可能性がある。次回リクエストから反映されれば受け入れ条件上は十分とする。
- **キャッシュ整合性**: dev / stg で同じ seed user ID を使っても、所持データやマスタが異なる可能性がある。環境切替時にキャッシュ全消去までは行わず、設定画面で toast 表示し、閉じる時の既存 refresh 導線に任せる。
