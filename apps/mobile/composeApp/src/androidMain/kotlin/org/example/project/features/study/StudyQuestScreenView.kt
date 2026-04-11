package org.example.project.features.study

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.style.TextOverflow
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
        MainQuestView(
            uiState = uiState,
            onTogglePause = { viewModel.onIntent(StudyQuestIntent.TogglePause) },
            onEndQuest = { viewModel.onIntent(StudyQuestIntent.EndQuest) },
            onResumeAdventure = { viewModel.onIntent(StudyQuestIntent.NextSession) },
            onFinishBreakSession = {
                viewModel.onIntent(StudyQuestIntent.StopQuest)
                onDismiss()
            }
        )
    }
}

@Composable
private fun MainQuestView(
    uiState: StudyQuestUiState,
    onTogglePause: () -> Unit,
    onEndQuest: () -> Unit,
    onResumeAdventure: () -> Unit,
    onFinishBreakSession: () -> Unit
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
    val showEnemyHpBar = !isBreak && (phase == AdventurePhase.ENCOUNTER || phase == AdventurePhase.ATTACKING)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(44.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isBreak) {
                        Text(
                            text = "🌿 休憩",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BreakAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BreakAccent.copy(alpha = 0.14f))
                                .border(1.dp, BreakAccent.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else {
                        resolvedStudyGenreLabel(uiState.genreId)?.let { label ->
                            QuestMetaCapsule(text = label)
                        }
                        uiState.dungeonName?.takeIf { it.isNotEmpty() }?.let { dn ->
                            QuestMetaCapsule(text = dn, leadingEmoji = "🏰")
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOvertime) {
                        Text(
                            "⚡",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = PurpleGlow,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PurpleGlow.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (uiState.status == StudySessionStatus.PAUSED) {
                        Text(
                            "停止中",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (!isBreak) {
                        Text(
                            "F${uiState.currentFloor}/${uiState.totalFloors}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkSurface.copy(alpha = 0.9f))
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                        Text(
                            "💀 ${uiState.defeatedCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.14f))
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (isBreak) {
            Spacer(modifier = Modifier.height(10.dp))
            BreakAfterStudySummaryCard(uiState = uiState)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 探索中は敵 HP 非表示だが列は 2 分割。プレイヤーが与えたダメージは敵列側フロートへ。
                if (!isBreak) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧙‍♂️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        QuestHpBarStrip(
                            currentHp = uiState.playerHp,
                            maxHp = uiState.playerMaxHp,
                            floatingDamage = uiState.lastPlayerDamage,
                            adventurePhaseTick = uiState.adventurePhaseTick,
                            floatTriggerTurnMod = 1L,
                            modifier = Modifier.weight(1f),
                            header = {
                                Text("HP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "${uiState.playerHp}/${uiState.playerMaxHp}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (showEnemyHpBar) {
                                QuestHpBarStrip(
                                    currentHp = uiState.enemyHp,
                                    maxHp = uiState.enemyMaxHp,
                                    floatingDamage = uiState.lastDamage,
                                    adventurePhaseTick = uiState.adventurePhaseTick,
                                    floatTriggerTurnMod = 0L,
                                    modifier = Modifier.fillMaxWidth(),
                                    header = {
                                        Text(uiState.enemyEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            uiState.enemyName,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${uiState.enemyHp}/${uiState.enemyMaxHp}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                )
                            } else {
                                QuestHpBarStrip(
                                    currentHp = 0,
                                    maxHp = 1,
                                    floatingDamage = uiState.lastDamage,
                                    adventurePhaseTick = uiState.adventurePhaseTick,
                                    floatTriggerTurnMod = 0L,
                                    modifier = Modifier.fillMaxWidth(),
                                    showChrome = false,
                                    header = { Spacer(modifier = Modifier.width(0.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!isBreak) {
                        val context = LocalContext.current
                        val hasBgRes = remember(uiState.dungeonName) { hasBackgroundResource(context, uiState.dungeonName) }
                        if (!hasBgRes) {
                            Column(modifier = Modifier.matchParentSize()) {
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Brush.verticalGradient(listOf(Color(0xFF2D1B0E), Color(0xFF1A1005))))
                                )
                            }
                        }
                    }
                    if (isBreak) {
                        BreakScene(restFlicker)
                    } else {
                        AdventureScene(
                            phase = phase,
                            enemyEmoji = uiState.enemyEmoji,
                            enemySpriteKey = uiState.enemySpriteKey,
                            enemyHp = uiState.enemyHp,
                            enemyMaxHp = uiState.enemyMaxHp,
                            lastDamage = uiState.lastDamage,
                            dungeonName = uiState.dungeonName,
                            currentFloor = uiState.currentFloor,
                            totalFloors = uiState.totalFloors,
                            adventurePhaseTick = uiState.adventurePhaseTick,
                            walkOffset = walkOffset,
                            walkBounce = walkBounce,
                            pulseAlpha = pulseAlpha
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
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
            if (isBreak) {
                OutlinedButton(
                    onClick = onFinishBreakSession,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Text("終了する", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Button(
                    onClick = onResumeAdventure,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("⚔️ 冒険へ", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            } else {
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
}

private val ConfrontGap = 30.dp
/** 探索時の歩きスプライト（118dp）と揃え、床位置を一致させる */
private val ConfrontPlayerSize = 118.dp
private val ConfrontEnemySize = 118.dp
/** 最終階のボス遭遇時のみ（縦横おおよそ 2 倍） */
private val ConfrontBossEnemySize = 236.dp

/**
 * 背景を下基準でクロップしたときの「床帯」に足を乗せるための下端オフセット。
 * 大きいほどキャラが画面上に上がる（以前の 44dp は床から浮いていた）。
 */
private val AdventureFloorInsetDp = 8.dp

/** 「総合」「general」は表示しない。それ以外のジャンル ID のみ返す。 */
private fun resolvedStudyGenreLabel(genreId: String?): String? {
    val raw = genreId?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val lower = raw.lowercase()
    if (lower == "general" || raw == "総合") return null
    return raw
}

private fun combatTurnMod(phaseTick: Long): Long {
    var m = phaseTick % STUDY_QUEST_ATTACK_CYCLE_SEC
    if (m < 0) m += STUDY_QUEST_ATTACK_CYCLE_SEC
    return m
}

private fun hpRatioColor(ratio: Float): Color = when {
    ratio > 0.5f -> EmeraldGreen
    ratio > 0.25f -> FireOrange
    else -> FireRed
}

@Composable
private fun FloatingDamageText(damage: Int, phaseSyncKey: Long, floatTriggerTurnMod: Long) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var alphaV by remember { mutableFloatStateOf(0f) }
    fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
    LaunchedEffect(damage, phaseSyncKey) {
        if (damage <= 0) {
            alphaV = 0f
            offsetY = 0f
            return@LaunchedEffect
        }
        if (combatTurnMod(phaseSyncKey) != floatTriggerTurnMod) return@LaunchedEffect
        offsetY = 6f
        alphaV = 1f
        val steps = 14
        repeat(steps) { i ->
            val t = (i + 1) / steps.toFloat()
            offsetY = lerpF(6f, -22f, t)
            alphaV = if (t < 0.28f) 1f else lerpF(1f, 0f, (t - 0.28f) / 0.72f).coerceIn(0f, 1f)
            delay(40)
        }
        alphaV = 0f
        offsetY = 0f
    }
    if (alphaV > 0.02f && damage > 0) {
        Text(
            text = "-$damage",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = DamageRed,
            modifier = Modifier
                .offset(y = offsetY.dp)
                .alpha(alphaV)
        )
    }
}

private val QuestHpHeaderRowHeight = 18.dp

/** プレイヤー列・敵列で共通の HP ヘッダー＋バー＋中央ダメージフロート */
@Composable
private fun QuestHpBarStrip(
    currentHp: Int,
    maxHp: Int,
    floatingDamage: Int,
    adventurePhaseTick: Long,
    floatTriggerTurnMod: Long,
    modifier: Modifier = Modifier,
    showChrome: Boolean = true,
    header: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (showChrome) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(QuestHpHeaderRowHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                header()
            }
        } else {
            Spacer(modifier = Modifier.height(QuestHpHeaderRowHeight))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showChrome) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSurface)
                ) {
                    val ratio = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .background(hpRatioColor(ratio), RoundedCornerShape(3.dp))
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.BottomCenter)
                )
            }
            FloatingDamageText(
                damage = floatingDamage,
                phaseSyncKey = adventurePhaseTick,
                floatTriggerTurnMod = floatTriggerTurnMod
            )
        }
    }
}

@Composable
private fun QuestMetaCapsule(text: String, leadingEmoji: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (leadingEmoji != null) {
            Text(leadingEmoji, fontSize = 11.sp)
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 戦闘中: tick%3 が 0→攻撃 or idle, 1→idle（敵ターン）, 2→prep（ViewModel と同期） */
private fun combatPlayerMode(phaseTick: Long, lastDamage: Int): PlayerSpriteMode =
    when (combatTurnMod(phaseTick)) {
        0L -> if (lastDamage > 0) PlayerSpriteMode.Attack else PlayerSpriteMode.Idle
        1L -> PlayerSpriteMode.Idle
        2L -> PlayerSpriteMode.Prep
        else -> PlayerSpriteMode.Idle
    }

/** 遭遇〜戦闘：ステージ内テキストなし。床ラインのみ（敵 HP は MainQuestView）。 */
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
    lastDamage: Int,
    currentFloor: Int,
    totalFloors: Int
) {
    val isStriking = isAttackPhase && combatTurnMod(adventurePhaseTick) == 0L && lastDamage > 0
    val enemyBobY = if (!isAttackPhase) {
        if (syncBob) (-3).dp else 2.dp
    } else {
        0.dp
    }
    val progress = approachProgress.coerceIn(0f, 1f)
    val isBossEncounter = currentFloor >= totalFloors
    val enemySlotSize = if (isBossEncounter) ConfrontBossEnemySize else ConfrontEnemySize
    var enemyNudgeX by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(adventurePhaseTick, isAttackPhase) {
        if (!isAttackPhase || combatTurnMod(adventurePhaseTick) != 1L) return@LaunchedEffect
        try {
            enemyNudgeX = -14f
            delay(85)
        } finally {
            enemyNudgeX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val centerX = maxWidth / 2
            val playerEndLeft = centerX - ConfrontGap / 2 - ConfrontPlayerSize
            val enemyEndLeft = centerX + ConfrontGap / 2 - (enemySlotSize - ConfrontEnemySize) / 2
            val playerStartLeft = (-32).dp
            val enemyStartLeft = maxWidth + 88.dp + (enemySlotSize - ConfrontEnemySize)
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
                        .offset(x = playerLeft + if (isAttackPhase && isStriking) 6.dp else 0.dp)
                        .padding(start = 16.dp, bottom = AdventureFloorInsetDp)
                )
            } else {
                Text(
                    "🧙‍♂️",
                    fontSize = 52.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = playerLeft)
                        .padding(start = 16.dp, bottom = AdventureFloorInsetDp)
                )
            }

            if (hasEnemySprite) {
                BattleSprite(
                    spriteKey = enemySpriteKey,
                    spriteType = "enemy",
                    size = enemySlotSize,
                    animateFrames = false,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = enemyLeft + enemyNudgeX.dp, y = enemyBobY)
                        .padding(bottom = AdventureFloorInsetDp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = enemyLeft + enemyNudgeX.dp, y = enemyBobY)
                        .padding(bottom = AdventureFloorInsetDp)
                        .size(enemySlotSize),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        enemyEmoji,
                        fontSize = if (isBossEncounter) 112.sp else 56.sp
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
    enemySpriteKey: String,
    enemyHp: Int,
    enemyMaxHp: Int,
    lastDamage: Int,
    dungeonName: String?,
    currentFloor: Int,
    totalFloors: Int,
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
                    lastDamage = lastDamage,
                    currentFloor = currentFloor,
                    totalFloors = totalFloors
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
                    lastDamage = lastDamage,
                    currentFloor = currentFloor,
                    totalFloors = totalFloors
                )
            }

            AdventurePhase.ENEMY_DEFEATED,
            AdventurePhase.FLOOR_CLEAR -> {
                Box(modifier = Modifier.fillMaxSize())
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

        }
    }
}

@Composable
private fun BreakScene(flickerAlpha: Float) {
    val breatheTransition = rememberInfiniteTransition(label = "break_breathe")
    val breatheY by breatheTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val dotPulse by breatheTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "dot"
    )

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
                color = Color.White.copy(alpha = flickerAlpha * 0.32f),
                modifier = Modifier.offset(x = (xRatio * 180).dp, y = (yRatio * 120).dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2818),
                            Color(0xFF071209),
                            Color(0xFF0D2214)
                        )
                    )
                )
                .border(1.5.dp, BreakAccent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(vertical = 18.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BreakAccent.copy(alpha = 0.12f))
                    .border(1.dp, BreakAccent.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(BreakAccent.copy(alpha = dotPulse))
                )
                Text(
                    "休憩中",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Text(
                    "HP 回復",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BreakAccent.copy(alpha = 0.92f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BreakAccent.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-6).dp)
                        .size(width = 150.dp, height = 70.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BreakGlow.copy(alpha = flickerAlpha * 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                PlayerSprite(
                    mode = PlayerSpriteMode.Rest,
                    size = 148.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = breatheY.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "焚き火を囲んで ゆっくり休んでいます",
                fontSize = 12.sp,
                color = BreakAccent,
                fontWeight = FontWeight.Medium
            )
            Text(
                "「冒険へ」で続行、「終了する」でホームへ戻れます",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun BreakAfterStudySummaryCard(uiState: StudyQuestUiState) {
    val studyMinutes = (uiState.completedStudyElapsedSeconds / 60).toInt().coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BreakCard.copy(alpha = 0.95f))
            .border(1.dp, BreakAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "📊 直前の冒険の結果",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BreakAccent
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RewardItem("⏱", "集中時間", "${studyMinutes}分")
            RewardItem("⭐", "経験値", "+${uiState.earnedXp}")
            RewardItem("💀", "討伐数", "${uiState.defeatedCount}体")
            RewardItem("💎", "ダイヤ", "+${uiState.earnedStones}")
        }
        uiState.serverRewards.takeIf { it.isNotEmpty() }?.let { rewards ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                rewards.joinToString("  "),
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
        uiState.serverSynced?.let { ok ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (ok) "サーバーに記録しました" else "サーバー未同期（あとで再試行）",
                fontSize = 10.sp,
                color = if (ok) BreakAccent else TextMuted
            )
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
