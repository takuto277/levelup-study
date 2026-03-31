package org.example.project.features.home

import org.example.project.domain.model.UserCharacter

/**
 * ホーム画面の UI 状態
 */
data class HomeUiState(
    val totalStudySeconds: Long = 0,
    val stones: Int = 0,
    val gold: Int = 0,
    val mainCharacter: UserCharacter? = null,
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /** 累計勉強時間のフォーマット表示（例: "124h 30m"） */
    val formattedStudyTime: String
        get() {
            val hours = totalStudySeconds / 3600
            val minutes = (totalStudySeconds % 3600) / 60
            return "${hours}h ${minutes}m"
        }
}
