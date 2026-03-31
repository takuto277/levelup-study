package org.example.project.features.study

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
    val displayTime: String = "25:00"
)
