package org.example.project.features.quest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.example.project.domain.model.DungeonMonster
import org.example.project.domain.model.DungeonStageEnemy
import org.example.project.domain.model.StageDrop

class StageDropTableTest {

    @Test
    fun guaranteedDropsAreSummedAndAliasesAreAccepted() {
        val reward = stageDropsToReward(
            listOf(
                StageDrop(itemType = "gold", amount = 100),
                StageDrop(itemType = "xp", amount = 20),
                StageDrop(itemType = "exp", amount = 5),
                StageDrop(itemType = "stone", amount = 3),
                StageDrop(itemType = "gacha_stones", amount = 2),
            )
        )

        assertEquals(100, reward.gold)
        assertEquals(25, reward.exp)
        assertEquals(5, reward.gachaStones)
        assertNull(reward.bonusItemName)
        assertEquals(0f, reward.bonusItemDropRate)
    }

    @Test
    fun probabilisticStoneDropsBecomeBonusDisplay() {
        val reward = stageDropsToReward(
            listOf(
                StageDrop(itemType = "stones", amount = 1, rate = 0.5),
                StageDrop(itemType = "stones", amount = 3, rate = 0.25),
            )
        )

        assertEquals(0, reward.gachaStones)
        assertEquals("召喚石 +3", reward.bonusItemName)
        assertEquals(0.5f, reward.bonusItemDropRate)
    }

    @Test
    fun nonGuaranteedGoldAndExpAreIgnoredForFixedReward() {
        val reward = stageDropsToReward(
            listOf(
                StageDrop(itemType = "gold", amount = 999, rate = 0.99),
                StageDrop(itemType = "exp", amount = 999, rate = 0.1),
            )
        )

        assertEquals(0, reward.gold)
        assertEquals(0, reward.exp)
    }

    @Test
    fun enemySummarySortsBySortOrderAndFallsBackToEmojiThenQuestionMark() {
        val summary = stageEnemySummary(
            listOf(
                DungeonStageEnemy(sortOrder = 20, count = 1, monster = monster(name = "", emoji = "🧊")),
                DungeonStageEnemy(sortOrder = 10, count = 2, monster = monster(name = "スライム", emoji = "🫧")),
                DungeonStageEnemy(sortOrder = 30, count = 1, monster = monster(name = "", emoji = "")),
            )
        )

        assertEquals("スライム×2、🧊、？", summary)
    }

    @Test
    fun enemySummaryReturnsPlaceholderForEmptyList() {
        assertEquals("敵情報なし", stageEnemySummary(emptyList()))
    }

    private fun monster(name: String, emoji: String): DungeonMonster = DungeonMonster(
        id = "monster-$name-$emoji",
        slug = "monster",
        name = name,
        emoji = emoji,
        hp = 10,
        atk = 1,
        def = 1,
        imageUrl = "",
    )
}
