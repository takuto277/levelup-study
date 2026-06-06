package org.example.project.domain.repository

import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudySession

interface StudyRepository {

    suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String? = null,
        defeatNormalCount: Int = 0,
        defeatBossCount: Int = 0,
        difficultyMultiplier: Double = 1.0
    ): StudyCompleteResult

    suspend fun getSessionHistory(limit: Int = 20, offset: Int = 0): List<StudySession>

    suspend fun savePendingCompletion(pending: PendingStudyCompletion)

    suspend fun syncPendingSessions()

    suspend fun getPendingCount(): Int

    suspend fun hasFailedPendingSessions(): Boolean

    /** 失敗済みの未同期セッションを手動リトライ（既存の pending も合わせて送信） */
    suspend fun retryFailedSessions()
}
