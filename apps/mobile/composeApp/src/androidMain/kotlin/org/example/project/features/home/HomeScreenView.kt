package org.example.project.features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image

// ── カラー（青テーマ）────────────────────────────
private val BgColor = Color(0xFF0B1120)
private val BgDark2 = Color(0xFF0F172A)
private val CardWhite = Color(0xFF111B2E)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentCyan = Color(0xFF22D3EE)
private val GoldYellow = Color(0xFFFFD700)
private val FireRed = Color(0xFFEF4444)
private val FireOrange = Color(0xFFF59E0B)
private val EmeraldGreen = Color(0xFF10B981)
private val BgSurface = Color(0xFF1A2744)

@Composable
fun HomeScreenView() {
    var selectedTab by remember { mutableStateOf(2) }
    var showStudySheet by remember { mutableStateOf(false) }
    var studyMinutes by remember { mutableStateOf(25) }
    var selectedGenreSlug by remember { mutableStateOf("general") }

    val homeViewModel = remember { org.example.project.di.getHomeViewModel() }
    val homeState by homeViewModel.uiState.collectAsState()

    if (showStudySheet) {
        org.example.project.features.study.StudyQuestScreenView(
            initialStudyMinutes = studyMinutes,
            genreId = selectedGenreSlug,
            dungeonName = homeState.selectedDungeonName,
            onDismiss = { showStudySheet = false }
        )
    } else {
        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgColor, BgDark2)))) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    when (selectedTab) {
                        0 -> org.example.project.features.quest.QuestScreenView()
                        1 -> org.example.project.features.party.PartyScreenView()
                        2 -> HomeTabContent(
                            studyMinutes = studyMinutes,
                            onStudyMinutesChange = { studyMinutes = it },
                            selectedGenreSlug = selectedGenreSlug,
                            onGenreChange = { selectedGenreSlug = it },
                            onStartStudy = { showStudySheet = true },
                            homeState = homeState,
                            homeViewModel = homeViewModel
                        )
                        3 -> org.example.project.features.gacha.GachaScreenView()
                        4 -> org.example.project.features.record.RecordScreenView()
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Home Tab — リッチなキャラクター + 吹き出し + 没入感
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun HomeTabContent(
    studyMinutes: Int,
    onStudyMinutesChange: (Int) -> Unit,
    selectedGenreSlug: String,
    onGenreChange: (String) -> Unit,
    onStartStudy: () -> Unit,
    homeState: org.example.project.features.home.HomeUiState,
    homeViewModel: org.example.project.features.home.HomeViewModel
) {
    var showAddGenreDialog by remember { mutableStateOf(false) }
    var newGenreLabel by remember { mutableStateOf("") }
    var newGenreEmoji by remember { mutableStateOf("📖") }

    val genres = remember(homeState.genres) {
        if (homeState.genres.isEmpty()) listOf(Triple("general", "📚", "総合"))
        else homeState.genres.map { Triple(it.slug, it.emoji, it.label) }
    }
    // アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "home")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    // セリフ切り替え
    val messages = remember {
        listOf(
            "今日の特訓も頑張ろうな！",
            "知識こそ最強の武器だ。",
            "お前の成長、楽しみにしてるぞ。",
            "さぁ、冒険の時間だ！",
            "集中すれば、何でもできる。"
        )
    }
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000L)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    if (showAddGenreDialog) {
        AlertDialog(
            onDismissRequest = { showAddGenreDialog = false },
            title = { Text("ジャンル追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newGenreLabel,
                        onValueChange = { newGenreLabel = it },
                        label = { Text("ジャンル名") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("📖", "📐", "🧪", "🌍", "🎵", "⚽", "🎮", "✏️").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (newGenreEmoji == emoji) AccentBlue.copy(0.2f) else Color.Transparent)
                                    .clickable { newGenreEmoji = emoji }
                                    .padding(6.dp)
                            ) { Text(emoji, fontSize = 22.sp) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGenreLabel.isNotEmpty()) {
                            homeViewModel.onIntent(HomeIntent.AddGenre(newGenreLabel, newGenreEmoji, "#6B7280"))
                            newGenreLabel = ""; newGenreEmoji = "📖"
                            showAddGenreDialog = false
                        }
                    }
                ) { Text("追加") }
            },
            dismissButton = { TextButton(onClick = { showAddGenreDialog = false }) { Text("キャンセル") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHeader(homeState = homeState)

        homeState.selectedDungeonName?.let { dn ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(AccentBlue.copy(0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("次の目的地:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(dn, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // ── キャラクターエリア ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 背景オーラ
            Box(
                modifier = Modifier
                    .size((240 * breathScale).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentBlue.copy(alpha = glowAlpha * 0.3f),
                                AccentIndigo.copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 吹き出し
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(CardWhite, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "「${messages[messageIndex]}」",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                // 吹き出し三角
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .background(CardWhite)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                val context = LocalContext.current
                val playerIdleRes = remember {
                    context.resources.getIdentifier("sprite_player_idle_1", "drawable", context.packageName)
                }
                if (playerIdleRes != 0) {
                    Image(
                        painter = painterResource(playerIdleRes),
                        contentDescription = "Player",
                        modifier = Modifier
                            .size(160.dp)
                            .offset(y = bounceY.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    Text(
                        "🧙‍♂️",
                        fontSize = 100.sp,
                        modifier = Modifier.offset(y = bounceY.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // キャラ名 + Lv
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("マーリン", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Lv.24", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // ── 冒険時間設定 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏱ 冒険時間", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(CardWhite, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // マイナスボタン
                IconButton(
                    onClick = { if (studyMinutes > 5) onStudyMinutesChange(studyMinutes - 5) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Decrease", tint = AccentBlue)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "$studyMinutes",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text("分", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { if (studyMinutes < 120) onStudyMinutesChange(studyMinutes + 5) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Increase", tint = AccentBlue)
                    }
                }
            }

            // クイック選択
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60).forEach { min ->
                    val isSelected = studyMinutes == min
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) AccentBlue else Color(0xFFE2E8F0)
                            )
                            .clickable { onStudyMinutesChange(min) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${min}分",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── ジャンル選択 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📖 ジャンル", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in genres.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        row.forEach { (slug, emoji, label) ->
                            val isSelected = selectedGenreSlug == slug
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AccentIndigo else Color(0xFFE2E8F0))
                                    .clickable { onGenreChange(slug) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary)
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(0.1f))
                        .clickable { showAddGenreDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Text("追加", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 出発ボタン ──
        Button(
            onClick = onStartStudy,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(60.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(listOf(FireRed, FireOrange)),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚔️", fontSize = 22.sp)
                    Text("冒険に出発する", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── ヘッダー ─────────────────────────────────

@Composable
private fun HomeHeader(homeState: org.example.project.features.home.HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(CardWhite, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📖", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("累計勉強", fontSize = 9.sp, color = TextSecondary)
                Text(homeState.formattedStudyTime, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
        }

        Row(
            modifier = Modifier
                .background(CardWhite, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💎", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("知識の結晶", fontSize = 9.sp, color = TextSecondary)
                Text("${homeState.stones}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// BottomNavigationBar / NavItem / Placeholder — 変更なし
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val BarBg = Color(0xFF0F172A)
private val BarStroke = Color(0xFF263859)

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = BarBg.copy(alpha = 0.96f),
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BarStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Default.Place, "冒険", selectedTab == 0) { onTabSelected(0) }
                NavItem(Icons.Default.Person, "編成", selectedTab == 1) { onTabSelected(1) }
                NavItem(Icons.Default.Home, "ホーム", selectedTab == 2, isCenter = true) { onTabSelected(2) }
                NavItem(Icons.Default.Star, "召喚", selectedTab == 3) { onTabSelected(3) }
                NavItem(Icons.Default.Info, "記録", selectedTab == 4) { onTabSelected(4) }
            }
        }
    }
}

private val NavCyan = Color(0xFF22D3EE)
private val NavDim = Color(0xFF64748B)

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, isCenter: Boolean = false, onClick: () -> Unit) {
    val color = if (isSelected) NavCyan else NavDim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .width(60.dp)
    ) {
        Box(
            modifier = Modifier.height(40.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isCenter) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(AccentBlue, AccentIndigo)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            } else {
                if (isSelected) {
                    Surface(
                        color = AccentBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 44.dp, height = 30.dp)
                    ) {}
                }
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            label,
            fontSize = 10.sp,
            color = if (isCenter) Color.White else color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = if (isCenter) 4.dp else 0.dp)
        )
    }
}

@Composable
fun PlaceholderScreen(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = title, modifier = Modifier.size(60.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}
