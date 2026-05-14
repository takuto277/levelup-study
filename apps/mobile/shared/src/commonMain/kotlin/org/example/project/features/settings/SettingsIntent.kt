package org.example.project.features.settings

sealed interface SettingsIntent {
    data object Refresh : SettingsIntent
    data class PatchCurrencies(val stonesDelta: Int, val goldDelta: Int) : SettingsIntent
    data object ClearToast : SettingsIntent
}
