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

// ══════════════════════════════════════════════════════════════
// Data Types
// ══════════════════════════════════════════════════════════════

private enum class GachaPhase { BANNER_SELECT, CONFIRM, PULLING, RESULT }

private enum class BannerType(val label: String, val colors: List<Color>) {
    CHARACTER("キャラ", listOf(Color(0xFF4B32C8), Color(0xFF8033E6))),
    WEAPON("武器", listOf(Color(0xFFCC3333), Color(0xFFE6661A))),
    MIXED("ミックス", listOf(Color(0xFF9933CC), Color(0xFFE64D80)))
}

private data class BannerDisplay(
    val id: String,
    val name: String,
    val type: BannerType,
    val pityThreshold: Int,
    val featuredRarity: Int,
    val description: String
)

private enum class ItemType(val label: String) {
    CHARACTER("キャラクター"), WEAPON("武器")
}

private data class ResultItem(
    val id: String,
    val name: String,
    val rarity: Int,
    val type: ItemType,
    val isNew: Boolean
)

// ══════════════════════════════════════════════════════════════
// Store (local mock ViewModel)
// TODO: KMP GachaViewModel + Koin 接続後はこのクラスを置き換える
// ══════════════════════════════════════════════════════════════

private class GachaStore {
    var phase by mutableStateOf(GachaPhase.BANNER_SELECT)
    var banners by mutableStateOf(emptyList<BannerDisplay>())
    var selectedBanner by mutableStateOf<BannerDisplay?>(null)
    var currentStones by mutableIntStateOf(1250)
    var pityCount by mutableIntStateOf(47)
    var pullResults by mutableStateOf(emptyList<ResultItem>())
    var lastPullCount by mutableIntStateOf(0)

    val canPullSingle: Boolean get() = currentStones >= SINGLE_COST
    val canPullMulti: Boolean get() = currentStones >= MULTI_COST
    val highestRarity: Int get() = pullResults.maxOfOrNull { it.rarity } ?: 3

    init { loadBanners() }

    fun loadBanners() {
        banners = listOf(
            BannerDisplay("b1", "光の勇者ピックアップ", BannerType.CHARACTER, 90, 5, "★5 光の勇者アリア 排出率UP!"),
            BannerDisplay("b2", "伝説の聖剣ガチャ", BannerType.WEAPON, 80, 5, "★5 聖剣エクスカリバー 排出率UP!"),
            BannerDisplay("b3", "新学期スペシャル召喚", BannerType.MIXED, 0, 4, "★4以上キャラ＆武器の排出率2倍!")
        )
    }

    fun selectBanner(banner: BannerDisplay) {
        selectedBanner = banner
        pityCount = Random.nextInt(20, 76)
        phase = GachaPhase.CONFIRM
    }

    suspend fun pullSingle() {
        if (!canPullSingle) return
        currentStones -= SINGLE_COST; lastPullCount = 1
        executePull(1)
    }

    suspend fun pullMulti() {
        if (!canPullMulti) return
        currentStones -= MULTI_COST; lastPullCount = 10
        executePull(10)
    }

    private suspend fun executePull(count: Int) {
        phase = GachaPhase.PULLING
        delay(3200L) // 演出の最低時間
        pullResults = generateResults(count)
        phase = GachaPhase.RESULT
    }

    fun pullAgain() { pullResults = emptyList(); phase = GachaPhase.CONFIRM }
    fun backToBannerSelect() { selectedBanner = null; pullResults = emptyList(); phase = GachaPhase.BANNER_SELECT }

    // ── Mock Data ──
    private val charPool = listOf(
        "光の勇者アリア" to 5, "闇の魔王ゼファー" to 5, "聖女セラフィーナ" to 5,
        "炎の魔術師レイ" to 4, "氷の弓使いリナ" to 4, "風の剣士カイト" to 4,
        "見習い戦士タロウ" to 3, "森の精霊コダマ" to 3, "街の商人マルコ" to 3
    )
    private val weapPool = listOf(
        "聖剣エクスカリバー" to 5, "闇の大鎌デスサイズ" to 5,
        "氷の弓フロストアロー" to 4, "炎の杖ヘルフレイム" to 4,
        "鉄の剣" to 3, "木の杖" to 3, "革の盾" to 3
    )

    private fun generateResults(count: Int): List<ResultItem> = (1..count).map { i ->
        val rarity = rollRarity(count >= 10 && i == count)
        val isChar = Random.nextBoolean()
        val pool = if (isChar) charPool else weapPool
        val item = pool.filter { it.second == rarity }.randomOrNull()
            ?: pool.filter { it.second <= rarity }.random()
        ResultItem("mock_${System.nanoTime()}_$i", item.first, item.second,
            if (isChar) ItemType.CHARACTER else ItemType.WEAPON, Random.nextInt(4) == 0)
    }

    private fun rollRarity(guarantee4: Boolean): Int {
        val roll = Random.nextInt(1, 1001)
        return when { roll <= 30 -> 5; roll <= 180 || guarantee4 -> 4; else -> 3 }
    }

    companion object {
        const val SINGLE_COST = 50
        const val MULTI_COST = 450
    }
}

// ══════════════════════════════════════════════════════════════
// Main Screen
// ══════════════════════════════════════════════════════════════

@Composable
fun GachaScreenView() {
    val store = remember { GachaStore() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(BgDark1, BgDark2, BgDark3)))
    ) {
        // 背景パーティクル
        BackgroundParticles()

        Crossfade(
            targetState = store.phase,
            animationSpec = tween(400),
            label = "phase"
        ) { phase ->
            when (phase) {
                GachaPhase.BANNER_SELECT -> BannerSelectPhase(store)
                GachaPhase.CONFIRM -> ConfirmPhase(store)
                GachaPhase.PULLING -> PullAnimationPhase(store.highestRarity)
                GachaPhase.RESULT -> ResultPhase(store)
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
private fun BannerSelectPhase(store: GachaStore) {
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
            StoneCountBadge(store.currentStones)
        }

        // バナーカード一覧
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            store.banners.forEachIndexed { index, banner ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 120L); visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    BannerCard(banner) { store.selectBanner(banner) }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun BannerCard(banner: BannerDisplay, onClick: () -> Unit) {
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
            .background(Brush.linearGradient(banner.type.colors))
            .drawBehind {
                // shimmer overlay
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                        start = Offset(shimmerX, 0f), end = Offset(shimmerX + 200f, size.height)
                    )
                )
            }
    ) {
        // 大アイコン背景
        val iconRes = when (banner.type) {
            BannerType.CHARACTER -> Icons.Default.Person
            BannerType.WEAPON -> Icons.Default.Shield
            BannerType.MIXED -> Icons.Default.Star
        }
        Icon(
            iconRes, null,
            modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-10).dp),
            tint = Color.White.copy(alpha = 0.1f)
        )

        // テキスト情報
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BadgeChip("PICK UP", Color.White.copy(alpha = 0.25f))
                BadgeChip(banner.type.label, Color.White.copy(alpha = 0.15f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(banner.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(banner.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                repeat(banner.featuredRarity) {
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
private fun ConfirmPhase(store: GachaStore) {
    val coroutineScope = rememberCoroutineScope()
    val banner = store.selectedBanner ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { store.backToBannerSelect() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る", tint = Color.White.copy(alpha = 0.7f))
            }
            Text("バナー選択", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.weight(1f))
            StoneCountBadge(store.currentStones)
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
            Text(banner.description, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(24.dp))

            // 召喚オーブ
            SummoningOrb(banner.type)
            Spacer(modifier = Modifier.height(24.dp))

            // 天井カウンター
            if (banner.pityThreshold > 0) {
                PityCounter(store.pityCount, banner.pityThreshold)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 召喚ボタン
            GlowPullButton(
                "単発召喚", GachaStore.SINGLE_COST, store.canPullSingle, banner.type.colors
            ) { coroutineScope.launch { store.pullSingle() } }
            Spacer(modifier = Modifier.height(12.dp))
            GlowPullButton(
                "10連召喚", GachaStore.MULTI_COST, store.canPullMulti, banner.type.colors, isPrimary = true
            ) { coroutineScope.launch { store.pullMulti() } }
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
        // 外周リング
        Canvas(modifier = Modifier.size(160.dp).rotate(rotation)) {
            drawCircle(
                Brush.sweepGradient(type.colors + listOf(type.colors.first())),
                style = Stroke(width = 3f)
            )
        }
        // 内周リング
        Canvas(modifier = Modifier.size(120.dp).rotate(-rotation * 0.5f)) {
            drawCircle(type.colors.first().copy(alpha = 0.3f), style = Stroke(width = 1.5f))
        }
        // グロー
        Box(
            modifier = Modifier.size(120.dp).scale(pulse).background(
                Brush.radialGradient(listOf(type.colors.last().copy(alpha = 0.4f), Color.Transparent)),
                CircleShape
            )
        )
        // アイコン
        val icon = when (type) {
            BannerType.CHARACTER -> Icons.Default.Person
            BannerType.WEAPON -> Icons.Default.Shield
            BannerType.MIXED -> Icons.Default.Star
        }
        Icon(icon, null, modifier = Modifier.size(40.dp).scale(pulse * 0.9f), tint = Color.White.copy(alpha = 0.8f))
    }
}

// ══════════════════════════════════════════════════════════════
// Result Phase
// ══════════════════════════════════════════════════════════════

@Composable
private fun ResultPhase(store: GachaStore) {
    val coroutineScope = rememberCoroutineScope()
    val sorted = remember(store.pullResults) { store.pullResults.sortedByDescending { it.rarity } }

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("召喚結果", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            StoneCountBadge(store.currentStones)
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (store.lastPullCount == 1 && sorted.isNotEmpty()) {
                SingleResultCard(sorted.first())
            } else {
                // 10連 : 2列グリッド（scrollable column 内なので固定高さで配置）
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
            val cost = if (store.lastPullCount == 1) GachaStore.SINGLE_COST else GachaStore.MULTI_COST
            val canPull = if (store.lastPullCount == 1) store.canPullSingle else store.canPullMulti
            GlowPullButton("もう一度召喚する", cost, canPull,
                listOf(Color(0xFF4D99FF), Color(0xFF804DFF))
            ) { store.pullAgain() }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { store.backToBannerSelect() }) {
                Text("バナー選択に戻る", fontSize = 15.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── 単発結果カード ─────────────────────────────

@Composable
private fun SingleResultCard(item: ResultItem) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); revealed = true }

    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f), label = "s"
    )
    val alpha by animateFloatAsState(
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
            .graphicsLayer { alpha = this@SingleResultCard.alpha; rotationY = rotY }
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        // Rarity glow circle
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(140.dp).background(
                    Brush.radialGradient(
                        listOf(rarityColor(item.rarity).copy(alpha = 0.4f), rarityColor(item.rarity).copy(alpha = 0.05f), Color.Transparent)
                    ), CircleShape
                )
            )
            val icon = if (item.type == ItemType.CHARACTER) Icons.Default.Person else Icons.Default.Shield
            Icon(icon, null, modifier = Modifier.size(60.dp), tint = Color.White)
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
        Text(item.type.label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

// ── 10連結果カード ─────────────────────────────

@Composable
private fun MultiResultCard(item: ResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // アイコン + レアリティ光彩
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(48.dp).background(
                    Brush.radialGradient(
                        listOf(rarityColor(item.rarity).copy(alpha = 0.35f), Color.Transparent)
                    ), CircleShape
                )
            )
            val icon = if (item.type == ItemType.CHARACTER) Icons.Default.Person else Icons.Default.Shield
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.White)
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

// ── coroutineScope helper (import) ──

private fun kotlinx.coroutines.CoroutineScope.launch(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.launch(block = block)
}
