package org.example.project.features.settings

import org.example.project.core.session.SessionMode

data class SettingsUiState(
    val displayedUserId: String = "",
    val apiBaseUrl: String = "",
    val selectedEnvironment: String = "",
    val stones: Int = 0,
    val gold: Int = 0,
    val forceDevSeed: Boolean = false,
    val sessionMode: SessionMode = SessionMode.SEED,
    val isLoading: Boolean = false,
    val toast: String? = null,
)
