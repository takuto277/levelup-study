package org.example.project.features.quest

import org.example.project.domain.local.LocalDungeonIds
import org.example.project.domain.model.DungeonCategory
import org.example.project.domain.model.DungeonDifficulty

/** アプリ同梱のダンジョン定義（サーバー API のマスタとは別）。 */
object LocalDungeons {

    fun trainingGround(): Dungeon = Dungeon(
        id = LocalDungeonIds.TRAINING_GROUND,
        name = LocalDungeonIds.TRAINING_GROUND_NAME,
        description = "",
        difficulty = DungeonDifficulty.BEGINNER,
        category = DungeonCategory.GENERAL,
        totalStages = 1,
        clearedStages = 0,
        recommendedMinutes = 25,
        rewards = DungeonReward(gold = 0, exp = 0, gachaStones = 0),
        iconEmoji = "🛢",
        imageUrl = "",
        isFromServer = false,
        isLocked = false
    )
}
