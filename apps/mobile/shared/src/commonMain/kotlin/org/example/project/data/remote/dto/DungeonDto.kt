package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.DungeonProgress
import org.example.project.domain.model.DungeonStage
import org.example.project.domain.model.MasterDungeon

// ── Response ────────────────────────────────────

@Serializable
data class MasterDungeonResponse(
    val id: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("unlock_condition") val unlockCondition: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class MasterDungeonListResponse(
    val dungeons: List<MasterDungeonResponse>
)

@Serializable
data class DungeonStageResponse(
    val id: String,
    @SerialName("dungeon_id") val dungeonId: String,
    @SerialName("stage_number") val stageNumber: Int,
    @SerialName("recommended_power") val recommendedPower: Int,
    @SerialName("enemy_composition") val enemyComposition: String,
    @SerialName("drop_table") val dropTable: String
)

@Serializable
data class DungeonStageListResponse(
    val stages: List<DungeonStageResponse>
)

@Serializable
data class DungeonProgressResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("dungeon_id") val dungeonId: String,
    @SerialName("current_stage") val currentStage: Int,
    @SerialName("max_cleared_stage") val maxClearedStage: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class DungeonProgressListResponse(
    val progress: List<DungeonProgressResponse>
)

// ── Mapper ──────────────────────────────────────

fun MasterDungeonResponse.toDomain(): MasterDungeon = MasterDungeon(
    id = id,
    name = name,
    sortOrder = sortOrder,
    unlockCondition = unlockCondition,
    imageUrl = imageUrl,
    isActive = isActive
)

fun DungeonStageResponse.toDomain(): DungeonStage = DungeonStage(
    id = id,
    dungeonId = dungeonId,
    stageNumber = stageNumber,
    recommendedPower = recommendedPower,
    enemyComposition = enemyComposition,
    dropTable = dropTable
)

fun DungeonProgressResponse.toDomain(): DungeonProgress = DungeonProgress(
    id = id,
    userId = userId,
    dungeonId = dungeonId,
    currentStage = currentStage,
    maxClearedStage = maxClearedStage,
    updatedAt = updatedAt
)
