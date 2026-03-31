package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.StudyGateway
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudySession
import org.example.project.domain.repository.StudyRepository

class StudyRepositoryImpl(
    private val gateway: StudyGateway
) : StudyRepository {

    override suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean
    ): StudyCompleteResult {
        val request = StudyCompleteRequest(
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted
        )
        return gateway.completeSession(request).getOrThrow().toDomain()
    }

    override suspend fun getSessionHistory(limit: Int, offset: Int): List<StudySession> {
        return gateway.getSessions(limit, offset).getOrThrow()
            .sessions.map { it.toDomain() }
    }

    override suspend fun savePendingSession(session: StudySession) {
        // TODO: SQLDelight ローカルDB に pending_study_sessions として保存
    }

    override suspend fun syncPendingSessions() {
        // TODO: ローカルの pending セッションを順次 completeSession で送信
        // 成功したら pending から削除、3回失敗で failed マーク
    }
}
