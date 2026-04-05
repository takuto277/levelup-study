package org.example.project.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            is HomeIntent.StartStudy -> { }
            is HomeIntent.TapMainCharacter -> { }
            is HomeIntent.SelectDungeon -> {
                _uiState.update { it.copy(selectedDungeonId = intent.id, selectedDungeonName = intent.name) }
            }
            is HomeIntent.AddGenre -> addGenre(intent.label, intent.emoji, intent.colorHex)
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
                        genres = data.genres,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun addGenre(label: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            try {
                homeUseCase.createGenre(label, emoji, colorHex)
                val data = homeUseCase.loadHomeData()
                _uiState.update { it.copy(genres = data.genres) }
            } catch (_: Exception) { }
        }
    }
}
