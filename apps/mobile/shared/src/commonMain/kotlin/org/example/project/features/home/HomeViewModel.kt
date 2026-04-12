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
import org.example.project.core.storage.KeyValueStore
import org.example.project.domain.local.LocalDungeonIds
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class HomeViewModel(
    private val homeUseCase: HomeUseCase,
    private val userRepository: UserRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val kv = KeyValueStore()

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
        if (LocalDungeonIds.isTrainingGround(id)) {
            kv.putString(KEY_PERSISTED_LOCAL_DUNGEON_ID, id)
        } else {
            kv.remove(KEY_PERSISTED_LOCAL_DUNGEON_ID)
        }
        _uiState.update {
            it.copy(
                selectedDungeonId = id,
                selectedDungeonName = name,
                selectedDungeonImageUrl = imageUrl?.takeIf { s -> s.isNotBlank() }
            )
        }
        if (LocalDungeonIds.isTrainingGround(id)) return
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
                    withPersistedTrainingOverride(
                        it.copy(isLoading = false, error = e.message, isOfflineTraining = !isDeviceOnline())
                    )
                }
            }
        }
    }

    private fun applyHomeData(data: HomeUseCase.HomeData, clearLoading: Boolean) {
        val offlineTraining = !isDeviceOnline()
        val usePersistedTraining = LocalDungeonIds.isTrainingGround(kv.getString(KEY_PERSISTED_LOCAL_DUNGEON_ID))
        _uiState.update { prev ->
            val (mergedId, mergedName, mergedImg) = if (usePersistedTraining) {
                Triple(LocalDungeonIds.TRAINING_GROUND, LocalDungeonIds.TRAINING_GROUND_NAME, null)
            } else {
                Triple(
                    data.user.selectedDungeonId ?: prev.selectedDungeonId,
                    data.selectedDungeonName ?: prev.selectedDungeonName,
                    // ホームデータ側の方針（同梱背景）に合わせ、前回のリモート URL は引き継がない
                    data.selectedDungeonImageUrl
                )
            }
            prev.copy(
                totalStudySeconds = data.user.totalStudySeconds,
                stones = data.user.stones,
                gold = data.user.gold,
                displayName = data.user.displayName,
                mainCharacter = data.mainCharacter,
                genres = data.genres,
                selectedDungeonId = mergedId,
                selectedDungeonName = mergedName,
                selectedDungeonImageUrl = mergedImg,
                isLoading = if (clearLoading) false else prev.isLoading,
                isOfflineTraining = offlineTraining
            )
        }
    }

    private fun withPersistedTrainingOverride(state: HomeUiState): HomeUiState {
        if (!LocalDungeonIds.isTrainingGround(kv.getString(KEY_PERSISTED_LOCAL_DUNGEON_ID))) return state
        return state.copy(
            selectedDungeonId = LocalDungeonIds.TRAINING_GROUND,
            selectedDungeonName = LocalDungeonIds.TRAINING_GROUND_NAME,
            selectedDungeonImageUrl = null
        )
    }

    private companion object {
        /** サーバーに存在しないローカル専用ダンジョンの選択をホーム再読込後も維持する */
        const val KEY_PERSISTED_LOCAL_DUNGEON_ID = "home_persisted_local_dungeon_id"
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
