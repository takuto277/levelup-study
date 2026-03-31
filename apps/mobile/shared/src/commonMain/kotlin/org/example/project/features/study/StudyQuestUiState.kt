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
    /** タイマー表示用（ViewModel が毎秒計算して反映） */
    val displayTime: String = "25:00",
    /** ジャンルID（ホーム画面で選択） */
    val genreId: String? = null,
    /** 冒険フェーズ */
    val adventurePhase: AdventurePhase = AdventurePhase.WALKING,
    /** 敵の名前 */
    val enemyName: String = "スライム",
    /** 敵の絵文字 */
    val enemyEmoji: String = "👾",
    /** 敵の現在HP */
    val enemyHp: Int = 100,
    /** 敵の最大HP */
    val enemyMaxHp: Int = 100,
    /** 直近のダメージ量（表示用） */
    val lastDamage: Int = 0,
    /** 倒した敵の累計数 */
    val defeatedCount: Int = 0,
    /** サーバーから返された報酬リスト（リザルト画面用） */
    val serverRewards: List<String> = emptyList(),
    /** サーバー同期完了フラグ（null=未送信, true=成功, false=失敗） */
    val serverSynced: Boolean? = null
)
