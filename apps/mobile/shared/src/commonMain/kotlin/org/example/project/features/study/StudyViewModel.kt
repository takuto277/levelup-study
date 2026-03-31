package org.example.project.features.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 勉強タイマー画面の ViewModel
 * ポモドーロ式タイマー + RPG冒険連動 + セッション完了報酬
 */
class StudyViewModel(
    private val studyUseCase: StudyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartedAt: String? = null

    companion object {
        private const val DEFAULT_STUDY_SECONDS = 25 * 60  // 25分
        private const val DEFAULT_BREAK_SECONDS = 5 * 60   // 5分
        private const val ADVENTURE_LOG_INTERVAL = 10       // 10秒ごとにログ
    }

    /** RPG風の冒険ログメッセージ */
    private val adventureMessages = listOf(
        "🗡️ パーティが前方のスライムを倒した！",
        "💎 光る宝石を発見した！",
        "🛡️ 罠を巧みに回避した！",
        "📖 古代の書物を発見した！",
        "⚔️ 強力なモンスターが現れた！",
        "🏆 隠し部屋を発見した！",
        "🔥 炎の精霊が仲間に力を与えた！",
        "🌿 回復の泉を見つけた！",
        "🗝️ 金色の鍵を手に入れた！",
        "🐉 遠くでドラゴンの咆哮が聞こえる…"
    )

    fun onIntent(intent: StudyIntent) {
        when (intent) {
            is StudyIntent.SetCategory -> _uiState.update { it.copy(category = intent.category) }
            is StudyIntent.SetTargetSeconds -> _uiState.update { it.copy(targetSeconds = intent.seconds) }
            is StudyIntent.StartTimer -> startTimer()
            is StudyIntent.TogglePause -> togglePause()
            is StudyIntent.FinishSession -> finishSession()
            is StudyIntent.ResetTimer -> resetTimer()
            is StudyIntent.NextSession -> nextSession()
            is StudyIntent.DismissResult -> _uiState.update { it.copy(isShowingResult = false) }
        }
    }

    private fun startTimer() {
        sessionStartedAt = kotlinx.datetime.Clock.System.now().toString()
        _uiState.update {
            it.copy(
                sessionStatus = StudySessionStatus.RUNNING,
                elapsedSeconds = 0,
                isOvertime = false,
                adventureLog = listOf("⚔️ 冒険が始まった！")
            )
        }
        startTickJob()
    }

    private fun togglePause() {
        val current = _uiState.value
        when (current.sessionStatus) {
            StudySessionStatus.RUNNING -> {
                timerJob?.cancel()
                _uiState.update { it.copy(sessionStatus = StudySessionStatus.PAUSED) }
            }
            StudySessionStatus.PAUSED -> {
                _uiState.update { it.copy(sessionStatus = StudySessionStatus.RUNNING) }
                startTickJob()
            }
            else -> { /* no-op */ }
        }
    }

    private fun finishSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(sessionStatus = StudySessionStatus.FINISHED) }

        viewModelScope.launch {
            val state = _uiState.value
            val endedAt = kotlinx.datetime.Clock.System.now().toString()
            try {
                val result = studyUseCase.completeSession(
                    category = state.category,
                    startedAt = sessionStartedAt ?: endedAt,
                    endedAt = endedAt,
                    durationSeconds = state.elapsedSeconds,
                    isCompleted = state.elapsedSeconds >= state.targetSeconds
                )
                _uiState.update { it.copy(completeResult = result, isShowingResult = true) }
            } catch (e: Exception) {
                // オフライン時はローカルに保存
                studyUseCase.saveOfflineSession(
                    category = state.category,
                    startedAt = sessionStartedAt ?: endedAt,
                    endedAt = endedAt,
                    durationSeconds = state.elapsedSeconds,
                    isCompleted = state.elapsedSeconds >= state.targetSeconds
                )
                _uiState.update { it.copy(error = "オフラインモード: 報酬は同期時に確定します") }
            }
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        sessionStartedAt = null
        _uiState.update {
            StudyUiState(
                targetSeconds = DEFAULT_STUDY_SECONDS,
                category = it.category
            )
        }
    }

    private fun nextSession() {
        val isStudy = _uiState.value.sessionType == StudySessionType.STUDY
        _uiState.update {
            it.copy(
                sessionType = if (isStudy) StudySessionType.BREAK else StudySessionType.STUDY,
                sessionStatus = StudySessionStatus.READY,
                targetSeconds = if (isStudy) DEFAULT_BREAK_SECONDS else DEFAULT_STUDY_SECONDS,
                elapsedSeconds = 0,
                isOvertime = false,
                adventureLog = emptyList(),
                completeResult = null,
                isShowingResult = false
            )
        }
    }

    private fun startTickJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_uiState.value.sessionStatus != StudySessionStatus.RUNNING) break

                _uiState.update { state ->
                    val newElapsed = state.elapsedSeconds + 1
                    val isOvertime = newElapsed > state.targetSeconds
                    val newLog = if (newElapsed % ADVENTURE_LOG_INTERVAL == 0 &&
                        state.sessionType == StudySessionType.STUDY) {
                        state.adventureLog + adventureMessages.random()
                    } else {
                        state.adventureLog
                    }

                    state.copy(
                        elapsedSeconds = newElapsed,
                        isOvertime = isOvertime,
                        adventureLog = newLog
                    )
                }

                // 休憩モードは目標時間で自動終了
                val state = _uiState.value
                if (state.sessionType == StudySessionType.BREAK &&
                    state.elapsedSeconds >= state.targetSeconds) {
                    _uiState.update { it.copy(sessionStatus = StudySessionStatus.FINISHED) }
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
