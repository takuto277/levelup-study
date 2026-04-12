package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.RewardType
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudyReward
import org.example.project.domain.model.StudySession

// ── Response ────────────────────────────────────

@Serializable
data class StudySessionResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    val category: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class StudyRewardResponse(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("reward_type") val rewardType: String,
    val amount: Int,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class StudyCompleteResponse(
    @SerialName("session_id") val sessionId: String,
    val rewards: List<StudyRewardResponse>,
    @SerialName("updated_user") val updatedUser: UserResponse
)

@Serializable
data class StudySessionListResponse(
    val sessions: List<StudySessionResponse>
)

// ── Request ─────────────────────────────────────

@Serializable
data class StudyCompleteRequest(
    val category: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("is_completed") val isCompleted: Boolean,
    /** 冒険に出した所持キャラID（省略時はサーバーでパーティ先頭） */
    @SerialName("user_character_id") val userCharacterId: String? = null,
    @SerialName("defeat_normal_count") val defeatNormalCount: Int = 0,
    @SerialName("defeat_boss_count") val defeatBossCount: Int = 0,
    /** ダンジョン難易度による経験値倍率。1.0 で等倍。 */
    @SerialName("difficulty_multiplier") val difficultyMultiplier: Double = 1.0
)

// ── Mapper ──────────────────────────────────────

fun StudySessionResponse.toDomain(): StudySession = StudySession(
    id = id,
    userId = userId,
    category = category,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSeconds = durationSeconds,
    isCompleted = isCompleted,
    createdAt = createdAt
)

fun StudyRewardResponse.toDomain(): StudyReward = StudyReward(
    id = id,
    sessionId = sessionId,
    rewardType = runCatching { RewardType.valueOf(rewardType.uppercase()) }
        .getOrDefault(RewardType.STONES),
    amount = amount,
    itemId = itemId,
    createdAt = createdAt
)

fun StudyCompleteResponse.toDomain(): StudyCompleteResult = StudyCompleteResult(
    sessionId = sessionId,
    rewards = rewards.map { it.toDomain() },
    updatedUser = updatedUser.toDomain()
)
