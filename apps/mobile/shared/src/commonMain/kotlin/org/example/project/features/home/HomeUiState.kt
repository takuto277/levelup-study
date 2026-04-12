package org.example.project.features.home

import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.UserCharacter

data class HomeUiState(
    val totalStudySeconds: Long = 0,
    val stones: Int = 0,
    val gold: Int = 0,
    val mainCharacter: UserCharacter? = null,
    val displayName: String = "",
    val selectedDungeonId: String? = null,
    val selectedDungeonName: String? = null,
    val genres: List<MasterStudyGenre> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** ネット未接続時はダンジョンの代わりに訓練場モードで勉強する */
    val isOfflineTraining: Boolean = false
) {
    val formattedStudyTime: String
        get() {
            val hours = totalStudySeconds / 3600
            val minutes = (totalStudySeconds % 3600) / 60
            return "${hours}h ${minutes}m"
        }
}
