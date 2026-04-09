package org.example.project.features.study

enum class AdventurePhase {
    WALKING,
    ENCOUNTER,
    ATTACKING,
    ENEMY_DEFEATED,
    RESTING,
    PLAYER_DEAD,
    FLOOR_CLEAR
}

data class StudyQuestUiState(
    val type: StudySessionType = StudySessionType.STUDY,
    val status: StudySessionStatus = StudySessionStatus.READY,
    val targetStudyMinutes: Int = 25,
    val targetBreakMinutes: Int = 5,
    val elapsedSeconds: Long = 0,
    val isOvertime: Boolean = false,
    val currentLog: List<String> = listOf("冒険の準備が整った！"),
    val displayTime: String = "25:00",
    val genreId: String? = null,
    val adventurePhase: AdventurePhase = AdventurePhase.WALKING,
    val enemyName: String = "スライム",
    val enemyEmoji: String = "👾",
    val enemyHp: Int = 100,
    val enemyMaxHp: Int = 100,
    val lastDamage: Int = 0,
    val lastPlayerDamage: Int = 0,
    val defeatedCount: Int = 0,
    val serverRewards: List<String> = emptyList(),
    val serverSynced: Boolean? = null,
    val partyLeadName: String = "冒険者",
    val partyLeadImageUrl: String = "",
    val dungeonName: String? = null,
    // 階層システム
    val currentFloor: Int = 1,
    val totalFloors: Int = 10,
    val floorClearCount: Int = 0,
    // プレイヤーHP
    val playerHp: Int = 100,
    val playerMaxHp: Int = 100,
    // 獲得報酬トラッカー
    val earnedXp: Int = 0,
    val earnedStones: Int = 0
)
