package org.example.project

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * セッションの種類
 */
enum class StudySessionType {
    STUDY,
    BREAK
}

/**
 * セッションのステータス
 */
enum class StudySessionStatus {
    READY,
    RUNNING,
    PAUSED,
    FINISHED
}

/**
 * 勉強・冒険セッションの状態
 */
data class StudyQuestState(
    val type: StudySessionType = StudySessionType.STUDY,
    val status: StudySessionStatus = StudySessionStatus.READY,
    val targetStudyMinutes: Int = 25,
    val targetBreakMinutes: Int = 5,
    val elapsedSeconds: Long = 0,
    val isOvertime: Boolean = false,
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
        "限界を超えた集中力を発揮している...",
        "ゾーンに入った！攻撃力が上昇！"
    )
    
    private val breakMessages = listOf(
        "焚き火で休憩中...",
        "ポーションを飲んで体力を回復した",
        "装備のメンテナンスをしている",
        "これまでの冒険を振り返っている"
    )

    fun updateTargetMinutes(studyMinutes: Int) {
        if (_uiState.value.status != StudySessionStatus.READY) return
        _uiState.update { it.copy(targetStudyMinutes = studyMinutes) }
    }

    fun startQuest(initialStudyMinutes: Int? = null) {
        if (_uiState.value.status == StudySessionStatus.RUNNING) return
        
        val studyMin = initialStudyMinutes ?: _uiState.value.targetStudyMinutes
        
        _uiState.update { 
            it.copy(
                status = StudySessionStatus.RUNNING,
                targetStudyMinutes = studyMin,
                elapsedSeconds = 0,
                isOvertime = false,
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
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    val targetSeconds = if (state.type == StudySessionType.STUDY) 
                        state.targetStudyMinutes.toLong() * 60 
                    else 
                        state.targetBreakMinutes.toLong() * 60

                    val newElapsed = state.elapsedSeconds + 1
                    var newLogs = state.currentLog
                    
                    // 10秒ごとにログ追加
                    if (newElapsed % 10 == 0L) {
                        val messages = if (state.type == StudySessionType.STUDY) studyMessages else breakMessages
                        newLogs = (newLogs + messages.random()).takeLast(5)
                    }
                    
                    val overTime = state.type == StudySessionType.STUDY && newElapsed > targetSeconds
                    
                    state.copy(
                        elapsedSeconds = newElapsed,
                        isOvertime = overTime,
                        currentLog = newLogs
                    )
                }
                
                // 休憩モードは目標時間に達したら自動終了
                val state = _uiState.value
                val targetSeconds = if (state.type == StudySessionType.STUDY) 
                    state.targetStudyMinutes.toLong() * 60 
                else 
                    state.targetBreakMinutes.toLong() * 60

                if (state.type == StudySessionType.BREAK && state.elapsedSeconds >= targetSeconds) {
                    _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
                    break
                }
            }
        }
    }

    /**
     * 表示用の秒数を計算する
     * 勉強中:
     * - 目標時間内: target - elapsed (カウントダウン)
     * - 延長中: elapsed (カウントアップ 25:01, 26:00...)
     * 休憩中:
     * - target - elapsed (カウントダウン)
     */
    fun getDisplaySeconds(state: StudyQuestState): Long {
        val targetSeconds = if (state.type == StudySessionType.STUDY) 
            state.targetStudyMinutes.toLong() * 60 
        else 
            state.targetBreakMinutes.toLong() * 60

        return if (state.type == StudySessionType.STUDY) {
            if (state.elapsedSeconds <= targetSeconds) {
                targetSeconds - state.elapsedSeconds
            } else {
                state.elapsedSeconds
            }
        } else {
            // 休憩は常にカウントダウン
            (targetSeconds - state.elapsedSeconds).coerceAtLeast(0)
        }
    }

    fun finishSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
    }

    fun nextSession() {
        _uiState.update { 
            if (it.type == StudySessionType.STUDY) {
                it.copy(
                    type = StudySessionType.BREAK,
                    elapsedSeconds = 0,
                    isOvertime = false,
                    status = StudySessionStatus.RUNNING,
                    currentLog = listOf("休憩を開始した。")
                )
            } else {
                it.copy(
                    type = StudySessionType.STUDY,
                    elapsedSeconds = 0,
                    isOvertime = false,
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
        _uiState.update { StudyQuestState(status = StudySessionStatus.READY) }
    }

    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        fun Long.pad(): String = if (this < 10) "0$this" else "$this"
        return "${m.pad()}:${s.pad()}"
    }
}
