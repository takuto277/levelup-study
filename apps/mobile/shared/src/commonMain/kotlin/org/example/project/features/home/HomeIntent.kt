package org.example.project.features.home

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object StartStudy : HomeIntent
    data object TapMainCharacter : HomeIntent
    data class SelectDungeon(val id: String, val name: String) : HomeIntent
    data class AddGenre(val label: String, val emoji: String, val colorHex: String) : HomeIntent
    data class DeleteGenre(val genreId: String) : HomeIntent
}
