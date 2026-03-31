package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.User

// ── Response ────────────────────────────────────

@Serializable
data class UserResponse(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("total_study_seconds") val totalStudySeconds: Long,
    val stones: Int,
    val gold: Int,
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
    @SerialName("display_name") val displayName: String
)

// ── Mapper ──────────────────────────────────────

fun UserResponse.toDomain(): User = User(
    id = id,
    displayName = displayName,
    totalStudySeconds = totalStudySeconds,
    stones = stones,
    gold = gold,
    createdAt = createdAt,
    updatedAt = updatedAt
)
