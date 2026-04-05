package org.example.project.features.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot

/**
 * 編成（Party）画面の ViewModel
 * PartyUseCase 経由でバックエンドのパーティ・キャラ・武器データを取得
 */
class PartyViewModel(
    private val partyUseCase: PartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    init {
        loadPartyData()
    }

    fun onIntent(intent: PartyIntent) {
        when (intent) {
            is PartyIntent.Refresh -> loadPartyData()
            is PartyIntent.SelectSlot -> _uiState.update { it.copy(selectedSlot = intent.slotPosition) }
            is PartyIntent.AssignCharacter -> assignCharacter(intent.slotPosition, intent.userCharacterId)
            is PartyIntent.RemoveFromSlot -> removeFromSlot(intent.slotPosition)
            is PartyIntent.SelectCharacter -> selectCharacter(intent.userCharacterId)
            is PartyIntent.DismissCharacterDetail -> _uiState.update { it.copy(selectedCharacter = null) }
            is PartyIntent.EquipWeapon -> { /* TODO */ }
            is PartyIntent.LevelUpCharacter -> { /* TODO */ }
            is PartyIntent.LevelUpWeapon -> { /* TODO */ }
        }
    }

    private fun loadPartyData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val data = partyUseCase.loadPartyData()
                _uiState.update {
                    it.copy(
                        party = Party(
                            slots = data.party.slots.sortedBy { s -> s.slotPosition }
                        ),
                        ownedCharacters = data.ownedCharacters,
                        ownedWeapons = data.ownedWeapons,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "データの取得に失敗しました")
                }
            }
        }
    }

    private fun assignCharacter(slotPosition: Int, userCharacterId: String) {
        viewModelScope.launch {
            try {
                partyUseCase.assignCharacterToSlot(slotPosition, userCharacterId)
                loadPartyData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun removeFromSlot(slotPosition: Int) {
        viewModelScope.launch {
            try {
                partyUseCase.removeFromSlot(slotPosition)
                loadPartyData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun selectCharacter(userCharacterId: String) {
        val character = _uiState.value.ownedCharacters.find { it.id == userCharacterId }
        _uiState.update { it.copy(selectedCharacter = character) }
    }
}
