package org.example.project.features.study

import org.example.project.domain.model.StudyCompleteResult

/**
 * 勉強タイマー画面の UI 状態
 */
data class StudyUiState(
    val sessionType: StudySessionType = StudySessionType.STUDY,
    val sessionStatus: StudySessionStatus = StudySessionStatus.READY,
    val category: String? = null,
    val targetSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val isOvertime: Boolean = false,
    val adventureLog: List<String> = emptyList(),
    val completeResult: StudyCompleteResult? = null,
    val isShowingResult: Boolean = false,
    val error: String? = null
) {
    /** 表示用の残り秒数（マイナスにならない） */
    val displaySeconds: Int
        get() = if (isOvertime) elapsedSeconds - targetSeconds
                else maxOf(0, targetSeconds - elapsedSeconds)

    /** フォーマットされた時間表示 (MM:SS) */
    val formattedTime: String
        get() {
            val seconds = displaySeconds
            val min = seconds / 60
            val sec = seconds % 60
            val minStr = if (min < 10) "0$min" else "$min"
            val secStr = if (sec < 10) "0$sec" else "$sec"
            return "$minStr:$secStr"
        }

    /** 進捗率 (0.0 〜 1.0+) */
    val progress: Float
        get() = if (targetSeconds > 0) elapsedSeconds.toFloat() / targetSeconds else 0f
}

/** セッション種別 */
enum class StudySessionType {
    STUDY,
    BREAK
}

/** セッション状態 */
enum class StudySessionStatus {
    READY,
    RUNNING,
    PAUSED,
    FINISHED
}
