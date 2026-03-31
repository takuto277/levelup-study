package org.example.project.features.analytics

/**
 * 記録（Analytics）画面のユーザー操作
 */
sealed interface AnalyticsIntent {
    /** データをリフレッシュ */
    data object Refresh : AnalyticsIntent

    /** 表示期間を切り替え */
    data class ChangePeriod(val period: AnalyticsPeriod) : AnalyticsIntent
}
