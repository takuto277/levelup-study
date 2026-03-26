package org.example.project

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 勉強セッションの状態
 */
data class StudySessionState(
    val isActive: Boolean = false,
    val elapsedSeconds: Long = 0,
    val currentLog: List<String> = emptyList()
)

/**
 * 共有モジュールのViewModel（ビジネスロジック担当）
 * 本来は各プラットフォームでViewModelを継承するが、ここでは共通ロジックを示す。
 */
class StudyViewModel {
    private val _uiState = MutableStateFlow(StudySessionState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startStudy() {
        if (_uiState.value.isActive) return
        
        _uiState.value = _uiState.value.copy(isActive = true, currentLog = listOf("冒険を開始した！"))
        
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1
                )
                
                // 10秒ごとにログを追加する例
                if (_uiState.value.elapsedSeconds % 10 == 0L) {
                    addLog("モンスターと遭遇した！")
                }
            }
        }
    }

    fun stopStudy() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isActive = false)
        // ここでDB保存やAPI送信のUseCaseを呼ぶ
    }

    private fun addLog(message: String) {
        val newLogs = _uiState.value.currentLog + message
        _uiState.value = _uiState.value.copy(currentLog = newLogs.takeLast(10))
    }
}
