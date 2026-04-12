package org.example.project.features.quest

import org.example.project.domain.local.LocalDungeonIds
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
    /** サーバー `m_dungeons.image_url`（一覧・冒険シーンの背景用） */
    val imageUrl: String = "",
    val isFromServer: Boolean = false,
    val isLocked: Boolean = false
) {
    val progress: Float
        get() = if (totalStages > 0) clearedStages.toFloat() / totalStages else 0f

    val isCleared: Boolean
        get() = clearedStages >= totalStages

    /** 一覧・詳細の「推定Lv.」表示用（難易度＋進行の目安）。訓練場は 1 固定。 */
    fun estimatedRecommendedLevel(): Int {
        if (LocalDungeonIds.isTrainingGround(id)) return 1
        val base = when (difficulty) {
            DungeonDifficulty.BEGINNER -> 10
            DungeonDifficulty.INTERMEDIATE -> 35
            DungeonDifficulty.ADVANCED -> 60
            DungeonDifficulty.EXPERT -> 90
            DungeonDifficulty.LEGENDARY -> 130
        }
        return (base + clearedStages * 3).coerceIn(1, 999)
    }

    fun estimatedLevelChipText(): String = "推定Lv.${estimatedRecommendedLevel()}"
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
