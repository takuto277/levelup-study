package org.example.project.domain.model

/**
 * ダンジョンマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
data class MasterDungeon(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val unlockCondition: String?,
    val imageUrl: String,
    val isActive: Boolean
)

/**
 * ダンジョンステージマスタ
 */
data class DungeonStage(
    val id: String,
    val dungeonId: String,
    val stageNumber: Int,
    val recommendedPower: Int,
    val enemyComposition: String,
    val dropTable: String
)

/**
 * ダンジョン進行状況
 * Source of Truth: サーバー
 */
data class DungeonProgress(
    val id: String,
    val userId: String,
    val dungeonId: String,
    val currentStage: Int,
    val maxClearedStage: Int,
    val updatedAt: String
)
