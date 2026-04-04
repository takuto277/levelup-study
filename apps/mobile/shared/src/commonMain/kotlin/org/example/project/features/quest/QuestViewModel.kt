package org.example.project.features.quest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 冒険（ダンジョン選択）画面のViewModel
 * QuestUseCase 経由でサーバーのダンジョンデータを取得
 */
class QuestViewModel(
    private val questUseCase: QuestUseCase
) {
    private val _uiState = MutableStateFlow(QuestUiState())
    val uiState: StateFlow<QuestUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        loadDungeons()
    }

    fun onIntent(intent: QuestIntent) {
        when (intent) {
            is QuestIntent.SelectDungeon -> selectDungeon(intent.dungeonId)
            is QuestIntent.DismissDetail -> _uiState.update { it.copy(selectedDungeon = null) }
            is QuestIntent.RefreshDungeons -> loadDungeons()
        }
    }

    private fun selectDungeon(dungeonId: String) {
        val dungeon = _uiState.value.dungeons.find { it.id == dungeonId }
        _uiState.update { it.copy(selectedDungeon = dungeon) }
    }

    private fun loadDungeons() {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            try {
                val dungeons = questUseCase.loadDungeons()
                _uiState.update { it.copy(dungeons = dungeons, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "ダンジョンの取得に失敗しました"
                    )
                }
            }
        }
    }
}
