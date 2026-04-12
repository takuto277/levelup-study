package org.example.project.features.quest

import org.example.project.domain.model.DungeonCategory
import org.example.project.domain.model.DungeonDifficulty
import org.example.project.domain.repository.DungeonRepository

/**
 * 冒険画面のユースケース
 * サーバーのダンジョンマスタ + 進行状況を統合してUI用データに変換
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
                val totalStages = md.totalStages.takeIf { it > 0 }
                    ?: 10 // stages count from server, fallback
                Dungeon(
                    id = md.id,
                    name = md.name,
                    description = md.description ?: "",
                    difficulty = md.difficulty,
                    category = md.category,
                    totalStages = totalStages,
                    clearedStages = progress?.maxClearedStage ?: 0,
                    recommendedMinutes = md.recommendedMinutes ?: 25,
                    rewards = DungeonReward(gold = 100, exp = 50, gachaStones = 5),
                    iconEmoji = md.iconEmoji ?: "🏰",
                    imageUrl = md.imageUrl,
                    isFromServer = true,
                    isLocked = md.unlockCondition != null && (progress == null)
                )
            }
            serverDungeons + listOf(LocalDungeons.trainingGround())
        } catch (_: Exception) {
            getDefaultDungeons()
        }
    }

    companion object {
        fun getDefaultDungeons(): List<Dungeon> = listOf(
            Dungeon(
                id = "forest_of_beginnings", name = "はじまりの森",
                description = "新米冒険者の修行場。穏やかな森で基礎を固めよう。",
                difficulty = DungeonDifficulty.BEGINNER, category = DungeonCategory.GENERAL,
                totalStages = 10, clearedStages = 0, recommendedMinutes = 25,
                rewards = DungeonReward(gold = 100, exp = 50, gachaStones = 5),
                iconEmoji = "🌲",
                imageUrl = "https://picsum.photos/seed/levelup-dungeon-forest/1200/675",
                isFromServer = true,
                isLocked = false
            ),
            LocalDungeons.trainingGround()
        )
    }
}
