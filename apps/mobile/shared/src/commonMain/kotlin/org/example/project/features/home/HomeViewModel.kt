package org.example.project.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.repository.UserRepository

class HomeViewModel(
    private val homeUseCase: HomeUseCase,
    private val userRepository: UserRepository
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
            is HomeIntent.SelectDungeon -> selectDungeon(intent.id, intent.name)
            is HomeIntent.AddGenre -> addGenre(intent.label, intent.emoji, intent.colorHex)
        }
    }

    private fun selectDungeon(id: String, name: String) {
        _uiState.update { it.copy(selectedDungeonId = id, selectedDungeonName = name) }
        viewModelScope.launch {
            try {
                userRepository.updateSelectedDungeon(id)
            } catch (_: Exception) { }
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
                        selectedDungeonId = data.user.selectedDungeonId ?: it.selectedDungeonId,
                        selectedDungeonName = data.selectedDungeonName ?: it.selectedDungeonName,
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
