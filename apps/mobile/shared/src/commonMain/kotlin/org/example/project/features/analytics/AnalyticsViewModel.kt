package org.example.project.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 記録（Analytics）画面の ViewModel
 * 勉強時間の統計・グラフ・カレンダー表示を管理
 */
class AnalyticsViewModel(
    private val analyticsUseCase: AnalyticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.Refresh -> loadAnalytics()
            is AnalyticsIntent.ChangePeriod -> _uiState.update { it.copy(selectedPeriod = intent.period) }
        }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = analyticsUseCase.loadAnalyticsData()
                val dailyMap = analyticsUseCase.calculateDailyStudy(data.recentSessions)

                _uiState.update {
                    it.copy(
                        totalStudySeconds = data.user.totalStudySeconds,
                        recentSessions = data.recentSessions,
                        dailyStudySeconds = dailyMap,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
