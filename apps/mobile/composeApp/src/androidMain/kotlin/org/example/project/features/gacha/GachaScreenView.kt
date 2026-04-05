package org.example.project.features.gacha

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.di.getGachaViewModel
import org.example.project.domain.model.BannerType
import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResultType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ══════════════════════════════════════════════════════════════
// Color Palette
// ══════════════════════════════════════════════════════════════

private val BgDark1 = Color(0xFF0A0A2E)
private val BgDark2 = Color(0xFF1A0A3E)
private val BgDark3 = Color(0xFF0A1628)

private val GoldStar = Color(0xFFFFD700)
private val PurpleStar = Color(0xFF9B59B6)
private val BlueStar = Color(0xFF3498DB)
private val GreenStar = Color(0xFF2ECC71)
private val GrayStar = Color(0xFF95A5A6)

private fun rarityColor(rarity: Int): Color = when (rarity) {
    5 -> GoldStar; 4 -> PurpleStar; 3 -> BlueStar; 2 -> GreenStar; else -> GrayStar
}

private fun rarityStars(rarity: Int): String = "★".repeat(rarity)

private fun bannerColors(type: BannerType): List<Color> = when (type) {
    BannerType.CHARACTER -> listOf(Color(0xFF4B32C8), Color(0xFF8033E6))
    BannerType.WEAPON -> listOf(Color(0xFFCC3333), Color(0xFFE6661A))
    BannerType.MIXED -> listOf(Color(0xFF9933CC), Color(0xFFE64D80))
    else -> listOf(Color.Gray, Color.Black)
}

private fun bannerIcon(type: BannerType) = when (type) {
    BannerType.CHARACTER -> Icons.Default.Person
    BannerType.WEAPON -> Icons.Default.Shield
    BannerType.MIXED -> Icons.Default.Star
    else -> Icons.Default.QuestionMark
}

private fun itemIcon(type: GachaResultType) = when (type) {
    GachaResultType.CHARACTER -> Icons.Default.Person
    GachaResultType.WEAPON -> Icons.Default.Shield
    else -> Icons.Default.Star
}

// ══════════════════════════════════════════════════════════════
// Main Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun GachaScreenView() {
    val viewModel = remember { getGachaViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BgDark1, BgDark2, BgDark3)))
    ) {
        // 背景パーティクル
        BackgroundParticles()

        Crossfade(
            targetState = uiState.phase,
            animationSpec = tween(400),
            label = "phase"
        ) { phase ->
            when (phase) {
                GachaPhase.BANNER_SELECT -> BannerSelectPhase(viewModel, uiState)
                GachaPhase.CONFIRM -> ConfirmPhase(viewModel, uiState)
                GachaPhase.PULLING -> PullAnimationPhase(uiState.highestRarity)
                GachaPhase.RESULT -> ResultPhase(viewModel, uiState)
            }
        }

        // エラー表示
        uiState.error?.let { err ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                action = {
                    TextButton(onClick = { viewModel.onIntent(GachaIntent.DismissError) }) {
                        Text("閉じる", color = Color.White)
                    }
                }
            ) {
                Text(err)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Background Particles
// ══════════════════════════════════════════════════════════════

@Composable
private fun BackgroundParticles() {
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "phase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until 15) {
            val seed = i * 137.5f
            val x = ((seed * 7.3f + phase * size.width * 0.4f) % size.width)
            val y = ((seed * 11.1f + phase * size.height * 0.3f) % size.height)
            drawCircle(
                color = Color.White.copy(alpha = 0.03f + (i % 3) * 0.02f),
                radius = 2f + (i % 4),
                center = Offset(x, y)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Banner Selection Phase
// ══════════════════════════════════════════════════════════════

@Composable
private fun BannerSelectPhase(viewModel: GachaViewModel, uiState: GachaUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("召 喚", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("知識の結晶で仲間を召喚しよう", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            StoneCountBadge(uiState.currentStones)
        }

        // バナーカード一覧
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.banners.forEachIndexed { index, banner ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 120L); visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    BannerCard(banner) { viewModel.onIntent(GachaIntent.SelectBanner(banner.id)) }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun BannerCard(banner: GachaBanner, onClick: () -> Unit) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmer.animateFloat(
        initialValue = -300f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)), label = "sx"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(Brush.linearGradient(bannerColors(banner.bannerType)))
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                        start = Offset(shimmerX, 0f), end = Offset(shimmerX + 200f, size.height)
                    )
                )
            }
    ) {
        // 大アイコン背景
        Icon(
            bannerIcon(banner.bannerType), null,
            modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-10).dp),
            tint = Color.White.copy(alpha = 0.1f)
        )

        // テキスト情報
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BadgeChip("PICK UP", Color.White.copy(alpha = 0.25f))
                BadgeChip(when(banner.bannerType) {
                    BannerType.CHARACTER -> "キャラ"
                    BannerType.WEAPON -> "武器"
                    BannerType.MIXED -> "ミックス"
                    else -> "不明"
                }, Color.White.copy(alpha = 0.15f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(banner.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("期間限定召喚開催中！", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                repeat(5) {
                    Text("★", fontSize = 14.sp, color = GoldStar)
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, bg: Color) {
    Text(
        text,
        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ══════════════════════════════════════════════════════════════
// Confirm Phase
// ══════════════════════════════════════════════════════════════

@Composable
private fun ConfirmPhase(viewModel: GachaViewModel, uiState: GachaUiState) {
    val banner = uiState.selectedBanner ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.onIntent(GachaIntent.BackToBannerSelect) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る", tint = Color.White.copy(alpha = 0.7f))
            }
            Text("バナー選択", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.weight(1f))
            StoneCountBadge(uiState.currentStones)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // バナー名
            Text(banner.name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("知識の結晶を消費して召喚します", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(24.dp))

            // 召喚オーブ
            SummoningOrb(banner.bannerType)
            Spacer(modifier = Modifier.height(24.dp))

            // 天井カウンター
            banner.pityThreshold?.let { threshold ->
                if (threshold > 0) {
                    PityCounter(uiState.pityCount, threshold)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 召喚ボタン
            GlowPullButton(
                "単発召喚", GachaUiState.SINGLE_PULL_COST, uiState.canPullSingle, bannerColors(banner.bannerType)
            ) { viewModel.onIntent(GachaIntent.PullSingle(banner.id)) }
            Spacer(modifier = Modifier.height(12.dp))
            GlowPullButton(
                "10連召喚", GachaUiState.MULTI_PULL_COST, uiState.canPullMulti, bannerColors(banner.bannerType), isPrimary = true
            ) { viewModel.onIntent(GachaIntent.PullMulti(banner.id)) }
            Spacer(modifier = Modifier.height(20.dp))

            // 排出率情報
            RateInfoCard()
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── 天井カウンター ─────────────────────────────

@Composable
private fun PityCounter(current: Int, threshold: Int) {
    val progress = (current.toFloat() / threshold).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("天井カウント", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.weight(1f))
            Text("$current / $threshold", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF4DB8FF), Color(0xFF9966FF))),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

// ── 排出率情報 ───────────────────────────────

@Composable
private fun RateInfoCard() {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("排出率", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                RateRow("★★★★★", "3.0%", GoldStar)
                RateRow("★★★★", "15.0%", PurpleStar)
                RateRow("★★★", "82.0%", BlueStar)
            }
        }
    }
}

@Composable
private fun RateRow(stars: String, rate: String, color: Color) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(stars, fontSize = 12.sp, color = color)
        Spacer(modifier = Modifier.weight(1f))
        Text(rate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

// ══════════════════════════════════════════════════════════════
// Pull Animation Phase
// ══════════════════════════════════════════════════════════════

@Composable
private fun PullAnimationPhase(highestRarity: Int) {
    val accent = rarityColor(highestRarity)

    // アニメーション値
    var animPhase by remember { mutableIntStateOf(0) } // 0:出現, 1:収束, 2:バースト
    val circleScale by animateFloatAsState(
        targetValue = when (animPhase) { 0 -> 1f; 1 -> 1.4f; else -> 0f },
        animationSpec = tween(if (animPhase == 2) 200 else 600), label = "cs"
    )
    val circleAlpha by animateFloatAsState(
        targetValue = when (animPhase) { 2 -> 0f; else -> 1f },
        animationSpec = tween(250), label = "ca"
    )
    val glowScale by animateFloatAsState(
        targetValue = when (animPhase) { 0 -> 0.6f; 1 -> 1.2f; else -> 0f },
        animationSpec = tween(800), label = "gs"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = when (animPhase) { 0 -> 0.4f; 1 -> 0.8f; else -> 0f },
        animationSpec = tween(600), label = "ga"
    )
    val burstScale by animateFloatAsState(
        targetValue = if (animPhase >= 2) 5f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "bs"
    )
    val burstAlpha by animateFloatAsState(
        targetValue = if (animPhase == 2) 1f else 0f,
        animationSpec = tween(if (animPhase == 2) 200 else 300), label = "ba"
    )

    val rotation = rememberInfiniteTransition(label = "rot")
    val rotDeg by rotation.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)), label = "rd"
    )

    // パーティクル収束
    val particleProgress by animateFloatAsState(
        targetValue = if (animPhase >= 1) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "pp"
    )

    // フェーズ遷移タイミング
    LaunchedEffect(Unit) {
        delay(1500L); animPhase = 1
        delay(1200L); animPhase = 2
        delay(500L);  animPhase = 3
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        // パーティクル
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2; val cy = size.height / 2
            for (i in 0 until 20) {
                val angle = i * 18.0 * Math.PI / 180.0
                val r = 150f + (i % 3) * 40f
                val fromX = cx + cos(angle).toFloat() * r
                val fromY = cy + sin(angle).toFloat() * r
                val x = fromX + (cx - fromX) * particleProgress
                val y = fromY + (cy - fromY) * particleProgress
                drawCircle(
                    color = accent.copy(alpha = 0.7f * circleAlpha),
                    radius = 3f + (i % 3), center = Offset(x, y)
                )
            }
        }

        // 外側リング
        Canvas(
            modifier = Modifier.size(220.dp).scale(circleScale).rotate(rotDeg)
                .graphicsLayer { alpha = circleAlpha }
        ) {
            drawCircle(color = accent.copy(alpha = 0.8f), style = Stroke(width = 3f))
        }

        // 内側リング
        Canvas(
            modifier = Modifier.size(150.dp).scale(circleScale).rotate(-rotDeg * 0.7f)
                .graphicsLayer { alpha = circleAlpha }
        ) {
            drawCircle(color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))
        }

        // 十字装飾
        Canvas(
            modifier = Modifier.size(200.dp).scale(circleScale)
                .graphicsLayer { alpha = circleAlpha * 0.3f }
        ) {
            for (i in 0 until 4) {
                val angle = i * 45.0 * Math.PI / 180.0
                val endX = center.x + cos(angle).toFloat() * 100f
                val endY = center.y + sin(angle).toFloat() * 100f
                drawLine(accent, center, Offset(endX, endY), strokeWidth = 1f)
            }
        }

        // 中央グロー
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(glowScale)
                .graphicsLayer { alpha = glowAlpha }
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.1f), Color.Transparent)),
                    CircleShape
                )
        )

        // バースト
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(burstScale)
                .graphicsLayer { alpha = burstAlpha }
                .background(
                    Brush.radialGradient(listOf(Color.White, accent.copy(alpha = 0.4f), Color.Transparent)),
                    CircleShape
                )
        )

        // テキスト
        Text(
            "召 喚 中 ...",
            fontSize = 14.sp, letterSpacing = 4.sp, color = Color.White.copy(alpha = circleAlpha * 0.5f),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Center).offset(y = 160.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// Summoning Orb (idle in confirm phase)
// ══════════════════════════════════════════════════════════════

@Composable
private fun SummoningOrb(type: BannerType) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        val colors = bannerColors(type)
        // 外周リング
        Canvas(modifier = Modifier.size(160.dp).rotate(rotation)) {
            drawCircle(
                Brush.sweepGradient(colors + listOf(colors.first())),
                style = Stroke(width = 3f)
            )
        }
        // 内周リング
        Canvas(modifier = Modifier.size(120.dp).rotate(-rotation * 0.5f)) {
            drawCircle(colors.first().copy(alpha = 0.3f), style = Stroke(width = 1.5f))
        }
        // グロー
        Box(
            modifier = Modifier.size(120.dp).scale(pulse).background(
                Brush.radialGradient(listOf(colors.last().copy(alpha = 0.4f), Color.Transparent)),
                CircleShape
            )
        )
        // アイコン
        Icon(bannerIcon(type), null, modifier = Modifier.size(40.dp).scale(pulse * 0.9f), tint = Color.White.copy(alpha = 0.8f))
    }
}

// ══════════════════════════════════════════════════════════════
// Result Phase
// ══════════════════════════════════════════════════════════════

@Composable
private fun ResultPhase(viewModel: GachaViewModel, uiState: GachaUiState) {
    val sorted = remember(uiState.pullResults) { uiState.pullResults.sortedByDescending { it.rarity } }

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("召喚結果", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            StoneCountBadge(uiState.currentStones)
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.lastPullCount == 1 && sorted.isNotEmpty()) {
                SingleResultCard(sorted.first())
            } else {
                sorted.forEachIndexed { index, item ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(150L + index * 80L); visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300)) + scaleIn(tween(400), initialScale = 0.4f)
                    ) {
                        MultiResultCard(item)
                    }
                    if (index < sorted.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // アクションボタン
            val cost = if (uiState.lastPullCount == 1) GachaUiState.SINGLE_PULL_COST else GachaUiState.MULTI_PULL_COST
            val canPull = if (uiState.lastPullCount == 1) uiState.canPullSingle else uiState.canPullMulti
            GlowPullButton("もう一度召喚する", cost, canPull,
                listOf(Color(0xFF4D99FF), Color(0xFF804DFF))
            ) { viewModel.onIntent(GachaIntent.PullAgain) }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { viewModel.onIntent(GachaIntent.DismissResults) }) {
                Text("バナー選択に戻る", fontSize = 15.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── 単発結果カード ─────────────────────────────

@Composable
private fun SingleResultCard(item: GachaResultItem) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); revealed = true }

    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f), label = "s"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f, animationSpec = tween(400), label = "a"
    )
    val rotY by animateFloatAsState(
        targetValue = if (revealed) 0f else 180f,
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "ry"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .scale(scale)
            .graphicsLayer { alpha = cardAlpha; rotationY = rotY }
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(140.dp).background(
                    Brush.radialGradient(
                        listOf(rarityColor(item.rarity).copy(alpha = 0.4f), rarityColor(item.rarity).copy(alpha = 0.05f), Color.Transparent)
                    ), CircleShape
                )
            )
            Icon(itemIcon(item.type), null, modifier = Modifier.size(60.dp), tint = Color.White)
            if (item.isNew) {
                Text(
                    "NEW", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).background(Color.Red, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(rarityStars(item.rarity), fontSize = 24.sp, color = rarityColor(item.rarity))
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(if (item.type == GachaResultType.CHARACTER) "キャラクター" else "武器", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

// ── 10連結果カード ─────────────────────────────

@Composable
private fun MultiResultCard(item: GachaResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(48.dp).background(
                    Brush.radialGradient(
                        listOf(rarityColor(item.rarity).copy(alpha = 0.35f), Color.Transparent)
                    ), CircleShape
                )
            )
            Icon(itemIcon(item.type), null, modifier = Modifier.size(24.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(rarityStars(item.rarity), fontSize = 12.sp, color = rarityColor(item.rarity))
        }
        if (item.isNew) {
            Text(
                "NEW", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.background(Color.Red, RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Common Components
// ══════════════════════════════════════════════════════════════

@Composable
private fun StoneCountBadge(stones: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.Star, null, tint = GoldStar, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$stones", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun GlowPullButton(
    label: String, cost: Int, enabled: Boolean, colors: List<Color>,
    isPrimary: Boolean = false, onClick: () -> Unit
) {
    val height = if (isPrimary) 56.dp else 48.dp
    val shimmer = rememberInfiniteTransition(label = "btn")
    val shimmerX by shimmer.animateFloat(
        initialValue = -300f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "bsx"
    )
    val bg = if (enabled) Brush.horizontalGradient(colors) else Brush.horizontalGradient(
        listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(
                if (enabled) Modifier.drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent),
                            start = Offset(shimmerX, 0f), end = Offset(shimmerX + 200f, size.height)
                        )
                    )
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val fontSize = if (isPrimary) 17.sp else 15.sp
            val textColor = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
            Text(label, fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .background(Color.White.copy(alpha = if (enabled) 0.2f else 0.05f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = textColor, modifier = Modifier.size(14.dp))
                Text("$cost", fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}
