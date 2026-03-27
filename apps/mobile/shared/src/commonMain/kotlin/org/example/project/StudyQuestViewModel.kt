package org.example.project

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * セッションの種類
 */
enum class StudySessionType {
    STUDY,  // 勉強中（25分）
    BREAK   // 休憩中（5分）
}

/**
 * セッションのステータス
 */
enum class StudySessionStatus {
    RUNNING,
    PAUSED,
    FINISHED // 完了した瞬間（結果画面用）
}

/**
 * 勉強・冒険セッションの状態
 */
data class StudyQuestState(
    val type: StudySessionType = StudySessionType.STUDY,
    val status: StudySessionStatus = StudySessionStatus.PAUSED,
    val remainingSeconds: Long = 1500, // 25分スタート
    val currentLog: List<String> = listOf("冒険の準備が整った！")
)

/**
 * 共有モジュールのViewModel
 */
class StudyQuestViewModel {
    private val _uiState = MutableStateFlow(StudyQuestState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val studyMessages = listOf(
        "モンスターに10のダメージを与えた！",
        "経験値を5獲得した！",
        "ゴールドを2手に入れた！",
        "深い霧が立ち込めている...",
        "草むらからガサガサと音がした"
    )
    
    private val breakMessages = listOf(
        "焚き火で休憩中...",
        "ポーションを飲んで体力を回復した",
        "装備のメンテナンスをしている",
        "これまでの冒険を振り返っている"
    )

    fun startQuest() {
        if (_uiState.value.status == StudySessionStatus.RUNNING) return
        
        val initialSeconds = if (_uiState.value.type == StudySessionType.STUDY) 1500L else 300L
        _uiState.update { 
            it.copy(
                status = StudySessionStatus.RUNNING,
                remainingSeconds = initialSeconds,
                currentLog = listOf(if (it.type == StudySessionType.STUDY) "冒険を開始した！" else "休憩所を見つけた。")
            )
        }
        startTimer()
    }

    fun togglePause() {
        if (_uiState.value.status == StudySessionStatus.RUNNING) {
            timerJob?.cancel()
            _uiState.update { it.copy(status = StudySessionStatus.PAUSED) }
        } else if (_uiState.value.status == StudySessionStatus.PAUSED) {
            _uiState.update { it.copy(status = StudySessionStatus.RUNNING) }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.update { state ->
                    val newRemaining = state.remainingSeconds - 1
                    var newLogs = state.currentLog
                    
                    // 10秒ごとにログ追加
                    if (newRemaining % 10 == 0L && newRemaining > 0) {
                        val messages = if (state.type == StudySessionType.STUDY) studyMessages else breakMessages
                        newLogs = (newLogs + messages.random()).takeLast(5)
                    }
                    
                    state.copy(
                        remainingSeconds = newRemaining,
                        currentLog = newLogs
                    )
                }
            }
            
            if (_uiState.value.remainingSeconds <= 0) {
                _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
            }
        }
    }

    /**
     * 結果画面から次のステップへ
     */
    fun nextSession() {
        _uiState.update { 
            if (it.type == StudySessionType.STUDY) {
                // 勉強終了 -> 休憩へ
                it.copy(
                    type = StudySessionType.BREAK,
                    remainingSeconds = 300,
                    status = StudySessionStatus.RUNNING,
                    currentLog = listOf("休憩を開始した。")
                )
            } else {
                // 休憩終了 -> 次の勉強へ
                it.copy(
                    type = StudySessionType.STUDY,
                    remainingSeconds = 1500,
                    status = StudySessionStatus.RUNNING,
                    currentLog = listOf("次の冒険へ出発だ！")
                )
            }
        }
        startTimer()
    }

    fun stopQuest() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { StudyQuestState() } // リセット
    }

    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        fun Long.pad(): String = if (this < 10) "0$this" else "$this"
        return "${m.pad()}:${s.pad()}"
    }
}
