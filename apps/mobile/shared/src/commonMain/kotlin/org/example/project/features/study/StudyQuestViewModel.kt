package org.example.project.features.study

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 勉強クエスト画面の ViewModel
 *
 * - MutableStateFlow を内部保持、外部には StateFlow のみ公開
 * - UI からの入力は onIntent() に集約
 * - タイマー進行・表示時間計算・ログ生成すべて ViewModel が管理
 * - View 側で状態を独自保持してはならない
 */
class StudyQuestViewModel {

    private val _uiState = MutableStateFlow(StudyQuestUiState())
    val uiState: StateFlow<StudyQuestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── ログメッセージ ──────────────────────────────
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

    // ── Intent ハンドラ ─────────────────────────────
    fun onIntent(intent: StudyQuestIntent) {
        when (intent) {
            is StudyQuestIntent.StartQuest -> startQuest(intent.studyMinutes)
            is StudyQuestIntent.TogglePause -> togglePause()
            is StudyQuestIntent.FinishSession -> finishSession()
            is StudyQuestIntent.NextSession -> nextSession()
            is StudyQuestIntent.StopQuest -> stopQuest()
        }
    }

    // ── 公開ユーティリティ（iOS 側 KMP ブリッジ用に残す） ──
    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        fun Long.pad(): String = if (this < 10) "0$this" else "$this"
        return "${m.pad()}:${s.pad()}"
    }

    fun getDisplaySeconds(state: StudyQuestUiState): Long {
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
            (targetSeconds - state.elapsedSeconds).coerceAtLeast(0)
        }
    }

    // ── 内部ロジック ────────────────────────────────

    private fun startQuest(studyMinutes: Int) {
        if (_uiState.value.status == StudySessionStatus.RUNNING) return

        _uiState.update {
            it.copy(
                type = StudySessionType.STUDY,
                status = StudySessionStatus.RUNNING,
                targetStudyMinutes = studyMinutes,
                elapsedSeconds = 0,
                isOvertime = false,
                currentLog = listOf("冒険を開始した！"),
                displayTime = formatTime(studyMinutes.toLong() * 60)
            )
        }
        startTimer()
    }

    private fun togglePause() {
        val current = _uiState.value
        when (current.status) {
            StudySessionStatus.RUNNING -> {
                timerJob?.cancel()
                _uiState.update { it.copy(status = StudySessionStatus.PAUSED) }
            }
            StudySessionStatus.PAUSED -> {
                _uiState.update { it.copy(status = StudySessionStatus.RUNNING) }
                startTimer()
            }
            else -> { /* READY / FINISHED は何もしない */ }
        }
    }

    private fun finishSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
    }

    private fun nextSession() {
        val current = _uiState.value
        val newType = if (current.type == StudySessionType.STUDY)
            StudySessionType.BREAK else StudySessionType.STUDY

        val targetSec = if (newType == StudySessionType.STUDY)
            current.targetStudyMinutes.toLong() * 60
        else
            current.targetBreakMinutes.toLong() * 60

        _uiState.update {
            it.copy(
                type = newType,
                status = StudySessionStatus.RUNNING,
                elapsedSeconds = 0,
                isOvertime = false,
                currentLog = listOf(
                    if (newType == StudySessionType.BREAK) "休憩を開始した。" else "次の冒険へ出発だ！"
                ),
                displayTime = formatTime(targetSec)
            )
        }
        startTimer()
    }

    private fun stopQuest() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            StudyQuestUiState(
                status = StudySessionStatus.READY,
                displayTime = formatTime(it.targetStudyMinutes.toLong() * 60)
            )
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

                    if (newElapsed % 10 == 0L) {
                        val messages = if (state.type == StudySessionType.STUDY)
                            studyMessages else breakMessages
                        newLogs = (newLogs + messages.random()).takeLast(5)
                    }

                    val overTime = state.type == StudySessionType.STUDY && newElapsed > targetSeconds

                    val displaySec = if (state.type == StudySessionType.STUDY) {
                        if (newElapsed <= targetSeconds) targetSeconds - newElapsed else newElapsed
                    } else {
                        (targetSeconds - newElapsed).coerceAtLeast(0)
                    }

                    state.copy(
                        elapsedSeconds = newElapsed,
                        isOvertime = overTime,
                        currentLog = newLogs,
                        displayTime = formatTime(displaySec)
                    )
                }

                val state = _uiState.value
                if (state.type == StudySessionType.BREAK) {
                    val targetSeconds = state.targetBreakMinutes.toLong() * 60
                    if (state.elapsedSeconds >= targetSeconds) {
                        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
                        break
                    }
                }
            }
        }
    }

    fun cleanup() {
        timerJob?.cancel()
        viewModelScope.cancel()
    }
}
