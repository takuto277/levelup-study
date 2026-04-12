package org.example.project.domain.model

import kotlinx.serialization.Serializable

/**
 * オフラインまたは送信失敗時にローカルへ溜め、オンライン復帰後に [StudyRepository.completeSession] 相当で送信するペイロード。
 */
@Serializable
data class PendingStudyCompletion(
    val localId: String,
    val category: String? = null,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Int,
    val isCompleted: Boolean,
    val userCharacterId: String? = null,
    val defeatNormalCount: Int = 0,
    val defeatBossCount: Int = 0,
    val difficultyMultiplier: Double = 1.0,
    /** 訓練場（敵討伐なし・経験値はサーバーで時間分のみ） */
    val isTrainingGround: Boolean = false
)
