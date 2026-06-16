package org.example.project.features.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.WeaponRepository

class CollectionViewModel(
    private val characterRepository: CharacterRepository,
    private val weaponRepository: WeaponRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
    }

    fun loadCollection() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val characters = characterRepository.getUserCharacters()
                val weapons = weaponRepository.getUserWeapons()
                _uiState.update {
                    it.copy(characters = characters, weapons = weapons, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "データの取得に失敗しました")
                }
            }
        }
    }
}
