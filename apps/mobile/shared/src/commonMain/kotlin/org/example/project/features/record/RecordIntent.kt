package org.example.project.features.record

/**
 * 記録画面のユーザー操作
 */
sealed interface RecordIntent {
    /** 表示期間を切り替え（今日 / 週間 / 月間） */
    data class SelectPeriod(val period: RecordPeriod) : RecordIntent

    /** ジャンルフィルターを選択（null で全ジャンル表示） */
    data class SelectGenre(val genre: GenreInfo?) : RecordIntent

    /** データをリフレッシュ */
    data object Refresh : RecordIntent
}
