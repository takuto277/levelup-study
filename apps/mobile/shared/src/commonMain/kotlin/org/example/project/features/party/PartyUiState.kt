package org.example.project.features.party

import org.example.project.domain.model.Party
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.model.UserWeapon

/**
 * 編成（Party）画面の UI 状態
 */
data class PartyUiState(
    val party: Party? = null,
    val ownedCharacters: List<UserCharacter> = emptyList(),
    val ownedWeapons: List<UserWeapon> = emptyList(),
    val selectedSlot: Int? = null,
    val selectedCharacter: UserCharacter? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
