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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val attackShake by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(80), RepeatMode.Reverse), label = "shake"
    )
    val damageFlash by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse), label = "flash"
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
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isBreak) Brush.verticalGradient(listOf(Color(0xFF0A1F0A), Color(0xFF132613)))
                    else Brush.verticalGradient(listOf(Color(0xFF0A0F1E), Color(0xFF141C2F)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isBreak) {
                BreakScene(restFlicker)
            } else {
                AdventureScene(
                    phase = phase,
                    enemyEmoji = uiState.enemyEmoji,
                    enemyName = uiState.enemyName,
                    enemyHp = uiState.enemyHp,
                    enemyMaxHp = uiState.enemyMaxHp,
                    lastDamage = uiState.lastDamage,
                    walkOffset = walkOffset,
                    walkBounce = walkBounce,
                    attackShake = attackShake,
                    damageFlash = damageFlash,
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isBreak) BreakCard else DarkCard)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    if (isBreak) "🌙 キャンプログ" else "📜 冒険ログ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val logs = uiState.currentLog.takeLast(3)
                    logs.forEachIndexed { idx, log ->
                        val isLatest = idx == logs.lastIndex
                        Row(
                            modifier = Modifier.padding(bottom = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(4.dp)
                                    .background(
                                        if (isLatest) (if (isBreak) BreakAccent else AccentBlue) else TextMuted,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log,
                                fontSize = 11.sp,
                                color = if (isLatest) TextWhite else TextMuted,
                                fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
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

@Composable
private fun AdventureScene(
    phase: AdventurePhase,
    enemyEmoji: String,
    enemyName: String,
    enemyHp: Int,
    enemyMaxHp: Int,
    lastDamage: Int,
    walkOffset: Float,
    walkBounce: Float,
    attackShake: Float,
    damageFlash: Float,
    pulseAlpha: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .background(TextMuted.copy(alpha = 0.2f))
        )

        if (phase == AdventurePhase.WALKING) {
            for (i in 0..4) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .align(Alignment.BottomCenter)
                        .offset(
                            x = ((i * 70 - 140) + walkOffset * 3).dp,
                            y = (-38).dp
                        )
                        .background(TextMuted.copy(alpha = 0.3f), CircleShape)
                )
            }
        }

        when (phase) {
            AdventurePhase.WALKING -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("…", fontSize = 16.sp, color = TextMuted.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "🧙‍♂️",
                        fontSize = 64.sp,
                        modifier = Modifier.offset(x = walkOffset.dp, y = walkBounce.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("探索中…", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                }
            }

            AdventurePhase.ENCOUNTER -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FireRed.copy(alpha = damageFlash * 0.15f))
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${enemyName}が現れた！",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(enemyEmoji, fontSize = 72.sp)
                }
            }

            AdventurePhase.ATTACKING -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🧙‍♂️",
                        fontSize = 56.sp,
                        modifier = Modifier.offset(x = attackShake.dp)
                    )

                    Text(
                        "⚔️",
                        fontSize = 28.sp,
                        color = FireRed.copy(alpha = pulseAlpha),
                        modifier = Modifier.offset(y = (-8).dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedVisibility(
                            visible = lastDamage > 0,
                            enter = fadeIn(tween(100)) + slideInVertically { -20 },
                            exit = fadeOut(tween(500))
                        ) {
                            Text(
                                "-${lastDamage}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = DamageRed
                            )
                        }

                        Text(
                            enemyEmoji,
                            fontSize = 56.sp,
                            modifier = Modifier.offset(
                                x = if (lastDamage > 0) attackShake.dp else 0.dp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(enemyName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(6.dp)
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
                            Text("${enemyHp}/${enemyMaxHp}", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
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
