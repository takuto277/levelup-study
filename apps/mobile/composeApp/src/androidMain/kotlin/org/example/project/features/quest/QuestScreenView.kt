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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.model.DungeonDifficulty

// ── カラーパレット ───────────────────────────────────
private val BgColor = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val GoldColor = Color(0xFFFFD700)
private val ExpGreen = Color(0xFF10B981)
private val GachaViolet = Color(0xFF8B5CF6)

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

// ── メイン画面 ──────────────────────────────────────
@Composable
fun QuestScreenView() {
    val viewModel = remember { org.example.project.di.getQuestViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ヘッダー
            QuestHeader()

            // ダンジョン一覧
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val available = uiState.dungeons.filter { !it.isLocked }
                if (available.isNotEmpty()) {
                    item { SectionTitle("🗺️ 挑戦可能なダンジョン") }
                    items(available, key = { it.id }) { dungeon ->
                        DungeonCard(
                            dungeon = dungeon,
                            onClick = { viewModel.onIntent(QuestIntent.SelectDungeon(dungeon.id)) }
                        )
                    }
                }

                // セクション: ロック中
                val locked = uiState.dungeons.filter { it.isLocked }
                if (locked.isNotEmpty()) {
                    item { SectionTitle("🔒 未解放ダンジョン") }
                    items(locked, key = { it.id }) { dungeon ->
                        DungeonCard(
                            dungeon = dungeon,
                            onClick = { /* ロック中はタップ無効 or 解放条件表示 */ },
                            isLocked = true
                        )
                    }
                }

                // タブバー分の余白
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        uiState.selectedDungeon?.let { dungeon ->
            val homeVm = remember { org.example.project.di.getHomeViewModel() }
            DungeonDetailSheet(
                dungeon = dungeon,
                onDismiss = { viewModel.onIntent(QuestIntent.DismissDetail) },
                onSelect = {
                    homeVm.onIntent(
                        org.example.project.features.home.HomeIntent.SelectDungeon(
                            id = dungeon.id, name = dungeon.name
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
    onClick: () -> Unit,
    isLocked: Boolean = false
) {
    val alpha = if (isLocked) 0.5f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .shadow(
                elevation = if (isLocked) 2.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = difficultyColor(dungeon.difficulty).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        color = CardWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            // カード上部: アイコン + 情報
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ダンジョンアイコン
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(difficultyGradient(dungeon.difficulty)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        Text("🔒", fontSize = 24.sp)
                    } else {
                        Text(dungeon.iconEmoji, fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // テキスト情報
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dungeon.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 難易度バッジ
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = difficultyColor(dungeon.difficulty).copy(alpha = 0.12f)
                        ) {
                            Text(
                                dungeon.difficulty.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = difficultyColor(dungeon.difficulty),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        dungeon.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // メタ情報
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetaChip("🕐 ${dungeon.recommendedMinutes}分")
                        Spacer(modifier = Modifier.width(8.dp))
                        MetaChip("${dungeon.category.emoji} ${dungeon.category.label}")
                        Spacer(modifier = Modifier.width(8.dp))
                        // 難易度スター
                        Row {
                            repeat(dungeon.difficulty.stars) {
                                Text("⭐", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // 右矢印
                if (!isLocked) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "詳細",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // -- プログレスバー削除済み --

            // 報酬プレビュー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFBFC))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RewardChip("✨", "${dungeon.rewards.exp} EXP", ExpGreen)
                dungeon.rewards.bonusItemName?.let { itemName ->
                    RewardChip("🎁", itemName, Color(0xFFEC4899))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                // ダンジョンヒーロー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(16.dp)
                        .background(
                            brush = Brush.linearGradient(
                                difficultyGradient(dungeon.difficulty)
                                    .map { it.copy(alpha = 0.15f) }
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dungeon.iconEmoji, fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            dungeon.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = difficultyColor(dungeon.difficulty).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    dungeon.difficulty.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = difficultyColor(dungeon.difficulty),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${dungeon.category.emoji} ${dungeon.category.label}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 説明
                Text(
                    dungeon.description,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ダンジョン情報
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ダンジョン情報",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailInfoRow("推奨時間", "🕐 ${dungeon.recommendedMinutes}分")
                        DetailInfoRow("総ステージ", "📍 ${dungeon.totalStages}ステージ")
                        DetailInfoRow(
                            "難易度",
                            buildString { repeat(dungeon.difficulty.stars) { append("⭐") } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 報酬詳細
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(16.dp)
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
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    difficultyGradient(dungeon.difficulty)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "出発",
                                tint = Color.White
                            )
                            Text(
                                "このダンジョンに出発する",
                                fontSize = 16.sp,
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
