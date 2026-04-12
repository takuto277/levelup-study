package org.example.project.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.network.NetworkException
import org.example.project.core.network.isDeviceOnline
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class HomeViewModel(
    private val homeUseCase: HomeUseCase,
    private val userRepository: UserRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> loadHome()
            is HomeIntent.StartStudy -> { }
            is HomeIntent.TapMainCharacter -> { }
            is HomeIntent.SelectDungeon -> selectDungeon(intent.id, intent.name, intent.imageUrl)
            is HomeIntent.AddGenre -> addGenre(intent.label, intent.emoji, intent.colorHex)
            is HomeIntent.DeleteGenre -> deleteGenre(intent.genreId)
        }
    }

    private fun selectDungeon(id: String, name: String, imageUrl: String? = null) {
        _uiState.update {
            it.copy(
                selectedDungeonId = id,
                selectedDungeonName = name,
                selectedDungeonImageUrl = imageUrl?.takeIf { s -> s.isNotBlank() }
            )
        }
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
                try {
                    studyRepository.syncPendingSessions()
                } catch (_: Exception) { }
                val data = homeUseCase.loadHomeData()
                applyHomeData(data, clearLoading = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message, isOfflineTraining = !isDeviceOnline())
                }
            }
        }
    }

    private fun applyHomeData(data: HomeUseCase.HomeData, clearLoading: Boolean) {
        val offlineTraining = !isDeviceOnline()
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
                selectedDungeonImageUrl = data.selectedDungeonImageUrl ?: it.selectedDungeonImageUrl,
                isLoading = if (clearLoading) false else it.isLoading,
                isOfflineTraining = offlineTraining
            )
        }
    }

    private fun addGenre(label: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            try {
                homeUseCase.createGenre(label, emoji, colorHex)
                val data = homeUseCase.loadHomeData()
                applyHomeData(data, clearLoading = false)
            } catch (_: Exception) { }
        }
    }

    private fun deleteGenre(genreId: String) {
        viewModelScope.launch {
            try {
                homeUseCase.deleteGenre(genreId)
                val data = homeUseCase.loadHomeData()
                applyHomeData(data, clearLoading = false)
                _uiState.update { it.copy(error = null) }
            } catch (e: NetworkException) {
                _uiState.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "ジャンルの削除に失敗しました") }
            }
        }
    }
}
