package org.example.project.features.quest

/**
 * ダンジョンの難易度
 */
enum class DungeonDifficulty(val label: String, val stars: Int) {
    BEGINNER("初級", 1),
    INTERMEDIATE("中級", 2),
    ADVANCED("上級", 3),
    EXPERT("超級", 4),
    LEGENDARY("伝説", 5)
}

/**
 * ダンジョンのカテゴリー（勉強ジャンル）
 */
enum class DungeonCategory(val label: String, val emoji: String) {
    GENERAL("総合", "📚"),
    MATH("数学", "🔢"),
    SCIENCE("理科", "🔬"),
    LANGUAGE("語学", "🌍"),
    PROGRAMMING("プログラミング", "💻"),
    CREATIVE("クリエイティブ", "🎨")
}

/**
 * ダンジョンクリア時の報酬
 */
data class DungeonReward(
    val gold: Int,
    val exp: Int,
    val gachaStones: Int = 0,
    val bonusItemName: String? = null,
    val bonusItemDropRate: Float = 0f
)

/**
 * ダンジョン
 * サーバーから取得するものとローカルに持つものの両方をこのモデルで表現する
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
