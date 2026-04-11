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

    /** 全スプライトキーに対応するマスター一覧（`EnemySpriteAssets` の bundled と一致） */
    private val enemyCatalog: List<EnemyData> = listOf(
        EnemyData("スライム", "🟢", 30, 5, "slime"),
        EnemyData("ゴブリン", "👺", 50, 8, "goblin"),
        EnemyData("コウモリ", "🦇", 40, 6, "bat"),
        EnemyData("スケルトン", "💀", 60, 10, "skeleton"),
        EnemyData("ドラゴン", "🐉", 120, 18, "dragon"),
        EnemyData("オーク", "👹", 80, 12, "orc"),
        EnemyData("ゴーレム", "🗿", 95, 14, "golem"),
        EnemyData("リッチ", "🧙", 110, 16, "lich"),
        EnemyData("ワイバーン", "🐲", 100, 15, "wyvern"),
        EnemyData("ミノタウロス", "🐂", 105, 15, "minotaur"),
        EnemyData("マンドレイク", "🌱", 35, 5, "mandrake"),
        EnemyData("グリフォン", "🦅", 115, 16, "griffin"),
        EnemyData("バジリスク", "🐍", 90, 13, "basilisk"),
        EnemyData("ケルベロス", "🐕‍🦺", 125, 17, "cerberus"),
        EnemyData("クラーケン", "🐙", 140, 17, "kraken"),
        EnemyData("ヒドラ", "🐉", 135, 17, "hydra"),
        EnemyData("リバイアサン", "🐋", 145, 18, "leviathan"),
        EnemyData("バンシー", "👻", 70, 10, "banshee"),
        EnemyData("ウィスプ", "✨", 45, 6, "wisp"),
        EnemyData("インプ", "😈", 42, 7, "imp"),
        EnemyData("サキュバス", "💜", 88, 12, "succubus"),
        EnemyData("デュラハン", "🐴", 100, 14, "dullahan"),
        EnemyData("ガーゴイル", "🗿", 85, 12, "gargoyle"),
        EnemyData("スペクター", "👻", 65, 9, "specter"),
        EnemyData("マミー", "🤕", 92, 12, "mummy"),
        EnemyData("トロール", "👹", 110, 14, "troll"),
        EnemyData("コボルド", "🦎", 48, 7, "kobold"),
        EnemyData("ラミア", "🐍", 95, 13, "lamia"),
        EnemyData("クリムゾンデーモン", "🔥", 155, 20, "crimson_demon"),
        EnemyData("シャドウナイト", "🗡️", 118, 15, "shadow_knight"),
        EnemyData("ハーピー", "🪶", 72, 10, "harpy"),
        EnemyData("ケンタウロス", "🏹", 108, 14, "centaur"),
        EnemyData("ドライアド", "🌳", 68, 9, "dryad"),
        EnemyData("ナーガ", "🐍", 98, 13, "naga"),
        EnemyData("オーガ", "👹", 115, 15, "ogre"),
        EnemyData("サイクロプス", "👁️", 120, 16, "cyclops"),
        EnemyData("グール", "🧟", 78, 11, "ghoul"),
        EnemyData("レイス", "💀", 82, 11, "wraith"),
        EnemyData("フロストジャイアント", "❄️", 130, 16, "frost_giant"),
        EnemyData("サンドワーム", "🪱", 112, 14, "sand_worm"),
        EnemyData("コカトリス", "🐔", 76, 11, "cockatrice"),
        EnemyData("ワーウルフ", "🐺", 96, 13, "werewolf"),
        EnemyData("ヴァンパイア", "🧛", 104, 14, "vampire"),
        EnemyData("ゾンビ", "🧟‍♂️", 58, 8, "zombie"),
        EnemyData("リザードマン", "🦎", 74, 10, "lizardman"),
        EnemyData("ダークナイト", "⚔️", 122, 15, "dark_knight"),
        EnemyData("ラストモンスター", "🦀", 66, 9, "rust_monster"),
        EnemyData("ボーンドラゴン", "🦴", 128, 17, "bone_dragon"),
        EnemyData("ナイトメア", "🐴", 106, 14, "nightmare"),
        EnemyData("海賊の亡霊", "🏴‍☠️", 90, 12, "pirate_wraith"),
        EnemyData("石の番人", "🗿", 88, 11, "stone_sentinel"),
        EnemyData("サンダーバード", "⚡", 118, 15, "thunderbird"),
        EnemyData("フェニックス", "🐦", 132, 18, "phoenix"),
        EnemyData("巨大グモ", "🕷️", 86, 11, "giant_spider"),
        EnemyData("アイスウィッチ", "🧊", 102, 14, "ice_witch"),
        EnemyData("堕落した聖騎士", "⚔️", 124, 15, "corrupted_paladin"),
        EnemyData("深淵の海蛇", "🐍", 138, 17, "abyssal_serpent"),
        EnemyData("トレント", "🌲", 100, 12, "treant"),
        EnemyData("キメラ", "🦁", 112, 16, "chimera"),
        EnemyData("ミミック", "📦", 80, 11, "mimic")
    )

    private fun poolBySpriteKeys(keys: Set<String>): List<EnemyData> {
        val picked = enemyCatalog.filter { it.spriteKey in keys }
        return picked.ifEmpty { enemyCatalog }
    }

    private val defaultEnemyPool = poolBySpriteKeys(
        setOf(
            "slime", "goblin", "bat", "skeleton", "orc", "troll", "ogre", "kobold", "zombie", "ghoul",
            "cyclops", "minotaur", "centaur", "dark_knight", "corrupted_paladin", "mummy", "mimic",
            "mandrake", "chimera", "shadow_knight", "dullahan", "vampire", "lizardman", "werewolf",
            "rust_monster", "bone_dragon", "dragon", "gargoyle", "harpy", "nightmare"
        )
    )

    private val forestEnemyPool = poolBySpriteKeys(
        setOf(
            "dryad", "treant", "mandrake", "harpy", "werewolf", "chimera", "griffin", "naga", "cockatrice",
            "basilisk", "giant_spider", "wisp", "wyvern", "banshee", "lamia", "centaur", "ogre", "troll"
        )
    )

    private val caveEnemyPool = poolBySpriteKeys(
        setOf(
            "golem", "kobold", "rust_monster", "gargoyle", "specter", "skeleton", "mimic", "giant_spider",
            "wraith", "banshee", "dullahan", "stone_sentinel", "sand_worm", "ghoul", "mummy", "bat",
            "lich", "zombie", "cyclops"
        )
    )

    private val towerEnemyPool = poolBySpriteKeys(
        setOf(
            "dragon", "phoenix", "crimson_demon", "imp", "nightmare", "ice_witch", "succubus",
            "shadow_knight", "wisp", "bone_dragon", "banshee", "hydra", "leviathan", "thunderbird",
            "lamia", "wyvern", "cerberus", "lich", "specter"
        )
    )

    private fun getEnemiesForDungeon(dungeonName: String?): List<EnemyData> {
        if (dungeonName == null) return defaultEnemyPool
        return when {
            dungeonName.contains("森") || dungeonName.contains("forest", true) -> forestEnemyPool
            dungeonName.contains("洞窟") || dungeonName.contains("水晶") || dungeonName.contains("cave", true) -> caveEnemyPool
            dungeonName.contains("塔") || dungeonName.contains("炎") || dungeonName.contains("tower", true) -> towerEnemyPool
            dungeonName.contains("迷宮") || dungeonName.contains("コード") -> enemyCatalog.shuffled()
            else -> defaultEnemyPool
        }
    }

    private var currentEnemyTable: List<EnemyData> = defaultEnemyPool

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
        const val DEAD_DURATION = 3L
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
                enemySpriteKey = EnemySpriteAssets.drawableKey(firstEnemy.spriteKey),
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
                        defeatedCount++
                        val xpGain = enemyMaxHp / 2
                        earnedXp += xpGain
                        newLogs = (newLogs + "🎉 ${enemyName}を倒した！ EXP+${xpGain}").takeLast(5)
                        lastDamage = 0
                        lastPlayerDamage = 0
                        if (currentFloor >= state.totalFloors) {
                            floorClearCount++
                            earnedStones += 5
                            currentFloor = 1
                            playerHp = state.playerMaxHp
                            newLogs = (newLogs + "🏆 全${state.totalFloors}F制覇！ 💎+5 1Fから再挑戦！").takeLast(5)
                        } else {
                            currentFloor++
                            newLogs = (newLogs + "📍 ${currentFloor}Fへ進んだ…").takeLast(5)
                        }
                        val newEnemy = currentEnemyTable.random()
                        enemyName = newEnemy.name
                        enemyEmoji = newEnemy.emoji
                        enemySpriteKey = EnemySpriteAssets.drawableKey(newEnemy.spriteKey)
                        enemyHp = newEnemy.hp
                        enemyMaxHp = newEnemy.hp
                        phase = AdventurePhase.WALKING
                        phaseElapsed = 0L
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

            AdventurePhase.PLAYER_DEAD -> {
                if (phaseElapsed >= DEAD_DURATION) {
                    currentFloor = 1
                    playerHp = state.playerMaxHp
                    val newEnemy = currentEnemyTable.random()
                    enemyName = newEnemy.name
                    enemyEmoji = newEnemy.emoji
                    enemySpriteKey = EnemySpriteAssets.drawableKey(newEnemy.spriteKey)
                    enemyHp = newEnemy.hp
                    enemyMaxHp = newEnemy.hp
                    phase = AdventurePhase.WALKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "復活！ 1Fから再挑戦だ！").takeLast(5)
                }
            }

            AdventurePhase.RESTING -> { }

            AdventurePhase.ENEMY_DEFEATED,
            AdventurePhase.FLOOR_CLEAR -> { }
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
