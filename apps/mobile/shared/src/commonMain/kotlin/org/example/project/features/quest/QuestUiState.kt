package org.example.project.features.quest

import org.example.project.domain.model.DungeonCategory
import org.example.project.domain.model.DungeonDifficulty

/**
 * ダンジョンクリア時の報酬（UI 表示用）
 */
data class DungeonReward(
    val gold: Int,
    val exp: Int,
    val gachaStones: Int = 0,
    val bonusItemName: String? = null,
    val bonusItemDropRate: Float = 0f
)

/**
 * ダンジョン（UI 表示用ビューモデル）
 *
 * ドメインモデル MasterDungeon + DungeonProgress をフラットに統合し
 * 画面表示に必要な情報をすべて持たせる。
 * DungeonDifficulty / DungeonCategory は domain.model から import。
 */
data class Dungeon(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: DungeonDifficulty,
    val category: DungeonCategory = DungeonCategory.GENERAL,
    val totalStages: Int,
    val clearedStages: Int = 0,
    val recommendedMinutes: Int,
    val rewards: DungeonReward,
    val iconEmoji: String,
    val isFromServer: Boolean = false,
    val isLocked: Boolean = false
) {
    val progress: Float
        get() = if (totalStages > 0) clearedStages.toFloat() / totalStages else 0f

    val isCleared: Boolean
        get() = clearedStages >= totalStages
}

/**
 * 冒険画面のUI状態
 */
data class QuestUiState(
    val dungeons: List<Dungeon> = emptyList(),
    val selectedDungeon: Dungeon? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
