package org.example.project.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ホーム画面の ViewModel
 * ユーザーステータス表示 + メインキャラ表示 + 勉強開始導線
 */
class HomeViewModel(
    private val homeUseCase: HomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> loadHome()
            is HomeIntent.StartStudy -> { /* ナビゲーションはネイティブ側で処理 */ }
            is HomeIntent.TapMainCharacter -> { /* キャラタップ演出はネイティブ側 */ }
            is HomeIntent.SelectDungeon -> {
                _uiState.update { it.copy(selectedDungeonName = intent.name) }
            }
        }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = homeUseCase.loadHomeData()
                _uiState.update {
                    it.copy(
                        totalStudySeconds = data.user.totalStudySeconds,
                        stones = data.user.stones,
                        gold = data.user.gold,
                        displayName = data.user.displayName,
                        mainCharacter = data.mainCharacter,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
