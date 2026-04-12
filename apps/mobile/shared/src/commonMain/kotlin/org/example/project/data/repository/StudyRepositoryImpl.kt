package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.core.network.isDeviceOnline
import org.example.project.core.session.UserSessionStore
import org.example.project.data.local.PendingStudyQueueStore
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.StudyGateway
import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudySession
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class StudyRepositoryImpl(
    private val gateway: StudyGateway,
    private val userRepository: UserRepository,
    private val pendingQueue: PendingStudyQueueStore = PendingStudyQueueStore()
) : StudyRepository {

    private fun pendingToSession(p: PendingStudyCompletion, userId: String): StudySession =
        StudySession(
            id = "local-${p.localId}",
            userId = userId,
            category = p.category,
            startedAt = p.startedAt,
            endedAt = p.endedAt,
            durationSeconds = p.durationSeconds,
            isCompleted = p.isCompleted,
            createdAt = p.endedAt,
            isPendingLocal = true
        )

    override suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String?,
        defeatNormalCount: Int,
        defeatBossCount: Int,
        difficultyMultiplier: Double
    ): StudyCompleteResult {
        val userId = UserSessionStore.requireUserId()
        val request = StudyCompleteRequest(
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            userCharacterId = userCharacterId,
            defeatNormalCount = defeatNormalCount,
            defeatBossCount = defeatBossCount,
            difficultyMultiplier = difficultyMultiplier
        )
        val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
        userRepository.updateCachedUser(result.updatedUser)
        return result
    }

    override suspend fun getSessionHistory(limit: Int, offset: Int): List<StudySession> {
        val userId = runCatching { UserSessionStore.requireUserId() }.getOrNull().orEmpty()
        val pending = if (userId.isNotBlank()) {
            pendingQueue.readAll().map { pendingToSession(it, userId) }
        } else {
            emptyList()
        }
        val remote = try {
            gateway.listSessions(limit = 500, offset = 0).getOrThrow()
                .sessions.map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        return (pending + remote)
            .distinctBy { it.id }
            .sortedByDescending { it.endedAt }
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceAtLeast(0))
    }

    override suspend fun savePendingCompletion(pending: PendingStudyCompletion) {
        pendingQueue.append(pending)
    }

    override suspend fun syncPendingSessions() {
        if (!isDeviceOnline()) return
        val userId = runCatching { UserSessionStore.requireUserId() }.getOrNull() ?: return
        val queue = pendingQueue.readAll()
        if (queue.isEmpty()) return
        for (p in queue) {
            try {
                val request = StudyCompleteRequest(
                    category = p.category,
                    startedAt = p.startedAt,
                    endedAt = p.endedAt,
                    durationSeconds = p.durationSeconds,
                    isCompleted = p.isCompleted,
                    userCharacterId = p.userCharacterId,
                    defeatNormalCount = p.defeatNormalCount,
                    defeatBossCount = p.defeatBossCount,
                    difficultyMultiplier = p.difficultyMultiplier
                )
                val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
                userRepository.updateCachedUser(result.updatedUser)
                pendingQueue.remove(p.localId)
            } catch (_: Exception) {
                break
            }
        }
    }
}
