package org.example.project.features.study

/**
 * 勉強タイマー画面のユーザー操作
 */
sealed interface StudyIntent {
    /** 勉強カテゴリを設定 */
    data class SetCategory(val category: String?) : StudyIntent

    /** 目標時間を設定（秒） */
    data class SetTargetSeconds(val seconds: Int) : StudyIntent

    /** タイマー開始 */
    data object StartTimer : StudyIntent

    /** 一時停止 / 再開 トグル */
    data object TogglePause : StudyIntent

    /** セッション終了（タイマーを止めてサーバーに送信） */
    data object FinishSession : StudyIntent

    /** タイマーを完全にリセット */
    data object ResetTimer : StudyIntent

    /** 次のセッションへ（勉強 → 休憩 → 勉強のサイクル） */
    data object NextSession : StudyIntent

    /** リザルト画面を閉じる */
    data object DismissResult : StudyIntent
}
