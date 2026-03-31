package org.example.project.domain.model

/**
 * 勉強セッション
 * Source of Truth: サーバー（Go backend model.StudySession に対応）
 */
data class StudySession(
    val id: String,
    val userId: String,
    val category: String?,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Int,
    val isCompleted: Boolean,
    val createdAt: String
)

/**
 * セッション報酬明細
 * 1セッションに対し複数行（石, ゴールド, XP 等）
 */
data class StudyReward(
    val id: String,
    val sessionId: String,
    val rewardType: RewardType,
    val amount: Int,
    val itemId: String?,
    val createdAt: String
)

/** 報酬種別 */
enum class RewardType {
    STONES,
    STONES_BONUS_30,
    STONES_BONUS_60,
    STONES_BONUS_DAILY,
    GOLD,
    XP,
    ITEM_DROP
}

/**
 * 勉強完了時のレスポンス
 * サーバーが検証・報酬計算後に返す確定結果
 */
data class StudyCompleteResult(
    val sessionId: String,
    val rewards: List<StudyReward>,
    val updatedUser: User
)
