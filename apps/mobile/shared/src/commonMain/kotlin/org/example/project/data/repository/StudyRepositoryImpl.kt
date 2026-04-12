package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.StudyGateway
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudySession
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class StudyRepositoryImpl(
    private val gateway: StudyGateway,
    private val userRepository: UserRepository
) : StudyRepository {

    override suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String?
    ): StudyCompleteResult {
        val userId = UserSessionStore.requireUserId()
        val request = StudyCompleteRequest(
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            userCharacterId = userCharacterId
        )
        val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
        userRepository.updateCachedUser(result.updatedUser)
        return result
    }

    override suspend fun getSessionHistory(limit: Int, offset: Int): List<StudySession> {
        return try {
            gateway.listSessions(limit, offset).getOrThrow()
                .sessions.map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun savePendingSession(session: StudySession) {
        // TODO: SQLDelight ローカルDB に pending_study_sessions として保存
    }

    override suspend fun syncPendingSessions() {
        // TODO: ローカルの pending セッションを順次 completeSession で送信
    }
}
