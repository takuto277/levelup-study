package org.example.project.features.study

/**
 * 勉強クエスト画面のユーザーアクション
 *
 * View → ViewModel への入力はすべて Intent 経由。
 */
sealed interface StudyQuestIntent {
    /** クエスト開始（勉強時間を指定） */
    data class StartQuest(val studyMinutes: Int) : StudyQuestIntent

    /** 一時停止 / 再開のトグル */
    data object TogglePause : StudyQuestIntent

    /** セッション終了（結果画面へ） */
    data object FinishSession : StudyQuestIntent

    /** 次のセッションへ（勉強→休憩 or 休憩→勉強） */
    data object NextSession : StudyQuestIntent

    /** クエストを中止してホームに戻る */
    data object StopQuest : StudyQuestIntent
}
