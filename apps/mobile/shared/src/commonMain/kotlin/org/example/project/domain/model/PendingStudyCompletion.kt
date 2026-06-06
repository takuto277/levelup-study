package org.example.project.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PendingStudyCompletion(
    val localId: String,
    val category: String? = null,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Int,
    val isCompleted: Boolean,
    val userCharacterId: String? = null,
    val defeatNormalCount: Int = 0,
    val defeatBossCount: Int = 0,
    val difficultyMultiplier: Double = 1.0,
    val isTrainingGround: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: String? = null,
)

object SyncStatus {
    const val PENDING = "pending"
    const val SYNCING = "syncing"
    const val FAILED = "failed"
    const val MAX_RETRIES = 3
}
