package org.example.project.features.study

/**
 * 冒険フェーズ（勉強中のアニメーション切り替え用）
 */
enum class AdventurePhase {
    /** 前進中（歩きアニメ） */
    WALKING,
    /** 敵とエンカウント */
    ENCOUNTER,
    /** 攻撃中（ダメージ演出） */
    ATTACKING,
    /** 敵を倒した */
    ENEMY_DEFEATED,
    /** 休憩中（焚き火・回復） */
    RESTING
}

/**
 * 勉強・冒険セッションの UI 状態（不変データクラス）
 *
 * すべての画面表示に必要な値はここに集約する。
 * View 側で独自に @State / mutableStateOf で値を保持してはならない。
 *
 * NOTE: StudySessionType / StudySessionStatus は StudyUiState.kt に定義済み
 */
data class StudyQuestUiState(
    val type: StudySessionType = StudySessionType.STUDY,
    val status: StudySessionStatus = StudySessionStatus.READY,
    val targetStudyMinutes: Int = 25,
    val targetBreakMinutes: Int = 5,
    val elapsedSeconds: Long = 0,
    val isOvertime: Boolean = false,
    val currentLog: List<String> = listOf("冒険の準備が整った！"),
    val displayTime: String = "25:00",
    val genreId: String? = null,
    val adventurePhase: AdventurePhase = AdventurePhase.WALKING,
    val enemyName: String = "スライム",
    val enemyEmoji: String = "👾",
    val enemyHp: Int = 100,
    val enemyMaxHp: Int = 100,
    val lastDamage: Int = 0,
    val defeatedCount: Int = 0,
    val serverRewards: List<String> = emptyList(),
    val serverSynced: Boolean? = null,
    /** パーティ先頭キャラクター名 */
    val partyLeadName: String = "冒険者",
    /** パーティ先頭キャラクター画像URL */
    val partyLeadImageUrl: String = ""
)
