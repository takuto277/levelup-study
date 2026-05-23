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
    val isActive: Boolean,
    val stages: List<DungeonStage> = emptyList(),
)

/**
 * 敵マスタ（サーバー正規化。slug はバンドル画像、imageUrl は将来 CDN）
 */
data class DungeonMonster(
    val id: String,
    val slug: String,
    val name: String,
    val emoji: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val imageUrl: String,
    val isActive: Boolean = true
)

/**
 * ステージ上の敵スロット（同一 monster を複数行で表現）
 */
data class DungeonStageEnemy(
    val sortOrder: Int,
    val count: Int,
    val monster: DungeonMonster
)

/**
 * ステージ drop_table の1行
 */
data class StageDrop(
    val itemType: String,
    val amount: Int,
    val rate: Double = 1.0,
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
    val enemyComposition: String = "[]",
    val drops: List<StageDrop> = emptyList(),
    val enemies: List<DungeonStageEnemy> = emptyList()
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
