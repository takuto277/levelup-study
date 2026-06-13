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
import org.example.project.domain.model.SyncStatus
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
        difficultyMultiplier: Double,
        clientSessionId: String?
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
            difficultyMultiplier = difficultyMultiplier,
            clientSessionId = clientSessionId
        )
        val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
            .copy(clientSessionId = clientSessionId)
        result.updatedUser?.let { userRepository.updateCachedUser(it) }
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
            if (p.syncStatus == SyncStatus.FAILED) continue
            pendingQueue.updateStatus(p.localId, SyncStatus.SYNCING)

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
                    difficultyMultiplier = p.difficultyMultiplier,
                    clientSessionId = p.localId
                )
                val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
                result.updatedUser?.let { userRepository.updateCachedUser(it) }
                pendingQueue.remove(p.localId)
            } catch (e: Exception) {
                val newRetryCount = p.retryCount + 1
                if (newRetryCount >= SyncStatus.MAX_RETRIES) {
                    pendingQueue.updateStatus(p.localId, SyncStatus.FAILED, e.message)
                } else {
                    pendingQueue.updateStatus(p.localId, SyncStatus.PENDING, e.message)
                }
            }
        }
    }

    override suspend fun getPendingCount(): Int = pendingQueue.count()

    override suspend fun hasFailedPendingSessions(): Boolean = pendingQueue.hasFailedItems()

    override suspend fun retryFailedSessions() {
        if (!isDeviceOnline()) return
        val userId = runCatching { UserSessionStore.requireUserId() }.getOrNull() ?: return
        val items = pendingQueue.readAll().filter { it.syncStatus == SyncStatus.FAILED }

        for (p in items) {
            pendingQueue.updateStatus(p.localId, SyncStatus.SYNCING)

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
                    difficultyMultiplier = p.difficultyMultiplier,
                    clientSessionId = p.localId
                )
                val result = gateway.completeSession(userId, request).getOrThrow().toDomain()
                result.updatedUser?.let { userRepository.updateCachedUser(it) }
                pendingQueue.remove(p.localId)
            } catch (e: Exception) {
                pendingQueue.updateStatus(p.localId, SyncStatus.FAILED, e.message)
            }
        }
    }
}
