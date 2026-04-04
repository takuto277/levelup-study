package org.example.project.features.study

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import org.example.project.domain.repository.PartyRepository

/**
 * 勉強クエスト画面の ViewModel
 *
 * - MutableStateFlow を内部保持、外部には StateFlow のみ公開
 * - UI からの入力は onIntent() に集約
 * - タイマー進行・冒険フェーズ・敵エンカウント・ログ生成すべて ViewModel が管理
 * - セッション完了時に StudyUseCase 経由で Go API に報酬リクエスト送信
 */
class StudyQuestViewModel(
    private val studyUseCase: StudyUseCase? = null,
    private val partyRepository: PartyRepository? = null
) {

    private val _uiState = MutableStateFlow(StudyQuestUiState())
    val uiState: StateFlow<StudyQuestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    // タイマー進行はUIスレッド依存にしない（KMP環境でMain dispatcher未設定でも動くようにする）
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sessionStartedAt: String? = null

    init {
        loadPartyLead()
    }

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
            } catch (_: Exception) {
                // パーティ未編成時はデフォルト値のまま
            }
        }
    }

    // ── 敵データ ──────────────────────────────
    private data class EnemyData(val name: String, val emoji: String, val hp: Int)

    private val enemies = listOf(
        EnemyData("スライム", "🟢", 30),
        EnemyData("ゴブリン", "👺", 50),
        EnemyData("コウモリ", "🦇", 40),
        EnemyData("スケルトン", "💀", 60),
        EnemyData("オーク", "👹", 80),
        EnemyData("ドラゴン", "🐉", 120),
        EnemyData("ダークウィザード", "🧙‍♀️", 100),
        EnemyData("ゴーレム", "🗿", 90),
        EnemyData("キメラ", "🦁", 110),
        EnemyData("デーモン", "😈", 150)
    )

    // ── ログメッセージ ──────────────────────────────
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

    // ── フェーズタイミング ──────────────────────────
    // 歩き→エンカウント→攻撃(繰り返し)→討伐→歩き のサイクル
    private companion object {
        const val WALK_DURATION = 8L       // 歩行フェーズの秒数
        const val ENCOUNTER_DURATION = 2L  // エンカウント演出の秒数
        const val ATTACK_INTERVAL = 3L     // 攻撃間隔（秒）
        const val DEFEAT_DURATION = 3L     // 討伐演出の秒数
    }

    // フェーズ内の経過秒数をトラック
    private var phaseElapsed = 0L

    // ── Intent ハンドラ ─────────────────────────────
    fun onIntent(intent: StudyQuestIntent) {
        when (intent) {
            is StudyQuestIntent.StartQuest -> startQuest(intent.studyMinutes, intent.genreId)
            is StudyQuestIntent.TogglePause -> togglePause()
            is StudyQuestIntent.EndQuest -> endQuest()
            is StudyQuestIntent.NextSession -> nextSession()
            is StudyQuestIntent.StopQuest -> stopQuest()
        }
    }

    // ── 公開ユーティリティ（iOS 側 KMP ブリッジ用に残す） ──
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
            if (state.elapsedSeconds <= targetSeconds) {
                targetSeconds - state.elapsedSeconds
            } else {
                state.elapsedSeconds
            }
        } else {
            (targetSeconds - state.elapsedSeconds).coerceAtLeast(0)
        }
    }

    // ── 内部ロジック ────────────────────────────────

    private fun startQuest(studyMinutes: Int, genreId: String?) {
        if (_uiState.value.status == StudySessionStatus.RUNNING) return

        sessionStartedAt = Clock.System.now().toString()
        val firstEnemy = enemies.random()
        phaseElapsed = 0L

        _uiState.update {
            it.copy(
                type = StudySessionType.STUDY,
                status = StudySessionStatus.RUNNING,
                targetStudyMinutes = studyMinutes,
                elapsedSeconds = 0,
                isOvertime = false,
                currentLog = listOf("冒険を開始した！"),
                displayTime = formatTime(studyMinutes.toLong() * 60),
                genreId = genreId,
                adventurePhase = AdventurePhase.WALKING,
                enemyName = firstEnemy.name,
                enemyEmoji = firstEnemy.emoji,
                enemyHp = firstEnemy.hp,
                enemyMaxHp = firstEnemy.hp,
                lastDamage = 0,
                defeatedCount = 0
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
            else -> { /* READY / FINISHED は何もしない */ }
        }
    }

    private fun endQuest() {
        // 経過時間分の報酬で結果画面へ
        timerJob?.cancel()
        _uiState.update { it.copy(status = StudySessionStatus.FINISHED) }

        // サーバーに勉強セッション完了を送信（UseCase が注入されている場合のみ）
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
            } catch (e: Exception) {
                // オフライン時 → ローカルに保存（TODO: 実装）
                _uiState.update {
                    it.copy(serverSynced = false)
                }
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
                lastDamage = 0
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

    private fun spawnNewEnemy(): EnemyData {
        return enemies.random()
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
                        // 休憩中: RESTINGフェーズ、ログ追加のみ
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
                        // 勉強中: 冒険フェーズ管理
                        advanceAdventurePhase(state, newElapsed, overTime, displaySec)
                    }
                }

                // 休憩の自動終了
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
        var lastDamage = 0
        var defeatedCount = state.defeatedCount

        when (phase) {
            AdventurePhase.WALKING -> {
                if (phaseElapsed >= WALK_DURATION) {
                    // エンカウント!
                    phase = AdventurePhase.ENCOUNTER
                    phaseElapsed = 0L
                    newLogs = (newLogs + "⚠️ ${enemyName}が現れた！").takeLast(4)
                } else if (phaseElapsed % 3 == 0L) {
                    newLogs = (newLogs + walkingMessages.random()).takeLast(4)
                }
            }

            AdventurePhase.ENCOUNTER -> {
                if (phaseElapsed >= ENCOUNTER_DURATION) {
                    // 戦闘開始!
                    phase = AdventurePhase.ATTACKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "⚔️ ${enemyName}に斬りかかった！").takeLast(4)
                }
            }

            AdventurePhase.ATTACKING -> {
                if (phaseElapsed % ATTACK_INTERVAL == 0L) {
                    // ダメージ計算
                    val baseDamage = (10..25).random()
                    val critChance = (1..10).random()
                    val damage = if (critChance >= 9) baseDamage * 2 else baseDamage
                    val isCrit = critChance >= 9

                    enemyHp = (enemyHp - damage).coerceAtLeast(0)
                    lastDamage = damage

                    val dmgMsg = if (isCrit) {
                        "💥 クリティカル！${enemyName}に${damage}のダメージ！"
                    } else {
                        "⚔️ ${enemyName}に${damage}のダメージを与えた！"
                    }
                    newLogs = (newLogs + dmgMsg).takeLast(4)

                    if (enemyHp <= 0) {
                        // 敵を倒した!
                        phase = AdventurePhase.ENEMY_DEFEATED
                        phaseElapsed = 0L
                        defeatedCount++
                        val expGain = enemyMaxHp / 2
                        newLogs = (newLogs + "🎉 ${enemyName}を倒した！ 経験値+${expGain}").takeLast(4)
                    }
                }
            }

            AdventurePhase.ENEMY_DEFEATED -> {
                if (phaseElapsed >= DEFEAT_DURATION) {
                    // 新しい敵を準備して歩き始める
                    val newEnemy = spawnNewEnemy()
                    enemyName = newEnemy.name
                    enemyEmoji = newEnemy.emoji
                    enemyHp = newEnemy.hp
                    enemyMaxHp = newEnemy.hp
                    phase = AdventurePhase.WALKING
                    phaseElapsed = 0L
                    newLogs = (newLogs + "さらに奥へ進んでいく…").takeLast(4)
                }
            }

            AdventurePhase.RESTING -> {
                // 勉強中にRESTINGにはならないが念のため
            }
        }

        return state.copy(
            elapsedSeconds = newElapsed,
            isOvertime = overTime,
            currentLog = newLogs,
            displayTime = formatTime(displaySec),
            adventurePhase = phase,
            enemyName = enemyName,
            enemyEmoji = enemyEmoji,
            enemyHp = enemyHp,
            enemyMaxHp = enemyMaxHp,
            lastDamage = lastDamage,
            defeatedCount = defeatedCount
        )
    }

    fun cleanup() {
        timerJob?.cancel()
        viewModelScope.cancel()
    }
}
