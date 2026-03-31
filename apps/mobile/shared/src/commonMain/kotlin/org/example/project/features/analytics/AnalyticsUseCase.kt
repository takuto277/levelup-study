package org.example.project.features.analytics

import org.example.project.domain.model.StudySession
import org.example.project.domain.model.User
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

/**
 * 記録画面のユースケース
 * 勉強統計データの集約・期間別集計
 */
class AnalyticsUseCase(
    private val userRepository: UserRepository,
    private val studyRepository: StudyRepository
) {
    /** 記録画面データ */
    data class AnalyticsData(
        val user: User,
        val recentSessions: List<StudySession>
    )

    /** 記録画面に必要なデータを取得 */
    suspend fun loadAnalyticsData(): AnalyticsData {
        val user = userRepository.getCurrentUser()
        val sessions = studyRepository.getSessionHistory(limit = 50)
        return AnalyticsData(user = user, recentSessions = sessions)
    }

    /**
     * セッション一覧から日別の勉強秒数マップを生成
     * key: "YYYY-MM-DD", value: 秒数
     * TODO: kotlinx-datetime を使って正確な日付計算に置き換え
     */
    fun calculateDailyStudy(sessions: List<StudySession>): Map<String, Long> {
        return sessions.groupBy { session ->
            // startedAt の日付部分を抽出（ISO 8601 形式想定）
            session.startedAt.take(10)
        }.mapValues { (_, daySessions) ->
            daySessions.sumOf { it.durationSeconds.toLong() }
        }
    }
}
