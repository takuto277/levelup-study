package org.example.project.features.study

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import org.example.project.domain.repository.PartyRepository

class StudyQuestViewModel(
    private val studyUseCase: StudyUseCase? = null,
    private val partyRepository: PartyRepository? = null
) {
    private val _uiState = MutableStateFlow(StudyQuestUiState())
    val uiState: StateFlow<StudyQuestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sessionStartedAt: String? = null

    init { loadPartyLead() }

    private fun loadPartyLead() {
        val repo = partyRepository ?: return
        viewModelScope.launch {
            try {
                val party = repo.getParty()
                val lead = party.mainCharacter
                    ?: party.slots.minByOrNull { it.slotPosition }?.userCharacter
                if (lead != null) {
                    _uiState.update {
                        it.copy(
                            partyLeadName = lead.character?.name ?: "冒険者",
                            partyLeadImageUrl = lead.character?.imageUrl ?: ""
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // ── ダンジョン別の敵テーブル ──
    private data class EnemyData(val name: String, val emoji: String, val hp: Int, val atk: Int, val spriteKey: String = "")

    private val defaultEnemies = listOf(
        EnemyData("スライム", "🟢", 30, 5, "slime"),
        EnemyData("ゴブリン", "👺", 50, 8, "goblin"),
        EnemyData("コウモリ", "🦇", 40, 6, "bat"),
        EnemyData("スケルトン", "💀", 60, 10, "skeleton"),
        EnemyData("オーク", "👹", 80, 12, "orc"),
        EnemyData("ゴーレム", "🗿", 90, 14, "golem"),
        EnemyData("ダークウィザード", "🧙‍♀️", 100, 15, "dark_wizard"),
        EnemyData("キメラ", "🦁", 110, 16, "chimera"),
        EnemyData("ドラゴン", "🐉", 120, 18, "dragon"),
        EnemyData("デーモン", "😈", 150, 20, "demon")
    )

    private val forestEnemies = listOf(
        EnemyData("毒キノコ", "🍄", 25, 4, "mushroom"),
        EnemyData("ウルフ", "🐺", 40, 7, "wolf"),
        EnemyData("トレント", "🌲", 70, 9, "treant"),
        EnemyData("フォレストスピリット", "🧚", 55, 6, "forest_spirit"),
        EnemyData("ベアー", "🐻", 90, 13, "bear")
    )

    private val caveEnemies = listOf(
        EnemyData("コウモリ群", "🦇", 35, 5, "bat"),
        EnemyData("クリスタルゴーレム", "💎", 80, 11, "crystal_golem"),
        EnemyData("ケーブスパイダー", "🕷️", 45, 8, "spider"),
        EnemyData("ロックワーム", "🪱", 100, 14, "rock_worm"),
        EnemyData("ミミック", "📦", 70, 10, "mimic")
    )

    private val towerEnemies = listOf(
        EnemyData("フレイムインプ", "🔥", 40, 8, "flame_imp"),
        EnemyData("ファイアエレメンタル", "🌋", 90, 15, "fire_elemental"),
        EnemyData("サラマンダー", "🦎", 60, 10, "salamander"),
        EnemyData("フェニックス", "🐦", 130, 18, "phoenix"),
        EnemyData("イフリート", "😈", 160, 22, "ifrit")
    )

    private fun getEnemiesForDungeon(dungeonName: String?): List<EnemyData> {
        if (dungeonName == null) return defaultEnemies
        return when {
            dungeonName.contains("森") || dungeonName.contains("forest", true) -> forestEnemies
            dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> caveEnemies
            dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> towerEnemies
            dungeonName.contains("迷宮") || dungeonName.contains("コード") -> defaultEnemies.shuffled()
            else -> defaultEnemies
        }
    }

    private var currentEnemyTable: List<EnemyData> = defaultEnemies

    private val walkingMessages = listOf(
        "ダンジョンの奥へ進んでいる…",
        "足音が響く暗い通路を歩いている…",
        "松明の光が揺れている…",
        "遠くで何かの気配がする…",
        "地図を確認しながら前進中…"
    )

    private val breakMessages = listOf(
        "焚き火で休憩中…",
        "ポーションを飲んで体力を回復した",
        "装備のメンテナンスをしている",
        "これまでの冒険を振り返っている",
        "仲間と次の作戦を練っている…"
    )

    private companion object {
        const val WALK_DURATION = 6L
        /** 接近演出＋向かい合い（HP表示）の秒数。UI の接近アニメと同期させる */
        const val ENCOUNTER_DURATION = 5L
        const val ATTACK_INTERVAL = 3L
        const val DEFEAT_DURATION = 2L
        const val DEAD_DURATION = 3L
        const val FLOOR_CLEAR_DURATION = 3L
    }

    private var phaseElapsed = 0L

    fun onIntent(intent: StudyQuestIntent) {
        when (intent) {
            is StudyQuestIntent.StartQuest -> startQuest(intent.studyMinutes, intent.genreId, intent.dungeonName)
            is StudyQuestIntent.TogglePause -> togglePause()
            is StudyQuestIntent.EndQuest -> endQuest()
            is StudyQuestIntent.NextSession -> nextSession()
            is StudyQuestIntent.StopQuest -> stopQuest()
        }
    }

    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        fun Long.pad(): String = if (this < 10) "0$this" else "$this"
        return "${m.pad()}:${s.pad()}"
    }

    fun getDisplaySeconds(state: StudyQuestUiState): Long {
        val targetSeconds = if (state.type == StudySessionType.STUDY)
            state.targetStudyMinutes.toLong() * 60
        else
            state.targetBreakMinutes.toLong() * 60
        return if (state.type == StudySessionType.STUDY) {
            if (state.elapsedSeconds <= targetSeconds) targetSeconds - state.elapsedSeconds
            else state.elapsedSeconds
        } else {
            (targetSeconds - state.elapsedSeconds).coerceAtLeast(0)
        }
    }

    private fun startQuest(studyMinutes: Int, genreId: String?, dungeonName: String? = null) {
        if (_uiState.value.status == StudySessionStatus.RUNNING) return

        sessionStartedAt = Clock.System.now().toString()
        currentEnemyTable = getEnemiesForDungeon(dungeonName)
        val firstEnemy = currentEnemyTable.random()
        phaseElapsed = 0L

        _uiState.update {
            it.copy(
                type = StudySessionType.STUDY,
                status = StudySessionStatus.RUNNING,
                targetStudyMinutes = studyMinutes,
                elapsedSeconds = 0,
                isOvertime = false,
                currentLog = listOf(
                    if (dungeonName != null) "「${dungeonName}」1F の探索を開始した！" else "冒険を開始した！"
                ),
                displayTime = formatTime(studyMinutes.toLong() * 60),
                genreId = genreId,
                adventurePhase = AdventurePhase.WALKING,
                adventurePhaseTick = 0L,
                enemyName = firstEnemy.name,
                enemyEmoji = firstEnemy.emoji,
                enemySpriteKey = firstEnemy.spriteKey,
                enemyHp = firstEnemy.hp,
                enemyMaxHp = firstEnemy.hp,
                lastDamage = 0,
                lastPlayerDamage = 0,
                defeatedCount = 0,
                dungeonName = dungeonName,
                currentFloor = 1,
                totalFloors = 10,
                floorClearCount = 0,
                playerHp = 100,
                playerMaxHp = 100,
                earnedXp = 0,
                earnedStones = 0
            )
        }
        startTimer()
    }

    private fun togglePause() {
        val current = _uiState.value
        when (current.status) {
            StudySessionStatus.RUNNING -> {
                timerJob?.cancel()
                _uiState.update { it.copy(status = StudySessionStatus.PAUSED) }
            }
            StudySessionStatus.PAUSED -> {
                _uiState.update { it.copy(status = StudySessionStatus.RUNNING) }
                startTimer()
            }
            else -> { }
        }
    }

    private fun endQuest() {
        timerJob?.cancel()
        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }

        val useCase = studyUseCase ?: return
        val state = _uiState.value
        val endedAt = Clock.System.now().toString()
        viewModelScope.launch {
            try {
                val result = useCase.completeSession(
                    category = state.genreId,
                    startedAt = sessionStartedAt ?: endedAt,
                    endedAt = endedAt,
                    durationSeconds = state.elapsedSeconds.toInt(),
                    isCompleted = state.elapsedSeconds >= state.targetStudyMinutes.toLong() * 60
                )
                _uiState.update {
                    it.copy(
                        serverRewards = result.rewards.map { r ->
                            "${r.rewardType.name}: +${r.amount}"
                        },
                        serverSynced = true
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(serverSynced = false) }
            }
        }
    }

    private fun nextSession() {
        val current = _uiState.value
        val newType = if (current.type == StudySessionType.STUDY)
            StudySessionType.BREAK else StudySessionType.STUDY

        val targetSec = if (newType == StudySessionType.STUDY)
            current.targetStudyMinutes.toLong() * 60
        else
            current.targetBreakMinutes.toLong() * 60

        phaseElapsed = 0L

        _uiState.update {
            it.copy(
                type = newType,
                status = StudySessionStatus.RUNNING,
                elapsedSeconds = 0,
                isOvertime = false,
                currentLog = listOf(
                    if (newType == StudySessionType.BREAK) "休憩を開始した。" else "次の冒険へ出発だ！"
                ),
                displayTime = formatTime(targetSec),
                adventurePhase = if (newType == StudySessionType.BREAK) AdventurePhase.RESTING else AdventurePhase.WALKING,
                adventurePhaseTick = 0L,
                lastDamage = 0,
                lastPlayerDamage = 0,
                currentFloor = if (newType == StudySessionType.STUDY) 1 else it.currentFloor,
                playerHp = if (newType == StudySessionType.STUDY) it.playerMaxHp else it.playerHp
            )
        }
        startTimer()
    }

    private fun stopQuest() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            StudyQuestUiState(
                status = StudySessionStatus.READY,
                displayTime = formatTime(it.targetStudyMinutes.toLong() * 60)
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    val targetSeconds = if (state.type == StudySessionType.STUDY)
                        state.targetStudyMinutes.toLong() * 60
                    else
                        state.targetBreakMinutes.toLong() * 60

                    val newElapsed = state.elapsedSeconds + 1
                    phaseElapsed++

                    val overTime = state.type == StudySessionType.STUDY && newElapsed > targetSeconds

                    val displaySec = if (state.type == StudySessionType.STUDY) {
                        if (newElapsed <= targetSeconds) targetSeconds - newElapsed else newElapsed
                    } else {
                        (targetSeconds - newElapsed).coerceAtLeast(0)
                    }

                    if (state.type == StudySessionType.BREAK) {
                        var newLogs = state.currentLog
                        if (newElapsed % 15 == 0L) {
                            newLogs = (newLogs + breakMessages.random()).takeLast(4)
                        }
                        state.copy(
                            elapsedSeconds = newElapsed,
                            isOvertime = false,
                            currentLog = newLogs,
                            displayTime = formatTime(displaySec),
                            adventurePhase = AdventurePhase.RESTING
                        )
                    } else {
                        advanceAdventurePhase(state, newElapsed, overTime, displaySec)
                    }
                }

                val state = _uiState.value
                if (state.type == StudySessionType.BREAK) {
                    val targetSeconds = state.targetBreakMinutes.toLong() * 60
                    if (state.elapsedSeconds >= targetSeconds) {
                        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }
                        break
                    }
                }
            }
        }
    }

    private fun advanceAdventurePhase(
        state: StudyQuestUiState,
        newElapsed: Long,
        overTime: Boolean,
        displaySec: Long
    ): StudyQuestUiState {
        var newLogs = state.currentLog
        var phase = state.adventurePhase
        var enemyHp = state.enemyHp
        var enemyMaxHp = state.enemyMaxHp
        var enemyName = state.enemyName
        var enemyEmoji = state.enemyEmoji
        var enemySpriteKey = state.enemySpriteKey
        var lastDamage = 0
        var lastPlayerDamage = 0
        var defeatedCount = state.defeatedCount
        var playerHp = state.playerHp
        var currentFloor = state.currentFloor
        var floorClearCount = state.floorClearCount
        var earnedXp = state.earnedXp
        var earnedStones = state.earnedStones

        when (phase) {
            AdventurePhase.WALKING -> {
                if (phaseElapsed >= WALK_DURATION) {
                    phase = AdventurePhase.ENCOUNTER
                    phaseElapsed = 0L
                    newLogs = (newLogs + "⚠️ ${currentFloor}F: ${enemyName}が現れた！").takeLast(5)
                } else if (phaseElapsed % 3 == 0L) {
                    newLogs = (newLogs + walkingMessages.random()).takeLast(5)
                }
            }

            AdventurePhase.ENCOUNTER -> {
                if (phaseElapsed >= ENCOUNTER_DURATION) {
                    phase = AdventurePhase.ATTACKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "⚔️ ${enemyName}に斬りかかった！").takeLast(5)
                }
            }

            AdventurePhase.ATTACKING -> {
                if (phaseElapsed % ATTACK_INTERVAL == 0L) {
                    // プレイヤーの攻撃
                    val baseDmg = (10..25).random()
                    val critRoll = (1..10).random()
                    val dmg = if (critRoll >= 9) baseDmg * 2 else baseDmg
                    val isCrit = critRoll >= 9
                    enemyHp = (enemyHp - dmg).coerceAtLeast(0)
                    lastDamage = dmg

                    val dmgMsg = if (isCrit) "💥 クリティカル！${enemyName}に${dmg}ダメージ！"
                    else "⚔️ ${enemyName}に${dmg}ダメージ！"
                    newLogs = (newLogs + dmgMsg).takeLast(5)

                    if (enemyHp <= 0) {
                        phase = AdventurePhase.ENEMY_DEFEATED
                        phaseElapsed = 0L
                        defeatedCount++
                        val xpGain = enemyMaxHp / 2
                        earnedXp += xpGain
                        newLogs = (newLogs + "🎉 ${enemyName}を倒した！ EXP+${xpGain}").takeLast(5)
                    } else {
                        // 敵の反撃
                        val enemyData = currentEnemyTable.find { it.name == enemyName }
                        val enemyAtk = enemyData?.atk ?: 8
                        val enemyDmg = (enemyAtk - 2..enemyAtk + 3).random().coerceAtLeast(1)
                        playerHp = (playerHp - enemyDmg).coerceAtLeast(0)
                        lastPlayerDamage = enemyDmg
                        newLogs = (newLogs + "🔻 ${enemyName}の反撃！ ${enemyDmg}ダメージ！").takeLast(5)

                        if (playerHp <= 0) {
                            phase = AdventurePhase.PLAYER_DEAD
                            phaseElapsed = 0L
                            newLogs = (newLogs + "💀 力尽きた…1Fからやり直し！").takeLast(5)
                        }
                    }
                }
            }

            AdventurePhase.ENEMY_DEFEATED -> {
                if (phaseElapsed >= DEFEAT_DURATION) {
                    // 1フロアにつき敵1体 → 倒したら階層クリア判定
                    if (currentFloor >= state.totalFloors) {
                        // 全階層クリア！
                        phase = AdventurePhase.FLOOR_CLEAR
                        phaseElapsed = 0L
                        floorClearCount++
                        val bonusStones = 5
                        earnedStones += bonusStones
                        newLogs = (newLogs + "🏆 全${state.totalFloors}F制覇！ 💎+${bonusStones} 1Fから再挑戦！").takeLast(5)
                    } else {
                        currentFloor++
                        val newEnemy = currentEnemyTable.random()
                        enemyName = newEnemy.name
                        enemyEmoji = newEnemy.emoji
                        enemySpriteKey = newEnemy.spriteKey
                        enemyHp = newEnemy.hp
                        enemyMaxHp = newEnemy.hp
                        phase = AdventurePhase.WALKING
                        phaseElapsed = 0L
                        newLogs = (newLogs + "📍 ${currentFloor}Fへ進んだ…").takeLast(5)
                    }
                }
            }

            AdventurePhase.FLOOR_CLEAR -> {
                if (phaseElapsed >= FLOOR_CLEAR_DURATION) {
                    currentFloor = 1
                    playerHp = state.playerMaxHp
                    val newEnemy = currentEnemyTable.random()
                    enemyName = newEnemy.name
                    enemyEmoji = newEnemy.emoji
                    enemySpriteKey = newEnemy.spriteKey
                    enemyHp = newEnemy.hp
                    enemyMaxHp = newEnemy.hp
                    phase = AdventurePhase.WALKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "1Fから再スタート！ HPも全回復した！").takeLast(5)
                }
            }

            AdventurePhase.PLAYER_DEAD -> {
                if (phaseElapsed >= DEAD_DURATION) {
                    currentFloor = 1
                    playerHp = state.playerMaxHp
                    val newEnemy = currentEnemyTable.random()
                    enemyName = newEnemy.name
                    enemyEmoji = newEnemy.emoji
                    enemySpriteKey = newEnemy.spriteKey
                    enemyHp = newEnemy.hp
                    enemyMaxHp = newEnemy.hp
                    phase = AdventurePhase.WALKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "復活！ 1Fから再挑戦だ！").takeLast(5)
                }
            }

            AdventurePhase.RESTING -> { }
        }

        return state.copy(
            elapsedSeconds = newElapsed,
            isOvertime = overTime,
            currentLog = newLogs,
            displayTime = formatTime(displaySec),
            adventurePhase = phase,
            adventurePhaseTick = phaseElapsed,
            enemyName = enemyName,
            enemyEmoji = enemyEmoji,
            enemySpriteKey = enemySpriteKey,
            enemyHp = enemyHp,
            enemyMaxHp = enemyMaxHp,
            lastDamage = lastDamage,
            lastPlayerDamage = lastPlayerDamage,
            defeatedCount = defeatedCount,
            playerHp = playerHp,
            currentFloor = currentFloor,
            floorClearCount = floorClearCount,
            earnedXp = earnedXp,
            earnedStones = earnedStones
        )
    }

    fun cleanup() {
        timerJob?.cancel()
        viewModelScope.cancel()
    }
}
