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
            is PartyIntent.SelectSlot -> _uiState.update {
                it.copy(
                    selectedSlot = if (it.selectedSlot == intent.slotPosition) null else intent.slotPosition
                )
            }
            is PartyIntent.AssignCharacter -> assignCharacter(intent.slotPosition, intent.userCharacterId)
            is PartyIntent.RemoveFromSlot -> removeFromSlot(intent.slotPosition)
            is PartyIntent.SelectCharacter -> selectCharacter(intent.userCharacterId)
            is PartyIntent.DismissCharacterDetail -> _uiState.update { it.copy(selectedCharacter = null) }
            is PartyIntent.EquipWeapon -> equipWeapon(intent.userCharacterId, intent.userWeaponId)
            is PartyIntent.LevelUpCharacter -> levelUpCharacter(intent.userCharacterId)
            is PartyIntent.LevelUpWeapon -> levelUpWeapon(intent.userWeaponId)
        }
    }

    private fun loadPartyData() {
        if (_uiState.value.isMutating) return
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
        if (_uiState.value.isMutating || _uiState.value.isLoading) return
        _uiState.update { it.copy(isMutating = true, error = null) }
        viewModelScope.launch {
            try {
                partyUseCase.assignCharacterToSlot(slotPosition, userCharacterId)
                _uiState.update { it.copy(isMutating = false) }
                loadPartyData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isMutating = false, error = e.message ?: "キャラクターの配置に失敗しました") }
            }
        }
    }

    private fun removeFromSlot(slotPosition: Int) {
        if (_uiState.value.isMutating || _uiState.value.isLoading) return
        _uiState.update { it.copy(isMutating = true, error = null) }
        viewModelScope.launch {
            try {
                partyUseCase.removeFromSlot(slotPosition)
                _uiState.update { it.copy(isMutating = false) }
                loadPartyData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isMutating = false, error = e.message ?: "スロット解除に失敗しました") }
            }
        }
    }

    private fun selectCharacter(userCharacterId: String) {
        val character = _uiState.value.ownedCharacters.find { it.id == userCharacterId }
        _uiState.update { it.copy(selectedCharacter = character) }
    }

    private fun equipWeapon(userCharacterId: String, userWeaponId: String?) {
        if (_uiState.value.isMutating || _uiState.value.isLoading) return
        _uiState.update { it.copy(isMutating = true) }
        viewModelScope.launch {
            try {
                partyUseCase.equipWeapon(userCharacterId, userWeaponId)
                val data = partyUseCase.loadPartyData()
                _uiState.update {
                    it.copy(
                        party = Party(slots = data.party.slots.sortedBy { s -> s.slotPosition }),
                        ownedCharacters = data.ownedCharacters,
                        ownedWeapons = data.ownedWeapons,
                        selectedCharacter = data.ownedCharacters.find { c -> c.id == userCharacterId },
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "武器装備に失敗しました") }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    private fun levelUpCharacter(userCharacterId: String) {
        if (_uiState.value.isMutating || _uiState.value.isLoading) return
        _uiState.update { it.copy(isMutating = true) }
        viewModelScope.launch {
            try {
                partyUseCase.levelUpCharacter(userCharacterId)
                refreshAfterMutation(userCharacterId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "レベルアップに失敗しました") }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    private fun levelUpWeapon(userWeaponId: String) {
        if (_uiState.value.isMutating || _uiState.value.isLoading) return
        _uiState.update { it.copy(isMutating = true) }
        val characterId = _uiState.value.selectedCharacter?.id
        viewModelScope.launch {
            try {
                partyUseCase.levelUpWeapon(userWeaponId)
                refreshAfterMutation(characterId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "武器のレベルアップに失敗しました") }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    private suspend fun refreshAfterMutation(selectedCharacterId: String?) {
        val data = partyUseCase.loadPartyData()
        _uiState.update {
            it.copy(
                party = Party(slots = data.party.slots.sortedBy { s -> s.slotPosition }),
                ownedCharacters = data.ownedCharacters,
                ownedWeapons = data.ownedWeapons,
                selectedCharacter = selectedCharacterId?.let { id ->
                    data.ownedCharacters.find { c -> c.id == id }
                },
                error = null
            )
        }
    }
}
