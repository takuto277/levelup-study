package org.example.project.features.study

/**
 * 勉強クエスト画面のユーザーアクション
 *
 * View → ViewModel への入力はすべて Intent 経由。
 */
sealed interface StudyQuestIntent {
    data class StartQuest(
        val studyMinutes: Int,
        val genreId: String? = null,
        val dungeonName: String? = null,
        val isTrainingGround: Boolean = false,
        val dungeonImageUrl: String? = null
    ) : StudyQuestIntent

    /** 一時停止 / 再開のトグル */
    data object TogglePause : StudyQuestIntent

    /** セッション終了（経過時間分の報酬を獲得して結果画面へ） */
    data object EndQuest : StudyQuestIntent

    /** 次のセッションへ（勉強→休憩 or 休憩→勉強） */
    data object NextSession : StudyQuestIntent

    /** クエストを中止してホームに戻る（報酬なし） */
    data object StopQuest : StudyQuestIntent
}
