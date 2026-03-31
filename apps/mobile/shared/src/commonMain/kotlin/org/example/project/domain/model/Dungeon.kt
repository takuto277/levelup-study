package org.example.project.domain.model

/**
 * ダンジョンマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
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
 * ダンジョンのカテゴリー（勉強ジャンルとリンク）
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
 * ダンジョンマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
data class MasterDungeon(
    val id: String,
    val name: String,
    val description: String? = null,
    val difficulty: DungeonDifficulty = DungeonDifficulty.BEGINNER,
    val category: DungeonCategory = DungeonCategory.GENERAL,
    val totalStages: Int = 0,
    val recommendedMinutes: Int? = null,
    val iconEmoji: String? = null,
    val sortOrder: Int,
    val unlockCondition: String?,
    val imageUrl: String,
    val rewardSummary: String? = null,
    val isActive: Boolean
)

/**
 * ダンジョンステージマスタ
 */
data class DungeonStage(
    val id: String,
    val dungeonId: String,
    val stageNumber: Int,
    val stageName: String? = null,
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
