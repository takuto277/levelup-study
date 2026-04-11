package org.example.project.features.study

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.example.project.components.BattleSprite
import org.example.project.components.DungeonBackground
import org.example.project.components.PlayerSprite
import org.example.project.components.PlayerSpriteMode
import org.example.project.components.hasBackgroundResource
import org.example.project.components.hasPlayerWalkSprite
import org.example.project.components.hasSpriteResource

private val DarkBg = Color(0xFF0F172A)
private val DarkCard = Color(0xFF1E293B)
private val DarkSurface = Color(0xFF334155)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val FireRed = Color(0xFFEF4444)
private val FireOrange = Color(0xFFF59E0B)
private val EmeraldGreen = Color(0xFF10B981)
private val PurpleGlow = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val DamageRed = Color(0xFFFF4444)
private val BreakBg = Color(0xFF0C1E0C)
private val BreakCard = Color(0xFF1A2E1A)
private val BreakAccent = Color(0xFF34D399)
private val BreakGlow = Color(0xFF10B981)

@Composable
fun StudyQuestScreenView(
    initialStudyMinutes: Int,
    genreId: String? = null,
    dungeonName: String? = null,
    onDismiss: () -> Unit
) {
    val viewModel = remember { StudyQuestViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(StudyQuestIntent.StartQuest(initialStudyMinutes, genreId, dungeonName))
    }

    val isBreak = uiState.type == StudySessionType.BREAK
    val bgColor = if (isBreak) BreakBg else DarkBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AnimatedContent(
            targetState = uiState.status == StudySessionStatus.FINISHED,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
            label = "screen_transition"
        ) { isFinished ->
            if (isFinished) {
                ResultScreen(
                    uiState = uiState,
                    onNext = { viewModel.onIntent(StudyQuestIntent.NextSession) },
                    onExit = {
                        viewModel.onIntent(StudyQuestIntent.StopQuest)
                        onDismiss()
                    }
                )
            } else {
                MainQuestView(
                    uiState = uiState,
                    onTogglePause = { viewModel.onIntent(StudyQuestIntent.TogglePause) },
                    onEndQuest = { viewModel.onIntent(StudyQuestIntent.EndQuest) }
                )
            }
        }
    }
}

@Composable
private fun MainQuestView(
    uiState: StudyQuestUiState,
    onTogglePause: () -> Unit,
    onEndQuest: () -> Unit
) {
    val isBreak = uiState.type == StudySessionType.BREAK
    val isOvertime = uiState.isOvertime
    val phase = uiState.adventurePhase

    val infiniteTransition = rememberInfiniteTransition(label = "quest")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse"
    )
    val walkOffset by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "walk"
    )
    val walkBounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "walkBounce"
    )
    val restFlicker by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker"
    )

    val targetSeconds = if (isBreak) uiState.targetBreakMinutes * 60 else uiState.targetStudyMinutes * 60
    val progress = if (targetSeconds > 0 && !isOvertime) {
        (uiState.elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    } else if (isOvertime) 1f else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                uiState.dungeonName?.takeIf { it.isNotEmpty() }?.let { dn ->
                    Box(
                        modifier = Modifier
                            .background(FireOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("🏰 $dn", fontSize = 11.sp, fontWeight = FontWeight.Black, color = FireOrange)
                    }
                }
                Box(
                    modifier = Modifier
                        .background(AccentIndigo.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("📖 " + (uiState.genreId ?: "総合"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        when {
                            isOvertime -> PurpleGlow.copy(alpha = 0.2f)
                            isBreak -> BreakAccent.copy(alpha = 0.15f)
                            phase == AdventurePhase.ATTACKING -> FireRed.copy(alpha = 0.2f)
                            phase == AdventurePhase.ENCOUNTER -> FireOrange.copy(alpha = 0.2f)
                            else -> AccentBlue.copy(alpha = 0.15f)
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isOvertime -> "⚡ 限界突破中"
                        uiState.status == StudySessionStatus.PAUSED -> "⏸ 一時停止"
                        isBreak -> "🏕️ 休憩中"
                        phase == AdventurePhase.WALKING -> "🚶 探索中"
                        phase == AdventurePhase.ENCOUNTER -> "⚠️ エンカウント！"
                        phase == AdventurePhase.ATTACKING -> "⚔️ 戦闘中"
                        phase == AdventurePhase.ENEMY_DEFEATED -> "🎉 討伐完了！"
                        else -> "⚔️ 冒険中"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isOvertime -> PurpleGlow
                        isBreak -> BreakAccent
                        phase == AdventurePhase.ATTACKING -> FireRed
                        phase == AdventurePhase.ENCOUNTER -> FireOrange
                        else -> AccentBlue
                    }
                )
            }
            Box(
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("📍 ${uiState.currentFloor}F/${uiState.totalFloors}F", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("💀 ${uiState.defeatedCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            }
        }

        // プレイヤーHPバー
        if (!isBreak) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧙‍♂️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text("${uiState.playerHp}/${uiState.playerMaxHp}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(DarkSurface)
                    ) {
                        val ratio = if (uiState.playerMaxHp > 0) uiState.playerHp.toFloat() / uiState.playerMaxHp else 0f
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(ratio).background(
                                when {
                                    ratio > 0.5f -> EmeraldGreen
                                    ratio > 0.25f -> FireOrange
                                    else -> FireRed
                                }, RoundedCornerShape(3.dp)
                            )
                        )
                    }
                }
                if (uiState.lastPlayerDamage > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("-${uiState.lastPlayerDamage}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FireRed)
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isBreak) Brush.verticalGradient(listOf(Color(0xFF0A1F0A), Color(0xFF132613), Color(0xFF0A1F0A)))
                    else Brush.verticalGradient(listOf(Color(0xFF06060F), Color(0xFF0E1428), Color(0xFF1A1040)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isBreak) {
                val context = LocalContext.current
                val hasBgRes = remember(uiState.dungeonName) { hasBackgroundResource(context, uiState.dungeonName) }
                if (!hasBgRes) {
                    Column(modifier = Modifier.matchParentSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth().height(30.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFF2D1B0E), Color(0xFF1A1005)))))
                    }
                }
            }
            if (isBreak) {
                BreakScene(restFlicker)
            } else {
                AdventureScene(
                    phase = phase,
                    enemyEmoji = uiState.enemyEmoji,
                    enemyName = uiState.enemyName,
                    enemySpriteKey = uiState.enemySpriteKey,
                    enemyHp = uiState.enemyHp,
                    enemyMaxHp = uiState.enemyMaxHp,
                    lastDamage = uiState.lastDamage,
                    dungeonName = uiState.dungeonName,
                    currentFloor = uiState.currentFloor,
                    adventurePhaseTick = uiState.adventurePhaseTick,
                    walkOffset = walkOffset,
                    walkBounce = walkBounce,
                    pulseAlpha = pulseAlpha
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    val glowColor = when {
                        isOvertime -> PurpleGlow
                        isBreak -> BreakGlow
                        else -> AccentBlue
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4f
                        val r = (size.minDimension - strokeW) / 2
                        val c = Offset(size.width / 2, size.height / 2)
                        val tl = Offset(c.x - r, c.y - r)
                        val arcSize = Size(r * 2, r * 2)
                        drawArc(
                            color = glowColor.copy(alpha = 0.15f),
                            startAngle = 0f, sweepAngle = 360f,
                            useCenter = false, topLeft = tl, size = arcSize,
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = glowColor,
                            startAngle = -90f, sweepAngle = 360f * progress,
                            useCenter = false, topLeft = tl, size = arcSize,
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = uiState.displayTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        isOvertime -> PurpleGlow
                        isBreak -> BreakAccent
                        else -> TextWhite
                    }
                )
            }

            if (isOvertime) {
                Text(
                    "延長戦！",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow.copy(alpha = pulseAlpha),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 直近ログ（1行のみ）
        uiState.currentLog.lastOrNull()?.let { lastLog ->
            Text(
                text = lastLog,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBreak) BreakAccent else AccentBlue,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (isBreak) BreakCard else DarkCard).copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTogglePause,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.status == StudySessionStatus.RUNNING)
                        DarkSurface else AccentBlue
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (uiState.status == StudySessionStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (uiState.status == StudySessionStatus.RUNNING) "一時停止" else "再開",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
            }

            Button(
                onClick = onEndQuest,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("🏁 終了する", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

private val ConfrontGap = 30.dp
private val ConfrontPlayerSize = 108.dp
private val ConfrontEnemySize = 112.dp

/**
 * 背景を下基準でクロップしたときの「床帯」に足を乗せるための下端オフセット。
 * 大きいほどキャラが画面上に上がる（以前の 44dp は床から浮いていた）。
 */
private val AdventureFloorInsetDp = 8.dp


/** 戦闘中: 1秒目 idle → 2秒目 prep → 3秒目 attack（ATTACK_INTERVAL と同期） */
private fun combatPlayerMode(phaseTick: Long, lastDamage: Int): PlayerSpriteMode =
    when (phaseTick % 3L) {
        0L -> if (lastDamage > 0) PlayerSpriteMode.Attack else PlayerSpriteMode.Idle
        1L -> PlayerSpriteMode.Idle
        2L -> PlayerSpriteMode.Prep
        else -> PlayerSpriteMode.Idle
    }

/** 遭遇〜戦闘：左右から中央へ接近。接近中のみ敵が上下に揺れる。戦闘中は idle→prep→attack、上下運動なし */
@Composable
private fun BattleConfrontationLayer(
    isAttackPhase: Boolean,
    approachProgress: Float,
    syncBob: Boolean,
    adventurePhaseTick: Long,
    hasPlayerSprite: Boolean,
    hasEnemySprite: Boolean,
    enemySpriteKey: String,
    enemyEmoji: String,
    enemyName: String,
    enemyHp: Int,
    enemyMaxHp: Int,
    lastDamage: Int
) {
    val isStriking = isAttackPhase && lastDamage > 0
    val showEnemyHp = isAttackPhase || approachProgress >= 0.9f
    val enemyYOffset = if (!isAttackPhase) {
        if (syncBob) (-3).dp else 2.dp
    } else {
        0.dp
    }
    val progress = approachProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
    ) {
        Text(
            if (isAttackPhase) "⚔️ 戦闘中" else "⚠️ 遭遇！",
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )

        if (isStriking) {
            Text(
                "-$lastDamage",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DamageRed,
                modifier = Modifier.align(Alignment.Center).offset(y = (-42).dp)
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val centerX = maxWidth / 2
            val playerEndLeft = centerX - ConfrontGap / 2 - ConfrontPlayerSize
            val enemyEndLeft = centerX + ConfrontGap / 2
            val playerStartLeft = (-32).dp
            val enemyStartLeft = maxWidth + 88.dp
            val playerLeft = lerp(playerStartLeft, playerEndLeft, progress)
            val enemyLeft = lerp(enemyStartLeft, enemyEndLeft, progress)

            if (hasPlayerSprite) {
                PlayerSprite(
                    mode = when {
                        !isAttackPhase -> PlayerSpriteMode.Walking
                        else -> combatPlayerMode(adventurePhaseTick, lastDamage)
                    },
                    size = ConfrontPlayerSize,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = playerLeft + if (isAttackPhase && isStriking) 6.dp else 0.dp,
                            y = -AdventureFloorInsetDp
                        )
                )
            } else {
                Text(
                    "🧙‍♂️",
                    fontSize = 52.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = playerLeft, y = -AdventureFloorInsetDp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = enemyLeft, y = -AdventureFloorInsetDp + enemyYOffset)
                    .widthIn(min = 80.dp)
            ) {
                if (hasEnemySprite) {
                    BattleSprite(
                        spriteKey = enemySpriteKey,
                        spriteType = "enemy",
                        size = ConfrontEnemySize,
                        animateFrames = false,
                        modifier = Modifier.offset(x = 0.dp)
                    )
                } else {
                    Text(enemyEmoji, fontSize = 56.sp)
                }
                if (showEnemyHp) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        enemyName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DarkSurface)
                    ) {
                        val hpRatio = if (enemyMaxHp > 0) enemyHp.toFloat() / enemyMaxHp else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpRatio)
                                .background(
                                    when {
                                        hpRatio > 0.5f -> EmeraldGreen
                                        hpRatio > 0.25f -> FireOrange
                                        else -> FireRed
                                    },
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Text(
                        "$enemyHp/$enemyMaxHp",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        enemyName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange
                    )
                    Text(
                        "接近中…",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AdventureScene(
    phase: AdventurePhase,
    enemyEmoji: String,
    enemyName: String,
    enemySpriteKey: String,
    enemyHp: Int,
    enemyMaxHp: Int,
    lastDamage: Int,
    dungeonName: String?,
    currentFloor: Int,
    adventurePhaseTick: Long,
    walkOffset: Float,
    walkBounce: Float,
    pulseAlpha: Float
) {
    val context = LocalContext.current
    val hasPlayerSprite = remember { hasPlayerWalkSprite(context) }
    val hasEnemySprite = remember(enemySpriteKey) { hasSpriteResource(context, "enemy", enemySpriteKey) }
    val hasBg = remember(dungeonName) { hasBackgroundResource(context, dungeonName) }

    val encounterKey = remember(enemySpriteKey, enemyMaxHp, currentFloor) {
        "${enemySpriteKey}_${enemyMaxHp}_$currentFloor"
    }

    var approachProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(phase, encounterKey) {
        when (phase) {
            AdventurePhase.ENCOUNTER -> {
                approachProgress = 0f
                val durationMs = 2800L
                val start = System.currentTimeMillis()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed >= durationMs) {
                        approachProgress = 1f
                        break
                    }
                    val t = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    approachProgress = FastOutSlowInEasing.transform(t)
                    delay(16)
                }
            }
            AdventurePhase.ATTACKING -> {
                approachProgress = 1f
            }
            else -> { }
        }
    }

    var syncBob by remember { mutableStateOf(false) }
    LaunchedEffect(phase) {
        if (phase != AdventurePhase.ENCOUNTER) return@LaunchedEffect
        while (isActive) {
            delay(320)
            syncBob = !syncBob
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasBg) {
            DungeonBackground(
                dungeonName = dungeonName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.82f
            )
        }

        when (phase) {
            AdventurePhase.WALKING -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "探索中…",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    )
                    if (hasPlayerSprite) {
                        PlayerSprite(
                            mode = PlayerSpriteMode.Walking,
                            size = 118.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = AdventureFloorInsetDp)
                        )
                    } else {
                        Text(
                            "🧙‍♂️",
                            fontSize = 56.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = AdventureFloorInsetDp)
                        )
                    }
                }
            }

            AdventurePhase.ENCOUNTER -> {
                BattleConfrontationLayer(
                    isAttackPhase = false,
                    approachProgress = approachProgress,
                    syncBob = syncBob,
                    adventurePhaseTick = adventurePhaseTick,
                    hasPlayerSprite = hasPlayerSprite,
                    hasEnemySprite = hasEnemySprite,
                    enemySpriteKey = enemySpriteKey,
                    enemyEmoji = enemyEmoji,
                    enemyName = enemyName,
                    enemyHp = enemyHp,
                    enemyMaxHp = enemyMaxHp,
                    lastDamage = lastDamage
                )
            }

            AdventurePhase.ATTACKING -> {
                BattleConfrontationLayer(
                    isAttackPhase = true,
                    approachProgress = approachProgress,
                    syncBob = syncBob,
                    adventurePhaseTick = adventurePhaseTick,
                    hasPlayerSprite = hasPlayerSprite,
                    hasEnemySprite = hasEnemySprite,
                    enemySpriteKey = enemySpriteKey,
                    enemyEmoji = enemyEmoji,
                    enemyName = enemyName,
                    enemyHp = enemyHp,
                    enemyMaxHp = enemyMaxHp,
                    lastDamage = lastDamage
                )
            }

            AdventurePhase.ENEMY_DEFEATED -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${enemyName}を倒した！",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0..4) {
                            Text(
                                "✨",
                                fontSize = (16 + (i * 4)).sp,
                                modifier = Modifier.offset(y = (walkBounce * (i + 1) * 0.5f).dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("経験値を獲得！", fontSize = 13.sp, color = FireOrange, fontWeight = FontWeight.Bold)
                }
            }

            AdventurePhase.RESTING -> {
                BreakScene(pulseAlpha)
            }

            AdventurePhase.PLAYER_DEAD -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💀", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("力尽きた…", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FireRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1Fからやり直し！", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }
            }

            AdventurePhase.FLOOR_CLEAR -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("全階層制覇！", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FireOrange)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💎 +5  1Fから再挑戦！", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurpleGlow)
                }
            }
        }
    }
}

@Composable
private fun BreakScene(flickerAlpha: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val stars = remember {
            (0..12).map {
                Triple(
                    (-0.45f + (Math.random().toFloat() * 0.9f)),
                    (-0.4f + (Math.random().toFloat() * 0.5f)),
                    8 + (Math.random() * 8).toInt()
                )
            }
        }
        stars.forEach { (xRatio, yRatio, size) ->
            Text(
                "✦",
                fontSize = size.sp,
                color = Color.White.copy(alpha = flickerAlpha * 0.4f),
                modifier = Modifier.offset(x = (xRatio * 160).dp, y = (yRatio * 100).dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧙‍♂️", fontSize = 48.sp, modifier = Modifier.offset(x = (-20).dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("🔥", fontSize = (32 * flickerAlpha).sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("休憩中… 体力を回復しています", fontSize = 12.sp, color = BreakAccent, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultScreen(
    uiState: StudyQuestUiState,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val isStudy = uiState.type == StudySessionType.STUDY
    val infiniteTransition = rememberInfiniteTransition(label = "result")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "shimmer"
    )
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bounce"
    )

    val actualMinutes = (uiState.elapsedSeconds / 60).toInt().coerceAtLeast(1)

    val bgGradient = if (isStudy) {
        Brush.verticalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF0F172A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF0A2E1A), Color(0xFF0C1E0C)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isStudy) "⚔️ QUEST CLEAR!" else "🌿 REST COMPLETE!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isStudy) FireOrange.copy(alpha = 0.8f) else BreakAccent.copy(alpha = 0.8f),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isStudy) "クエスト達成！" else "休憩完了！",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    (if (isStudy) FireOrange else BreakAccent).copy(alpha = shimmer * 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Text(
                    if (isStudy) "🏆" else "✨",
                    fontSize = 72.sp,
                    modifier = Modifier.offset(y = bounceY.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .background(
                        if (isStudy) DarkCard else BreakCard,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isStudy) {
                        Text("📊 冒険結果", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RewardItem("⏱", "集中時間", "${actualMinutes}分")
                            RewardItem("⭐", "経験値", "+${uiState.earnedXp}")
                            RewardItem("💀", "討伐数", "${uiState.defeatedCount}体")
                            RewardItem("💎", "ダイヤ", "+${uiState.earnedStones}")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧙‍♂️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "「見事な集中力だ！\nその調子で強くなるぞ。」",
                                    fontSize = 13.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        Text("🌙 休憩完了", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A3A1A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧙‍♂️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "「いいリフレッシュになったな。\nさあ、次の冒険に備えよう！」",
                                    fontSize = 13.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Text("🏠 街に戻る", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextMuted)
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStudy) EmeraldGreen else AccentBlue
                    )
                ) {
                    Text(
                        if (isStudy) "🌿 休憩へ" else "⚔️ 冒険へ",
                        fontWeight = FontWeight.Black, fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = TextMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextWhite)
    }
}
