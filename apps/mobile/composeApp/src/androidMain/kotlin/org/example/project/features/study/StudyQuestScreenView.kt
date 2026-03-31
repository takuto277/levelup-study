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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── カラー ─────────────────────────────
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
// 休憩
private val BreakBg = Color(0xFF0C1E0C)
private val BreakCard = Color(0xFF1A2E1A)
private val BreakAccent = Color(0xFF34D399)
private val BreakGlow = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyQuestScreenView(
    initialStudyMinutes: Int,
    onDismiss: () -> Unit
) {
    val viewModel = remember { StudyQuestViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(StudyQuestIntent.StartQuest(initialStudyMinutes))
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
                    onFinishSession = { viewModel.onIntent(StudyQuestIntent.FinishSession) },
                    onExit = {
                        viewModel.onIntent(StudyQuestIntent.StopQuest)
                        onDismiss()
                    }
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// メインクエスト — 没入感のある暗めUI
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MainQuestView(
    uiState: StudyQuestUiState,
    onTogglePause: () -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit
) {
    val isBreak = uiState.type == StudySessionType.BREAK
    val isOvertime = uiState.isOvertime

    val infiniteTransition = rememberInfiniteTransition(label = "quest")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    // タイマー進捗の計算
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
        // ── トップバー ──
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 離脱
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Exit", tint = TextMuted, modifier = Modifier.size(24.dp))
            }
            // ステータスバッジ
            Box(
                modifier = Modifier
                    .background(
                        if (isOvertime) PurpleGlow.copy(alpha = 0.2f)
                        else if (isBreak) BreakAccent.copy(alpha = 0.15f)
                        else AccentBlue.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isOvertime -> "⚡ 限界突破中"
                        uiState.status == StudySessionStatus.PAUSED -> "⏸ 一時停止"
                        isBreak -> "🌿 休憩中"
                        else -> "⚔️ 冒険中"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isOvertime -> PurpleGlow
                        isBreak -> BreakAccent
                        else -> AccentBlue
                    }
                )
            }
            // ダミースペーサー
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // ── サークルタイマー ──
        Box(
            modifier = Modifier.size((220 * glowScale).dp),
            contentAlignment = Alignment.Center
        ) {
            // 外側グロー
            val glowColor = when {
                isOvertime -> PurpleGlow
                isBreak -> BreakGlow
                else -> AccentBlue
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 10f
                val r = (size.minDimension - strokeW) / 2
                val c = Offset(size.width / 2, size.height / 2)
                val tl = Offset(c.x - r, c.y - r)
                val arcSize = Size(r * 2, r * 2)

                // 背景リング
                drawArc(
                    color = glowColor.copy(alpha = 0.15f),
                    startAngle = 0f, sweepAngle = 360f,
                    useCenter = false, topLeft = tl, size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
                // 進捗リング
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(glowColor.copy(alpha = pulseAlpha), glowColor)
                    ),
                    startAngle = -90f, sweepAngle = 360f * progress,
                    useCenter = false, topLeft = tl, size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // キャラ & 敵
                if (isBreak) {
                    Text("🏕️", fontSize = 48.sp)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧙‍♂️", fontSize = 40.sp)
                        Text("⚔️", fontSize = 20.sp, color = FireRed.copy(alpha = pulseAlpha))
                        Text("👾", fontSize = 40.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // タイマー
                Text(
                    text = uiState.displayTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        isOvertime -> PurpleGlow
                        isBreak -> BreakAccent
                        else -> TextWhite
                    }
                )

                if (isOvertime) {
                    Text(
                        "延長戦！",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleGlow.copy(alpha = pulseAlpha)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── バトルログ ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isBreak) BreakCard else DarkCard)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isBreak) "🌙 キャンプログ" else "📜 冒険ログ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isOvertime) {
                        Box(
                            modifier = Modifier
                                .background(PurpleGlow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("超集中", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PurpleGlow)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    uiState.currentLog.forEachIndexed { idx, log ->
                        val isLatest = idx == uiState.currentLog.lastIndex
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .background(
                                        if (isLatest) (if (isBreak) BreakAccent else AccentBlue) else TextMuted,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = log,
                                fontSize = 13.sp,
                                color = if (isLatest) TextWhite else TextMuted,
                                fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 操作ボタン ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 完了ボタン（勉強中のみ）
            if (!isBreak) {
                Button(
                    onClick = onFinishSession,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("🏁 クエスト完了", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 一時停止 / 再開
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

                // 中止
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FireRed)
                ) {
                    Text("中止", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FireRed)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// リザルト画面 — 達成感のある演出
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
            // タイトル
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

            Spacer(modifier = Modifier.height(32.dp))

            // 報酬キャラ
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // グロー
                Box(
                    modifier = Modifier
                        .size(160.dp)
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
                    fontSize = 80.sp,
                    modifier = Modifier.offset(y = bounceY.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 報酬カード
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
                            RewardItem("⏱", "集中時間", "${uiState.targetStudyMinutes}分")
                            RewardItem("⭐", "経験値", "+${uiState.targetStudyMinutes * 10}")
                            RewardItem("💎", "結晶", "+${uiState.targetStudyMinutes / 5}")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // キャラ一言
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

            Spacer(modifier = Modifier.height(32.dp))

            // ボタン
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStudy) EmeraldGreen else AccentBlue
                )
            ) {
                Text(
                    if (isStudy) "🌿 休憩を開始する" else "⚔️ 次の冒険へ",
                    fontWeight = FontWeight.Black, fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onExit) {
                Text("🏠 街に戻る", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RewardItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextMuted)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextWhite)
    }
}
