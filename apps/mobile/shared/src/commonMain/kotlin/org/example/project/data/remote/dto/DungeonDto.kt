package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.DungeonMonster
import org.example.project.domain.model.DungeonProgress
import org.example.project.domain.model.DungeonStage
import org.example.project.domain.model.DungeonStageEnemy
import org.example.project.domain.model.MasterDungeon
import org.example.project.domain.model.StageDrop

// ── Response ────────────────────────────────────

@Serializable
data class MasterDungeonResponse(
    val id: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("unlock_condition") val unlockCondition: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_active") val isActive: Boolean,
    val stages: List<DungeonStageResponse> = emptyList(),
)

/** GET /master/dungeons/{id} — ダンジョン詳細（ステージ含む） */
@Serializable
data class MasterDungeonDetailResponse(
    val id: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("unlock_condition") val unlockCondition: String? = null,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    val stages: List<DungeonStageResponse> = emptyList(),
)

@Serializable
data class MasterDungeonListResponse(
    val dungeons: List<MasterDungeonResponse>
)

@Serializable
data class DungeonMonsterResponse(
    val id: String,
    val slug: String,
    val name: String,
    val emoji: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class DungeonStageEnemyResponse(
    @SerialName("stage_id") val stageId: String? = null,
    @SerialName("monster_id") val monsterId: String? = null,
    @SerialName("sort_order") val sortOrder: Int,
    val count: Int,
    val monster: DungeonMonsterResponse? = null
)

@Serializable
data class DungeonStageResponse(
    val id: String,
    @SerialName("dungeon_id") val dungeonId: String,
    @SerialName("stage_number") val stageNumber: Int,
    @SerialName("recommended_power") val recommendedPower: Int,
    @SerialName("enemy_composition") val enemyComposition: String? = null,
    @SerialName("drop_table") val dropTable: List<StageDropEntryResponse> = emptyList(),
    val enemies: List<DungeonStageEnemyResponse> = emptyList()
)

@Serializable
data class StageDropEntryResponse(
    @SerialName("item_type") val itemType: String,
    val amount: Int,
    val rate: Double = 1.0,
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
    isActive = isActive,
    stages = stages.map { it.toDomain() },
)

fun DungeonMonsterResponse.toDomain(): DungeonMonster = DungeonMonster(
    id = id,
    slug = slug,
    name = name,
    emoji = emoji,
    hp = hp,
    atk = atk,
    def = def,
    imageUrl = imageUrl,
    isActive = isActive
)

fun DungeonStageResponse.toDomain(): DungeonStage = DungeonStage(
    id = id,
    dungeonId = dungeonId,
    stageNumber = stageNumber,
    recommendedPower = recommendedPower,
    enemyComposition = enemyComposition ?: "[]",
    drops = dropTable.map { StageDrop(it.itemType, it.amount, it.rate) },
    enemies = enemies.mapNotNull { row ->
        val m = row.monster ?: return@mapNotNull null
        DungeonStageEnemy(
            sortOrder = row.sortOrder,
            count = row.count,
            monster = m.toDomain()
        )
    }
)

fun DungeonProgressResponse.toDomain(): DungeonProgress = DungeonProgress(
    id = id,
    userId = userId,
    dungeonId = dungeonId,
    currentStage = currentStage,
    maxClearedStage = maxClearedStage,
    updatedAt = updatedAt
)
