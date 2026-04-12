package org.example.project.features.quest

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.local.LocalDungeonIds
import org.example.project.domain.model.DungeonDifficulty

// ── カラーパレット（青テーマ）──────────────────────────
private val BgColor = Color(0xFF0B1120)
private val CardWhite = Color(0xFF111B2E)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentCyan = Color(0xFF22D3EE)
private val GoldColor = Color(0xFFFFD700)
private val ExpGreen = Color(0xFF10B981)
private val GachaViolet = Color(0xFF8B5CF6)
private val BgSurface = Color(0xFF1A2744)

private fun difficultyColor(difficulty: DungeonDifficulty): Color = when (difficulty) {
    DungeonDifficulty.BEGINNER -> Color(0xFF10B981)
    DungeonDifficulty.INTERMEDIATE -> Color(0xFF3B82F6)
    DungeonDifficulty.ADVANCED -> Color(0xFFF59E0B)
    DungeonDifficulty.EXPERT -> Color(0xFFEF4444)
    DungeonDifficulty.LEGENDARY -> Color(0xFF8B5CF6)
}

private fun difficultyGradient(difficulty: DungeonDifficulty): List<Color> = when (difficulty) {
    DungeonDifficulty.BEGINNER -> listOf(Color(0xFF10B981), Color(0xFF34D399))
    DungeonDifficulty.INTERMEDIATE -> listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
    DungeonDifficulty.ADVANCED -> listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
    DungeonDifficulty.EXPERT -> listOf(Color(0xFFEF4444), Color(0xFFF87171))
    DungeonDifficulty.LEGENDARY -> listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
}

/** 冒険タブ用: 同梱 `bg_dungeon_*` のみ。未一致は訓練場。 */
private fun questBannerDrawableId(context: android.content.Context, dungeon: Dungeon): Int {
    fun rid(base: String): Int =
        context.resources.getIdentifier(base, "drawable", context.packageName)
    val suffix = when {
        LocalDungeonIds.isTrainingGround(dungeon.id) -> "training"
        dungeon.name.contains("森") || dungeon.name.contains("forest", ignoreCase = true) -> "forest"
        dungeon.name.contains("洞窟") || dungeon.name.contains("水晶") ||
            dungeon.name.contains("cave", ignoreCase = true) -> "cave"
        dungeon.name.contains("塔") || dungeon.name.contains("炎") ||
            dungeon.name.contains("tower", ignoreCase = true) -> "tower"
        else -> "training"
    }
    val primary = rid("bg_dungeon_$suffix")
    if (primary != 0) return primary
    val training = rid("bg_dungeon_training")
    return if (training != 0) training else 0
}

// ── メイン画面 ──────────────────────────────────────
@Composable
fun QuestScreenView() {
    val viewModel = remember { org.example.project.di.getQuestViewModel() }
    val homeVm = remember { org.example.project.di.getHomeViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val homeState by homeVm.uiState.collectAsState()
    val selectedId = homeState.selectedDungeonId

    val available = uiState.dungeons.filter { !it.isLocked }

    LaunchedEffect(available, selectedId) {
        if (available.isNotEmpty()) {
            val hasValid = selectedId != null && available.any { it.id == selectedId }
            if (!hasValid) {
                val pick = available.firstOrNull { it.isFromServer }
                    ?: available.first()
                homeVm.onIntent(
                    org.example.project.features.home.HomeIntent.SelectDungeon(
                        id = pick.id,
                        name = pick.name,
                        imageUrl = if (LocalDungeonIds.isTrainingGround(pick.id)) null else pick.imageUrl.takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgColor, Color(0xFF0F172A))))) {
        Column(modifier = Modifier.fillMaxSize()) {
            QuestHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (available.isNotEmpty()) {
                    item { SectionTitle("🗺️ 挑戦可能なダンジョン") }
                    items(available, key = { it.id }) { dungeon ->
                        DungeonCard(
                            dungeon = dungeon,
                            isSelected = dungeon.id == selectedId,
                            onClick = { viewModel.onIntent(QuestIntent.SelectDungeon(dungeon.id)) }
                        )
                    }
                }

                val locked = uiState.dungeons.filter { it.isLocked }
                if (locked.isNotEmpty()) {
                    item { SectionTitle("🔒 未解放ダンジョン") }
                    items(locked, key = { it.id }) { dungeon ->
                        DungeonCard(
                            dungeon = dungeon,
                            isSelected = false,
                            onClick = { },
                            isLocked = true
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        uiState.selectedDungeon?.let { dungeon ->
            DungeonDetailSheet(
                dungeon = dungeon,
                isCurrentlySelected = dungeon.id == selectedId,
                onDismiss = { viewModel.onIntent(QuestIntent.DismissDetail) },
                onSelect = {
                    homeVm.onIntent(
                        org.example.project.features.home.HomeIntent.SelectDungeon(
                            id = dungeon.id,
                            name = dungeon.name,
                            imageUrl = if (LocalDungeonIds.isTrainingGround(dungeon.id)) null else dungeon.imageUrl.takeIf { it.isNotBlank() }
                        )
                    )
                    viewModel.onIntent(QuestIntent.DismissDetail)
                }
            )
        }
    }
}

// ── ヘッダー ─────────────────────────────────────────
@Composable
private fun QuestHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.08f), Color.Transparent)
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("⚔️", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "冒険",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "ダンジョンを選んで勉強を始めよう",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

// ── セクションタイトル ───────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ── ダンジョンカード ─────────────────────────────────
@Composable
private fun DungeonCard(
    dungeon: Dungeon,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    isLocked: Boolean = false
) {
    val alpha = if (isLocked) 0.5f else 1f
    val bgColor = if (isSelected) AccentBlue.copy(alpha = 0.12f) else CardWhite
    val borderColor = when {
        isSelected -> AccentCyan.copy(alpha = 0.6f)
        LocalDungeonIds.isTrainingGround(dungeon.id) -> AccentCyan.copy(alpha = 0.22f)
        else -> difficultyColor(dungeon.difficulty).copy(alpha = 0.15f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column {
            val context = LocalContext.current
            val bannerId = remember(dungeon.id, dungeon.name) { questBannerDrawableId(context, dungeon) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (bannerId != 0) {
                    Image(
                        painter = painterResource(bannerId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    difficultyGradient(dungeon.difficulty).map { it.copy(alpha = 0.4f) }
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CardWhite.copy(alpha = 0.98f))
                            )
                        )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLocked) {
                            Text("🔒 ", fontSize = 14.sp)
                        }
                        Text(
                            dungeon.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = AccentCyan) {
                                Text("選択中", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    if (dungeon.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(dungeon.description, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetaChip(dungeon.estimatedLevelChipText())
                    }
                }

                if (!isLocked) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "詳細", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = BgSurface
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RewardChip(emoji: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ── ダンジョン詳細シート ─────────────────────────────
@Composable
private fun DungeonDetailSheet(
    dungeon: Dungeon,
    isCurrentlySelected: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: () -> Unit = {}
) {
    // 半透明の背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        // ボトムシートカード
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.BottomCenter)
                .clickable(enabled = false, onClick = {}), // タップ透過防止
            color = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ドラッグハンドル
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                )

                // ダンジョンヒーロー（同梱 bg_dungeon_* のみ・デフォルト訓練場）
                val sheetContext = LocalContext.current
                val heroBannerId = remember(dungeon.id, dungeon.name) { questBannerDrawableId(sheetContext, dungeon) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    if (heroBannerId != 0) {
                        Image(
                            painter = painterResource(heroBannerId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        difficultyGradient(dungeon.difficulty).map { it.copy(alpha = 0.35f) }
                                    )
                                )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(dungeon.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Surface(shape = RoundedCornerShape(8.dp), color = AccentCyan.copy(alpha = 0.18f)) {
                            Text(
                                "推定Lv.${dungeon.estimatedRecommendedLevel()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        if (isCurrentlySelected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = AccentCyan.copy(alpha = 0.2f)) {
                                Text("✅ 現在選択中", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = AccentCyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (dungeon.description.isNotBlank()) {
                    Text(
                        dungeon.description,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = BgSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ダンジョン情報",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailInfoRow("総ステージ", "📍 ${dungeon.totalStages}ステージ")
                        DetailInfoRow("推定レベル", "Lv.${dungeon.estimatedRecommendedLevel()}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = BgSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎁 クリア報酬",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RewardDetailItem("✨", "経験値", "${dungeon.rewards.exp} EXP", ExpGreen)
                        }

                        dungeon.rewards.bonusItemName?.let { itemName ->
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎁", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            itemName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEC4899)
                                        )
                                        Text(
                                            "レアドロップ",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEC4899).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "確率 ${(dungeon.rewards.bonusItemDropRate * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEC4899),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onSelect() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    if (isCurrentlySelected) listOf(AccentCyan, AccentBlue) else listOf(AccentBlue, AccentIndigo)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isCurrentlySelected) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = "選択",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (isCurrentlySelected) "選択済み" else "このダンジョンを選択する",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun RewardDetailItem(emoji: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}
