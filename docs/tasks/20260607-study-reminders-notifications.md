# 学習リマインダー・通知設定 要件定義 / 設計

| 項目 | 内容 |
|------|------|
| Issue | [#62 学習リマインダー・通知設定](https://github.com/takuto277/levelup-study/issues/62) |
| 作成日 | 2026-06-07 |
| ステータス | 設計済み |
| 対象 | Mobile Android / iOS |

## 背景

LevelUp Study は、勉強完了・冒険進行・報酬・ガチャ・編成・記録のループを持つ。リリース後に継続利用を作るには、ユーザーが「今日の勉強を忘れた」ときに戻ってこられる導線が必要。

ただし初回起動直後に通知許可を求めると拒否されやすい。まずはユーザーがアプリの価値を理解した後、設定可能なローカル通知として導入する。

## 目的

- ユーザーが自分の生活リズムに合わせて毎日の学習リマインダーを受け取れる
- 通知はしつこくなく、設定からいつでも止められる
- Android / iOS で同じ設定項目・同じ意味の通知体験にする
- MVP ではサーバー Push を使わず、端末ローカル通知で実装する

## 今回のスコープ

### 必須

- 通知 ON / OFF
- 通知時刻の設定
- OS 通知許可の要求
- Android / iOS のローカル通知スケジュール
- 設定画面で現在の通知状態を確認できる
- 通知 OFF 時に予約済み通知をキャンセルする
- 文言と頻度制限を docs に残す
- 端末再起動後の通知再スケジュール（Android `RECEIVE_BOOT_COMPLETED`）

### できれば含める

- 初回勉強完了後に通知設定を提案する
- 初回オンボーディング完了後は「後で設定できる」程度に留める
- 通知タップ時にホームへ戻る
- 設定時刻を初期値 20:00 にする

## スコープ外

- サーバー Push 通知
- FCM / APNs token のサーバー管理
- 友達・ランキング・イベント通知
- #49 の目標未達通知の完全連携
- 「今日まだ勉強していない場合だけ通知」の完全なクロスプラットフォーム判定

## 要件

### 機能要件

1. ユーザーは設定画面で学習リマインダーを ON / OFF できる
2. ユーザーは通知時刻を 00:00〜23:59 の範囲で設定できる
3. 通知を ON にすると OS の通知許可を求める
4. 通知許可が拒否されている場合、アプリ内で状態を表示し、OS 設定へ誘導する
5. 通知が ON かつ許可済みの場合、毎日指定時刻に通知を出す
6. 通知を OFF にしたら予約済み通知をキャンセルする
7. 時刻変更時は既存予約をキャンセルし、新しい時刻で再予約する
8. アプリ再起動後も通知設定が保持される
9. 通知タップ時はアプリを開き、ホームまたは記録タブへ戻れる

### 非機能要件

- 初回起動直後に OS 通知許可ダイアログを出さない
- 通知文言は学習を責めず、戻りやすいトーンにする
- 個人情報や学習内容の詳細を通知本文に含めない
- タイムゾーンは端末の現在設定に従う
- MVP では Backend 依存を増やさない

## UX 設計

### 通知許可を求めるタイミング

1. 初回オンボーディングでは OS 許可を求めない
2. 初回勉強完了後の結果画面で「毎日リマインダーを設定する」CTA を出す
3. 設定画面からも常に変更できる
4. ユーザーが CTA または設定の ON を押した時点で OS 許可を求める

### 設定画面

設定画面に「学習リマインダー」セクションを追加する。

- リマインダー ON / OFF
- 通知時刻
- 通知許可状態
  - 未要求
  - 許可済み
  - 拒否済み
  - 不明 / OS 設定確認が必要
- 拒否済みの場合は「OS 設定で通知を許可してください」を表示

### 通知文言

MVP では日替わりランダムではなく、まず安全な固定文言または少数ローテーションにする。

| 種別 | タイトル | 本文 |
|------|----------|------|
| daily_default | 今日の勉強、少しだけ進めよう | 5分でも冒険は進みます。ホームから始めましょう。 |
| daily_evening | 今日の積み上げタイムです | ひと区切りだけ集中して、報酬を取りにいきましょう。 |
| streak_soft | 継続の流れをつなげよう | 今日の一歩を残しておくと、明日の自分が楽になります。 |

MVP の初期表示は `daily_default`。文言ローテーションは後続でよい。

## データ設計

shared に通知設定モデルを追加する。

```kotlin
data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val permissionStatus: ReminderPermissionStatus,
)

enum class ReminderPermissionStatus {
    NOT_DETERMINED,
    GRANTED,
    DENIED,
    UNKNOWN,
}
```

永続化は既存の `KeyValueStore` を使う。

| key | 例 | 用途 |
|-----|----|------|
| `study_reminder_enabled` | `true` | ON / OFF |
| `study_reminder_hour` | `20` | 通知時 |
| `study_reminder_minute` | `0` | 通知分 |
| `study_reminder_last_prompted_at` | `2026-06-07T10:00:00Z` | 許可依頼の出しすぎ防止 |

`permissionStatus` は OS の現在状態を都度問い合わせる。ただし Android 13+ では初回未要求と拒否済みを OS API だけで安定して区別できないため、以下の補助ルールで判定する:

| 状態 | 判定ルール |
|------|------------|
| `NOT_DETERMINED` | `study_reminder_permission_requested` が未設定かつ OS が未許可 |
| `GRANTED` | OS が許可 |
| `DENIED` | `study_reminder_permission_requested` が `true` かつ OS が未許可 |

| key | 例 | 用途 |
|-----|----|------|
| `study_reminder_permission_requested` | `true` | 初回リクエスト済みフラグ。未要求と拒否済みを区別する |

## アーキテクチャ

### shared commonMain

追加候補:

- `features/reminder/ReminderSettings.kt`
- `features/reminder/ReminderPermissionStatus.kt`
- `features/reminder/ReminderIntent.kt`
- `features/reminder/ReminderUiState.kt`
- `features/reminder/ReminderViewModel.kt`
- `domain/reminder/ReminderScheduler.kt`
- `data/local/ReminderSettingsStore.kt`

`ReminderScheduler` は expect / actual ではなく interface として common に置き、DI で platform 実装を差し込む。

```kotlin
// commonMain interface — OS 通知操作の抽象
interface ReminderScheduler {
    suspend fun permissionStatus(): ReminderPermissionStatus
    suspend fun scheduleDaily(hour: Int, minute: Int)
    suspend fun cancelAll()
    suspend fun openSystemNotificationSettings()
}
```

**権限リクエストの責務境界**: `requestPermission()` は common の `ReminderScheduler` に含めない。理由:
- Android の runtime permission は Activity / `ActivityResultLauncher` のライフサイクルに強く依存し、singleton や ViewModel が直接 Activity 参照を持つとリーク・未表示リスクがある
- 代わりに、権限リクエストは **platform UI 層（Android Compose / iOS SwiftUI）が責任を持つ**
- common には `permissionStatus()`（現在状態の取得）と `openSystemNotificationSettings()`（OS 設定誘導）のみを置く
- Android: `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` で Compose UI から OS ダイアログを表示し、結果を `KeyValueStore` の `study_reminder_permission_requested` に保存する
- iOS: `UNUserNotificationCenter.current().requestAuthorization()` は SwiftUI の `.onAppear` またはユーザー操作契機で呼ぶ

### Android

実装候補:

- `reminder/AndroidReminderScheduler.kt`
- `AndroidManifest.xml`
  - Android 13+ の `POST_NOTIFICATIONS`
  - reboot 後再登録を入れるなら `RECEIVE_BOOT_COMPLETED`
- `MainActivity` または platform DI で scheduler 登録

MVP の通知方式:

- `AlarmManager` + `BroadcastReceiver` + `NotificationManager`
- channel id: `study_reminder`
- request code は固定
- 端末再起動後は `RECEIVE_BOOT_COMPLETED` + `BootReceiver` で再スケジュールする（MVP 必須）

Android 注意点:

- Android 13+ は runtime permission が必要
- exact alarm はストア審査上の説明が重くなりがちなので、MVP は exact alarm 前提にしない
- Doze により数分ずれることは許容する

### iOS

実装候補:

- Swift 側で `UNUserNotificationCenter` を使う
- Kotlin shared から直接 iOS notification API を呼ぶより、Swift の adapter を用意して Koin / bridge で呼ぶ
- `UNCalendarNotificationTrigger(dateMatching:repeats:)` で毎日通知
- notification identifier: `study_reminder_daily`

iOS 注意点:

- 一度拒否された場合、再度 OS ダイアログは出せないため設定画面誘導にする
- 通知タップ時の遷移は初期実装ではホーム表示でよい

## 画面設計

### Android SettingsScreenDialog

- `SettingsViewModel` に reminder 状態を混ぜ込むか、`ReminderViewModel` を別に持つ
- 推奨は `ReminderViewModel` を別にし、Settings UI が両方を読む
- UI:
  - `Text("学習リマインダー")`
  - Switch
  - TimePicker または時/分の簡易 selector
  - Permission status text
  - OS 設定へ移動ボタン

### iOS SettingsScreenView

- SwiftUI の `Toggle`
- `DatePicker(displayedComponents: .hourAndMinute)`
- `Button("通知設定を開く")`
- shared の `ReminderViewModel` を polling ではなく bridge で購読できると理想だが、既存設定画面と揃えてまず polling でもよい

## 実装手順

1. `docs/tasks/` に本設計を追加
2. shared に `ReminderSettingsStore` と `ReminderViewModel` を追加
3. Android `ReminderScheduler` 実装、permission / channel / local notification を追加
4. iOS `ReminderScheduler` 実装、UNUserNotificationCenter scheduling を追加
5. Android / iOS 設定画面に通知設定 UI を追加
6. 初回勉強完了後の通知設定 CTA を追加するか、別 Issue に分けるか判断
7. QA 観点を docs に追記

## QA / 検証

### Android

- Android 13+ で初回 ON 時に通知許可が出る
- 許可後、指定時刻に通知が届く
- 拒否後、設定画面に拒否状態が出る
- OFF にすると通知が届かない
- 時刻変更後、古い時刻では届かず新しい時刻で届く
- 端末再起動後も通知予約が復元される（BootReceiver 経由）

### iOS

- 初回 ON 時に通知許可が出る
- 許可後、指定時刻に通知が届く
- 拒否後、設定画面誘導になる
- OFF にすると pending notification request が消える
- アプリ再起動後も設定値が残る

### 共通

- 初回起動直後に通知許可が出ない
- 通知本文に個人情報が出ない
- 端末タイムゾーン変更後も次回予約が破綻しない
- release build で debug UI と混ざらない

## リスク

- Android / iOS の通知 API は platform 差が大きいため、shared に寄せすぎると実装が複雑になる
- exact alarm を使うと Android の権限・審査説明が重くなる
- iOS は拒否後に OS ダイアログを再表示できない
- 「今日まだ勉強していない場合だけ通知」は、local notification だけだと当日実績の判定と再予約が複雑になる

## 実装判断

MVP は **毎日指定時刻のローカル通知** までに絞る。
「今日まだ勉強していない場合だけ通知」「目標未達通知」「ストリーク切れ通知」は、#49 の目標機能や記録データの安定後に拡張する。
