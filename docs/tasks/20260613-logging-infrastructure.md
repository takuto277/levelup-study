# ログ基盤の設計と実装

| 項目 | 内容 |
|------|------|
| 作成日 | 2026-06-13 |
| ステータス | 設計済み |
| 対象 | Backend + Android + iOS + KMP shared |

## 背景

現在のログ状況:

| 層 | 現状 | 課題 |
|----|------|------|
| Backend | `log.Println` + カスタム API ログミドルウェア（🌱 プレフィックス） | 構造化なし、ログレベルなし |
| KMP shared | アドホックな `println()` 3箇所 + Ktor `LogLevel.BODY` 常時ON | レベル制御なし、リリースで秘密情報漏洩リスク |
| Android | `android.util.Log` 未使用 | 実機デバッグ不可 |
| iOS | `os_log` 未使用 | 実機デバッグ不可 |
| 全体 | クラッシュレポート・分析 SDK ゼロ | 本番障害検知不可 |

## 目的

1. **開発効率**: 各層で一貫したログ出力とフィルタリングができる
2. **デバッグ容易性**: 実機でもログを確認できる（Android logcat / iOS Console.app / アプリ内ビューア）
3. **本番障害検知**: 最低限のエラーログを収集し、起票・対応できる
4. **段階的導入**: 一気に全部やらず、MVP で価値が出る最小構成から

## 設計方針

### 1. ログレベル

```
VERBOSE → 詳細（HTTP body、DB query、state 遷移の全内容）
DEBUG   → 開発時のみ（メソッド呼び出し、分岐、中間状態）
INFO    → 本番も含む通常イベント（画面遷移、API 成功、セッション開始/終了）
WARN    → 要注意（API リトライ、オフライン検知、バリデーション失敗）
ERROR   → 要対応（クラッシュ、500エラー、データ不整合）
```

| ビルド | 出力する最低レベル |
|--------|-------------------|
| debug | VERBOSE |
| release | INFO |

### 2. タグ体系

```
# カテゴリ + 画面/機能 の2階層
Lifecycle/HomeScreen     → 画面の表示/非表示
UserAction/HomeScreen    → ボタンタップ等
Network/StudyComplete    → API リクエスト/レスポンス
State/RecordViewModel    → ViewModel の状態遷移
Error/PartyRepository    → エラー発生箇所
Perf/StudyQuestScreen    → パフォーマンス計測
```

### 3. 出力先（プラットフォーム別）

| プラットフォーム | debug ビルド | release ビルド |
|-----------------|-------------|---------------|
| Backend | stderr + ファイル | stderr（Render log drain） |
| Android | logcat | リングバッファ（直近500件）+ クラッシュ時に送信 |
| iOS | os_log + Console.app | リングバッファ + クラッシュ時に送信 |

### 4. 絶対にログに出さないもの

- JWT トークン、API キー
- パスワード、メールアドレス
- 個人を特定できる学習データの詳細
- 位置情報、デバイス識別子
- ユーザーID は `user=xxx` ではなく一方向ハッシュ化して `user=<hash>` で出力

---

## 実装計画

### Phase 1: 基盤（全プラットフォーム共通）

#### 1-1. KMP shared に Logger インターフェースを追加

```kotlin
// shared/.../core/logging/Logger.kt
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

interface Logger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

fun Logger.v(tag: String, msg: String) = log(LogLevel.VERBOSE, tag, msg)
fun Logger.d(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg)
fun Logger.i(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)
fun Logger.w(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.WARN, tag, msg, t)
fun Logger.e(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.ERROR, tag, msg, t)

expect fun platformLogger(): Logger
```

#### 1-2. Android actual（`android.util.Log`）

```kotlin
// androidMain/.../core/logging/Logger.android.kt
actual fun platformLogger(): Logger = AndroidLogger

object AndroidLogger : Logger {
    var minimumLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.INFO

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minimumLevel.ordinal) return
        val tag2 = "LevelUp/$tag".take(23)  // logcat 23文字制限
        when (level) {
            LogLevel.ERROR -> Log.e(tag2, message, throwable)
            LogLevel.WARN  -> Log.w(tag2, message, throwable)
            LogLevel.INFO  -> Log.i(tag2, message)
            else           -> Log.d(tag2, message)
        }
        // リングバッファに常に蓄積（本番クラッシュ送信用）
        RingBuffer.append(level, tag, message)
    }
}
```

#### 1-3. iOS actual（`os_log`）

```swift
// iosApp 側で Objective-C ブリッジ経由、もしくは KMP で簡易実装
actual fun platformLogger(): Logger = IosLogger

object IosLogger : Logger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        // KMP から直接 os_log を叩けないため、Swift のヘルパー経由
        // もしくは NSLog で代用（デバッグ時のみ）
        IosLoggerHelper.log(level.ordinal, tag, message)
    }
}
```

#### 1-4. Backend（slog 標準ライブラリ）

Go 1.21+ の `log/slog` を使用。依存ゼロで構造化ログに対応。

```go
// backend/internal/logging/logger.go
package logging

import "log/slog"

var Logger *slog.Logger

func Init(level slog.Level, format string) {
    // format: "json" または "text"
    // Render では stderr に JSON 出力（自動的に log drain で収集される）
}
```

---

### Phase 2: アプリ内ログビューア（debug ビルド限定）

#### Android 設定画面に「開発者ログ」セクションを追加

- 設定画面から `DeveloperLogScreen` に遷移
- リングバッファの直近 500 件を LazyColumn で表示
- ログレベルフィルター + 検索
- 右上の共有ボタンでログをテキストとして共有（サポート問い合わせ用）

#### iOS 設定画面に同様のセクション

- `DeveloperLogView` を SwiftUI で実装
- リングバッファの内容を List 表示
- `.toolbar` に ShareLink でテキスト共有

---

### Phase 3: 各画面へのログ埋め込み（重要ポイントのみ）

無駄なログを避けるため、以下のルールで埋め込む:

1. **エラーは必ずログに残す**（try-catch の catch 節、API エラーレスポンス）
2. **画面遷移は INFO レベルで記録**（画面名 + 遷移元/先）
3. **重要なユーザー操作は DEBUG レベルで記録**（ガチャ実行、勉強開始、編成変更）
4. **状態遷移は VERBOSE レベル**（ViewModel の state 変更内容）
5. **パフォーマンスは INFO レベル**（API レイテンシが 3 秒超えた場合のみ WARN）

#### 埋め込み対象（優先度順）

| 画面/機能 | ログ種別 | 具体例 |
|----------|---------|--------|
| **API レイヤ** | ERROR + INFO | 全 API の成功/失敗 + レイテンシ |
| **勉強セッション** | INFO + ERROR | 開始/中断/完了、報酬計算 |
| **ガチャ** | INFO + ERROR | 実行/結果/残高、競合エラー |
| **パーティ編成** | INFO | スロット変更 |
| **画面遷移** | INFO | タブ切り替え、モーダル開閉 |
| **オフライン同期** | WARN + INFO | 未同期検出、再送回数 |
| **クラッシュ境界** | ERROR | キャッチされない例外 |
| **認証** | WARN | 401/403 発生、トークン期限切れ |

#### 埋め込み例（Kotlin）

```kotlin
// ViewModel 内
private val log = platformLogger()

fun onIntent(intent: StudyIntent) {
    when (intent) {
        is StudyIntent.CompleteStudy -> {
            log.i("UserAction/Study", "completeStudy duration=${intent.durationSeconds}s")
            viewModelScope.launch {
                try {
                    val result = studyUseCase.completeSession(...)
                    log.i("Network/StudyComplete", "success sessionId=${result.sessionId}")
                } catch (e: Exception) {
                    log.e("Error/StudyComplete", "failed", e)
                }
            }
        }
    }
}
```

```go
// Backend handler
func (h *GameHandler) UpdatePartySlot(w http.ResponseWriter, r *http.Request) {
    logging.Logger.Info("party slot update", "slot", slotPos, "char", req.UserCharacterID)
    // ...
    if err != nil {
        logging.Logger.Error("party upsert failed", "err", err)
    }
}
```

---

### Phase 4: ログの活用方法

#### 4-1. デバッグ

```
開発中の流れ:
1. 実機で操作 → logcat / Console.app でリアルタイム確認
2. 設定画面のログビューアで履歴確認
3. クラッシュ時はリングバッファから直前の状態を確認
```

#### 4-2. CI での活用

```
CI テスト失敗時:
1. テストログを stderr に出力（Go test -v）
2. 失敗ケースの前後 20 行を GitHub Actions の annotation として表示
3. KMP test は Gradle --info でログ出力
```

#### 4-3. 本番障害検知（将来）

```
MVP では Render log drain で確認。
将来的な拡張:
1. Render log drain → Papertrail / Logtail（無料枠あり）
2. ERROR レベルのログを Slack / Discord に通知
3. クラッシュ時はリングバッファを次回起動時にサーバー送信
```

#### 4-4. サポート問い合わせ

```
ユーザーからの問い合わせフロー:
1. ユーザーが設定画面から「ログを共有」をタップ
2. 直近 500 件のログがテキストとしてクリップボードにコピーされる
3. サポートフォームに貼り付け
→ PII は出ていない（設計により保証）ので安全に共有可能
```

---

## 実装手順

1. KMP shared に `Logger` interface + expect/actual を追加
2. Android actual（android.util.Log + リングバッファ）を実装
3. iOS actual（NSLog + リングバッファ）を実装
4. Backend に `log/slog` ベースの構造化ログを導入
5. 既存の `println` / `log.Println` を順次置き換え
6. Ktor Logging を debug ビルドのみに制限
7. Android 設定画面に開発者ログビューアを追加
8. iOS 設定画面に開発者ログビューアを追加
9. 優先度の高い画面からログ埋め込み（API レイヤ → 勉強 → ガチャ → 編成）
10. CI テストのログ出力を改善

## 受け入れ条件

- debug ビルドでは VERBOSE レベルまで出力される
- release ビルドでは INFO 以上のみ出力される
- Android logcat / iOS Console.app でログを確認できる
- 設定画面のログビューアで過去 500 件のログを見られる
- API エラーは常にログに残る
- 秘密情報（JWT、パスワード等）は絶対に出力されない
- リリースビルドに DEBUG/VERBOSE ログ文字列が残らない（R8/ProGuard での削除）

## 補足: 二重操作ガード実装状況

- GachaViewModel: PULLING phase ガード済み (PR #128)
- PartyViewModel: isLoading ガード済み (PR #135)
- SettingsViewModel: refreshMutex + isLoading ガード済み
