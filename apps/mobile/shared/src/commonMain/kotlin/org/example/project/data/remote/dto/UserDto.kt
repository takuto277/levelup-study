package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.User

// ── Response ────────────────────────────────────

@Serializable
data class UserResponse(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val level: Int = 1,
    @SerialName("current_xp") val currentXp: Int = 0,
    @SerialName("total_study_seconds") val totalStudySeconds: Long,
    val stones: Int,
    val gold: Int,
    @SerialName("selected_dungeon_id") val selectedDungeonId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

// ── Request ─────────────────────────────────────

@Serializable
data class CreateUserRequest(
    @SerialName("display_name") val displayName: String
)

@Serializable
data class UpdateUserRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("selected_dungeon_id") val selectedDungeonId: String? = null
)

// ── Mapper ──────────────────────────────────────

fun UserResponse.toDomain(): User = User(
    id = id,
    displayName = displayName,
    level = level,
    currentXp = currentXp,
    totalStudySeconds = totalStudySeconds,
    stones = stones,
    gold = gold,
    selectedDungeonId = selectedDungeonId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
