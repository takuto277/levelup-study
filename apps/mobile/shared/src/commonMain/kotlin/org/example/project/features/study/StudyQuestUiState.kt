package org.example.project.features.study

/** 戦闘ターン周期（0: プレイヤー攻撃, 1: 敵反撃, 2: 間合い）秒。ViewModel / UI で同じ値を使うこと。 */
const val STUDY_QUEST_ATTACK_CYCLE_SEC = 3L

enum class AdventurePhase {
    WALKING,
    ENCOUNTER,
    ATTACKING,
    ENEMY_DEFEATED,
    RESTING,
    PLAYER_DEAD,
    FLOOR_CLEAR,
    /** オフライン訓練場（敵戦闘なし） */
    TRAINING
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
    /** オフライン時の訓練場（討伐カウントは常に 0 でサーバー送信） */
    val isTrainingGround: Boolean = false,
    val adventurePhase: AdventurePhase = AdventurePhase.WALKING,
    /** 現在フェーズ内の経過秒（タイマー1秒ごとに加算。戦闘中 idle→prep→attack の周期に使用） */
    val adventurePhaseTick: Long = 0,
    val enemyName: String = "スライム",
    val enemyEmoji: String = "👾",
    val enemySpriteKey: String = "slime",
    val enemyHp: Int = 100,
    val enemyMaxHp: Int = 100,
    val lastDamage: Int = 0,
    val lastPlayerDamage: Int = 0,
    val defeatedCount: Int = 0,
    /** 最終フロア以外で倒した敵（サーバー経験値: +10/体） */
    val normalDefeatCount: Int = 0,
    /** 最終フロアで倒したボス（サーバー経験値: +50/体） */
    val bossDefeatCount: Int = 0,
    val serverRewards: List<String> = emptyList(),
    val serverSynced: Boolean? = null,
    val partyLeadName: String = "冒険者",
    val partyLeadImageUrl: String = "",
    /** 勉強完了APIに渡す所持キャラID（パーティ先頭・メイン） */
    val partyLeadUserCharacterId: String = "",
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
    val earnedStones: Int = 0,
    /** 「終了する」で勉強パートを切った直後の経過秒。休憩画面上部の冒険結果表示用（休憩タイマーとは別） */
    val completedStudyElapsedSeconds: Long = 0L
)
