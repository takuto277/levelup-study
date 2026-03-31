package org.example.project.features.analytics

import org.example.project.domain.model.StudySession

/**
 * 記録（Analytics）画面の UI 状態
 */
data class AnalyticsUiState(
    val totalStudySeconds: Long = 0,
    val recentSessions: List<StudySession> = emptyList(),
    val dailyStudySeconds: Map<String, Long> = emptyMap(),
    val weeklyStudySeconds: Long = 0,
    val monthlyStudySeconds: Long = 0,
    val streakDays: Int = 0,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val formattedTotalTime: String
        get() {
            val hours = totalStudySeconds / 3600
            val minutes = (totalStudySeconds % 3600) / 60
            return "${hours}h ${minutes}m"
        }
}

/** 表示期間 */
enum class AnalyticsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
