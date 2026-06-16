package org.example.project.features.quest

import org.example.project.domain.local.LocalDungeonIds
import org.example.project.domain.model.DungeonCategory
import org.example.project.domain.model.DungeonDifficulty
import org.example.project.domain.model.DungeonProgress
import org.example.project.domain.model.DungeonStage
import org.example.project.domain.model.MasterDungeon
import org.example.project.domain.repository.DungeonRepository

/**
 * 冒険画面のユースケース
 * サーバーのダンジョンマスタ + 進行状況 + ステージ drop_table を統合して UI 用データに変換
 */
class QuestUseCase(
    private val dungeonRepository: DungeonRepository
) {
    suspend fun loadDungeons(): List<Dungeon> {
        return try {
            val masterDungeons = dungeonRepository.getDungeons()
            val progressList = try {
                dungeonRepository.getAllProgress()
            } catch (_: Exception) {
                emptyList()
            }
            val progressMap = progressList.associateBy { it.dungeonId }

            val serverDungeons = masterDungeons.map { md ->
                val progress = progressMap[md.id]
                val stages = md.stages.ifEmpty {
                    try {
                        dungeonRepository.getDungeonStages(md.id)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                toDungeon(md, stages, progress)
            }
            serverDungeons + listOf(LocalDungeons.trainingGround())
        } catch (_: Exception) {
            getDefaultDungeons()
        }
    }

    private fun toDungeon(
        md: MasterDungeon,
        stages: List<DungeonStage>,
        progress: DungeonProgress?,
    ): Dungeon {
        val cleared = progress?.maxClearedStage ?: 0
        val totalStages = stages.size.takeIf { it > 0 } ?: md.totalStages.takeIf { it > 0 } ?: 10
        val nextStage = stages.firstOrNull { it.stageNumber == cleared + 1 }
        val rewards = nextStage?.let { stageDropsToReward(it.drops) }
            ?: stages.lastOrNull()?.let { stageDropsToReward(it.drops) }
            ?: DungeonReward(gold = 0, exp = 0, gachaStones = 0)

        return Dungeon(
            id = md.id,
            name = md.name,
            description = md.description ?: "",
            difficulty = md.difficulty,
            category = md.category,
            totalStages = totalStages,
            clearedStages = cleared,
            recommendedMinutes = md.recommendedMinutes ?: 25,
            rewards = rewards,
            nextStageNumber = nextStage?.stageNumber,
            nextStageEnemySummary = nextStage?.let { stageEnemySummary(it.enemies) },
            iconEmoji = md.iconEmoji ?: "🏰",
            imageUrl = md.imageUrl,
            isFromServer = true,
            isLocked = md.unlockCondition != null && progress == null,
        )
    }

    companion object {
        /** ネットワークなし時など、同梱マスタだけで表示名を補う（サーバー専用 ID は null） */
        fun bundledDisplayNameForDungeonId(id: String): String? =
            getDefaultDungeons().firstOrNull { it.id == id }?.name

        /** 同梱マスタから fallback 用の画像 URL を補う */
        fun bundledImageUrlForDungeonId(id: String): String? =
            getDefaultDungeons().firstOrNull { it.id == id }?.imageUrl

        fun getDefaultDungeons(): List<Dungeon> = listOf(
            Dungeon(
                id = "forest_of_beginnings",
                name = "はじまりの森",
                description = "新米冒険者の修行場。穏やかな森で基礎を固めよう。",
                difficulty = DungeonDifficulty.BEGINNER,
                category = DungeonCategory.GENERAL,
                totalStages = 10,
                clearedStages = 0,
                recommendedMinutes = 25,
                rewards = DungeonReward(gold = 20, exp = 10, gachaStones = 0),
                nextStageNumber = 1,
                nextStageEnemySummary = null,
                iconEmoji = "🌲",
                imageUrl = "https://picsum.photos/seed/levelup-dungeon-forest/1200/675",
                isFromServer = true,
                isLocked = false,
            ),
            LocalDungeons.trainingGround(),
        )
    }
}
