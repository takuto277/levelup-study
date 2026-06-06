# Issue #55 段階的チュートリアルと再表示導線 実行計画・設計

## 対象

- Issue: #55 段階的チュートリアルと再表示導線の設計・実装
- URL: https://github.com/takuto277/levelup-study/issues/55
- 現在ステータス: `status:要件定義済み`
- この設計 PR での更新: `status:設計済み`

## 背景

#50 で初回オンボーディングが追加され、初回起動時にコアループを説明できる状態になった。一方で、ホーム、学習タイマー、学習完了後の報酬、冒険、召喚、編成、記録などは利用文脈が異なるため、初回オンボーディングだけで全機能を説明すると情報量が多くなる。

#55 では、機能を初めて利用するタイミングで短いチュートリアルを段階的に出す。各トピックは一度だけ表示し、スキップ、あとで見る、完了、設定からの再表示/リセットを提供する。

## 現状調査

- Android の初回オンボーディングは `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/App.kt` で `KeyValueStore.isOnboardingDone()` を見て `OnboardingScreen` を出し、完了時に `setOnboardingDone()` を呼んでいる。
- iOS の初回オンボーディングは `apps/mobile/iosApp/iosApp/Views/MainTabView.swift` で `@AppStorage("onboarding_done")` を見て `OnboardingView` を出している。
- 共通キーは `apps/mobile/shared/src/commonMain/kotlin/org/example/project/core/storage/KeyValueStore.kt` の `ONBOARDING_DONE_KEY = "onboarding_done"` で、Android と iOS のキー名が揃っている。
- Android 設定は `SettingsScreenDialog` から「オンボーディングを再表示」を実行でき、`resetOnboarding()` によって既存フローを再表示している。
- iOS 設定は `SettingsScreenView` から `UserDefaults.standard.set(false, forKey: "onboarding_done")` を実行して既存フローを再表示している。
- Android のホームは `HomeScreenView` がタブ状態を持ち、`0:冒険 / 1:編成 / 2:ホーム / 3:召喚 / 4:記録` を切り替えている。
- iOS のメインタブは `MainTabView.Tab` が `quest / party / home / gacha / analytics` を切り替えている。
- 学習中 UI は Android/iOS とも `StudyQuestScreenView` に集約されている。Android では休憩状態で `BreakAfterStudySummaryCard` が表示され、直前の学習結果を出せる。
- 端末ローカル永続化は `KeyValueStore` の文字列保存が基本で、`PendingStudyQueueStore` は `kotlinx.serialization` と JSON 配列を使っている。チュートリアル進捗も同じ方針で共通実装できる。

## ゴール

- 初回オンボーディングを `core_loop_onboarding` トピックとして扱い、既存 `onboarding_done` と互換させる。
- 各トピックは原則 1 回だけ自動表示し、ユーザー操作で再表示できる。
- 学習タイマー中は集中を妨げない非モーダル表示にする。
- Android Compose と iOS SwiftUI で見た目は各 OS に寄せつつ、トピック ID、表示条件、進捗状態は共有する。
- 将来の #49 `goal_bonus` と #51 `pending_sync` 用のトピック ID は予約するが、該当機能の実装までは自動表示しない。

## 非ゴール

- 音声/動画チュートリアルは作らない。
- チュートリアル進捗のサーバー同期は行わない。
- #49 の目標ボーナスや #51 の未同期キュー UI 本体は実装しない。
- 今回の設計 PR ではアプリコードの実装は行わない。

## トピック一覧

| ID | 表示タイミング | 表示形式 | 完了条件 |
| --- | --- | --- | --- |
| `core_loop_onboarding` | 初回起動、または設定から再表示 | 既存オンボーディング画面 | 最終ページ完了またはスキップ |
| `home_start_study` | オンボーディング完了後、最初にホームを表示 | ホーム内の開始ボタン周辺のコーチマーク | 開始ボタン押下、閉じる、あとで |
| `study_timer` | 初めて学習を開始した直後 | タイマー画面内の非モーダルヒント | 一定時間表示後、閉じる、学習終了 |
| `study_reward` | 初めて学習完了後の休憩/報酬サマリー表示時 | 報酬サマリー内のインライン案内 | 休憩から続行、終了、閉じる |
| `quest_select` | 初めて冒険タブを開いた時 | ダンジョン選択リストのコーチマーク | ダンジョン詳細を開く、閉じる |
| `gacha_first_pull` | 初めて召喚タブを開き、召喚可能な石がある時 | 召喚ボタン周辺のコーチマーク | 召喚実行、閉じる |
| `party_setup` | 初めて編成タブを開いた時、または初回召喚結果後 | 編成枠周辺のコーチマーク | 編成操作、閉じる |
| `record_review` | 1 回以上学習記録がある状態で初めて記録タブを開いた時 | 記録サマリー周辺のコーチマーク | 画面スクロール、閉じる |
| `goal_bonus` | #49 実装後、目標達成ボーナス対象時 | 予約 ID | #49 で定義 |
| `pending_sync` | #51 実装後、未同期データがある時 | 予約 ID | #51 で定義 |

## 共通モデル

`apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/tutorial/` を追加する。

```kotlin
enum class TutorialTopic(val id: String) {
    CoreLoopOnboarding("core_loop_onboarding"),
    HomeStartStudy("home_start_study"),
    StudyTimer("study_timer"),
    StudyReward("study_reward"),
    QuestSelect("quest_select"),
    GachaFirstPull("gacha_first_pull"),
    PartySetup("party_setup"),
    RecordReview("record_review"),
    GoalBonus("goal_bonus"),
    PendingSync("pending_sync"),
}

enum class TutorialTopicStatus {
    NotSeen,
    Shown,
    Completed,
    Skipped,
    Snoozed,
}

@Serializable
data class TutorialTopicProgress(
    val topicId: String,
    val status: TutorialTopicStatus = TutorialTopicStatus.NotSeen,
    val showCount: Int = 0,
    val firstShownAtEpochMs: Long? = null,
    val lastShownAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val skippedAtEpochMs: Long? = null,
    val snoozedUntilEpochMs: Long? = null,
)
```

`Snoozed` は「あとで」を表す。初期値は 24 時間後、または次回アプリ起動後のどちらか短い方で再判定する。自動表示は `Completed` と `Skipped` を除外し、設定からの再表示は状態に関係なく手動起動できる。

## 進捗ストア

`TutorialProgressStore` は `KeyValueStore` に JSON 配列として保存する。既存の `PendingStudyQueueStore` と同様に `Json { ignoreUnknownKeys = true; encodeDefaults = true }` を使う。

- 保存キー: `tutorial_progress_v1`
- 読み取り失敗時: 空リストとして扱い、アプリ起動を妨げない
- 個別更新: `upsert(topicProgress)`
- 一括リセット: `resetAll()`
- 個別リセット: `reset(topic)`
- 既存オンボーディング移行:
  - `onboarding_done == true` かつ `core_loop_onboarding` が未登録なら `Completed` として登録する
  - `onboarding_done` が未設定なら既存フロー通り初回オンボーディングを表示する
  - 設定からオンボーディング再表示した場合は `core_loop_onboarding` だけ `NotSeen` に戻し、完了後に `Completed` へ戻す

## 表示判定

`TutorialCoordinator` を共通層に置き、OS 側 UI は以下の戻り値だけを見る。

```kotlin
class TutorialCoordinator(
    private val store: TutorialProgressStore,
) {
    fun nextFor(event: TutorialTriggerEvent, context: TutorialContext): TutorialTopic?
    fun markShown(topic: TutorialTopic)
    fun complete(topic: TutorialTopic)
    fun skip(topic: TutorialTopic)
    fun snooze(topic: TutorialTopic)
    fun reset(topic: TutorialTopic)
    fun resetAll()
}
```

`TutorialTriggerEvent` は `AppStarted`, `OnboardingCompleted`, `HomeVisible`, `TabVisible`, `StudyStarted`, `StudyRewardVisible`, `GachaResultShown` などに分ける。`TutorialContext` は `hasStudyRecord`, `stones`, `isTimerRunning`, `currentTab`, `featureFlags` を持たせる。

表示優先度は 1 イベントにつき 1 トピックまでにする。連続表示を避けるため、別トピックを自動表示した直後は同一セッション中の次トピックを出さず、次のユーザー操作か次回起動で再判定する。

## UI 設計

### Android Compose

- `TutorialHost` を `App` または `HomeScreenView` 配下に置き、現在表示すべき `TutorialTopic` を監視する。
- ホーム、冒険、編成、召喚、記録はタブ表示時に `TutorialTriggerEvent.TabVisible` を送る。
- `home_start_study` は開始ボタン近くに小さなカード型コーチマークを重ねる。
- `study_timer` はタイマー画面上部または下部に非モーダルのヒントバーとして出す。タイマー停止、一時停止、ダイアログ表示はしない。
- `study_reward` は `BreakAfterStudySummaryCard` の下にインラインカードとして出す。
- 設定は「チュートリアル」セクションを追加し、「初回オンボーディングを再表示」「この画面のヒントを再表示」「すべてのチュートリアルをリセット」を置く。

### iOS SwiftUI

- `MainTabView` で `TutorialCoordinator` を保持し、タブ切り替えと初回オンボーディング完了をトリガーにする。
- `HomeScreenView`、`StudyQuestScreenView`、`QuestScreenView`、`GachaScreenView`、`PartyScreenView`、`AnalyticsScreenView` に必要なトピック表示状態を渡す。
- SwiftUI 側は `TutorialCoachMarkView` と `TutorialInlineCard` を OS ネイティブな見た目で実装する。
- 設定は Android と同じ意味の導線を追加し、`onboarding_done` の直接書き換えは共通ストア経由へ寄せる。

## コピー方針

トピックごとのタイトル、本文、主ボタン、補助ボタンを `TutorialCopyCatalog` として共通層に置く。OS 側で表示形式が違っても文言は共通にする。

| ID | タイトル案 | 本文案 |
| --- | --- | --- |
| `home_start_study` | まずは学習を開始 | 時間とジャンルを選んで開始すると、学習時間が冒険の力になります。 |
| `study_timer` | タイマー中はそのまま集中 | 画面は自動で進みます。必要な時だけ一時停止や終了を使ってください。 |
| `study_reward` | 学習結果を確認 | 集中時間や報酬はここで確認できます。続ける場合は休憩後に冒険へ戻れます。 |
| `quest_select` | 冒険先を選択 | 挑戦できるダンジョンを選ぶと、次の学習の舞台が変わります。 |
| `gacha_first_pull` | 報酬で召喚 | 集めた石で仲間や装備を増やせます。 |
| `party_setup` | 編成を整える | 手に入れた仲間や装備を編成して、次の冒険に備えます。 |
| `record_review` | 学習を振り返る | これまでの学習時間や傾向を確認できます。 |

## 実装手順

1. 共通層に `TutorialTopic`、`TutorialTopicProgress`、`TutorialProgressStore`、`TutorialCoordinator`、`TutorialCopyCatalog` を追加する。
2. `SharedModule` と `KoinHelper` へチュートリアル関連の取得関数を追加する。
3. 既存 `onboarding_done` を `core_loop_onboarding` に移行する処理を追加し、Android/iOS の初回オンボーディング表示判定を共通ストアへ寄せる。
4. Android の `App`、`HomeScreenView`、各タブ、`StudyQuestScreenView`、`SettingsScreenDialog` にトリガー送信と表示コンポーネントを追加する。
5. iOS の `MainTabView`、各画面、`SettingsScreenView` に同等のトリガー送信と表示コンポーネントを追加する。
6. `goal_bonus` と `pending_sync` は ID とコピーだけ予約し、feature flag または context 条件で自動表示しないようにする。
7. 共通ロジックの単体テストと Android/iOS の手動 QA を追加する。

## テスト計画

- `TutorialProgressStore`:
  - 空状態から読み出すと全トピック未表示として扱う。
  - JSON 破損時にクラッシュせず空状態へフォールバックする。
  - `markShown`、`complete`、`skip`、`snooze`、`reset`、`resetAll` が期待通り保存される。
- `TutorialCoordinator`:
  - 1 イベントで複数候補があっても 1 件だけ返す。
  - `Completed` と `Skipped` は自動表示しない。
  - `Snoozed` は期限前に表示しない。
  - `goal_bonus` と `pending_sync` は該当 feature flag が有効な時だけ候補になる。
- 手動 QA:
  - 初回起動で既存オンボーディングが出る。
  - スキップ/完了後、ホームで開始ヒントが出る。
  - 学習開始時、タイマーを止めずにヒントが出る。
  - 学習完了後、報酬サマリーにヒントが出る。
  - 各タブ初回訪問時に該当ヒントが出る。
  - アプリ再起動後、完了済みヒントは自動再表示されない。
  - 設定から個別再表示と全リセットができる。
  - Reduce Motion 有効時に強いアニメーションを避ける。

## 受け入れ条件

- Android/iOS で同じトピック ID と進捗状態を使っている。
- 各トピックが一度だけ自動表示され、設定から再表示/リセットできる。
- 学習タイマー中のチュートリアルが非モーダルで、タイマー進行を妨げない。
- 既存 `onboarding_done` 利用者が再オンボーディングされない。
- #49/#51 の予約トピックが、該当機能実装前に表示されない。

## リスクと対応

- OS ごとにオンボーディング保存経路が分岐しているため、実装時は最初に共通ストア移行を入れてから UI を接続する。
- タブ初回表示でヒントが連続表示される可能性があるため、`TutorialCoordinator` にセッション内クールダウンを持たせる。
- 学習タイマー画面は集中体験への影響が大きいため、`study_timer` は非モーダルかつ閉じやすい表示に限定する。
- iOS から KMP の共通ストアを直接扱うため、Swift で扱いやすい wrapper 関数を `KoinHelper` に追加する。
