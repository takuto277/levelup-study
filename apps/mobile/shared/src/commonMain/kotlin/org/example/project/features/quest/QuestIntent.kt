package org.example.project.features.quest

/**
 * 冒険（Quest）画面のユーザー操作
 */
sealed interface QuestIntent {
    /** ダンジョン一覧をリフレッシュ */
    data object RefreshDungeons : QuestIntent

    /** ダンジョンを選択 */
    data class SelectDungeon(val dungeonId: String) : QuestIntent

    /** ダンジョン詳細を閉じる */
    data object DismissDetail : QuestIntent
}
