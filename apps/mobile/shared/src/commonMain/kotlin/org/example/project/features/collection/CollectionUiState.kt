package org.example.project.features.collection

import org.example.project.domain.model.UserCharacter
import org.example.project.domain.model.UserWeapon

data class CollectionUiState(
    val characters: List<UserCharacter> = emptyList(),
    val weapons: List<UserWeapon> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
