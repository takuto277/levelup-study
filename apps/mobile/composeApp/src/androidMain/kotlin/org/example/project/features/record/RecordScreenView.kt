package org.example.project.features.record

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── カラーパレット ─────────────────────────────────
private val BgColor = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val TextTertiary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)

private fun genreColor(genre: StudyGenre): Color = Color(genre.colorHex)

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatHoursMinutes(minutes: Int): Pair<String, String> {
    val h = minutes / 60
    val m = minutes % 60
    return Pair(h.toString(), if (m < 10) "0$m" else m.toString())
}

// ── メインView ────────────────────────────────────

@Composable
fun RecordScreenView() {
    val viewModel = remember { RecordViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ヘッダー + キャラ吹き出し
        RecordHeader(uiState)

        Spacer(modifier = Modifier.height(12.dp))

        // 累計勉強時間ヒーローカード
        TotalStudyCard(uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // 期間タブ
        PeriodTabs(
            selected = uiState.selectedPeriod,
            onSelect = { viewModel.onIntent(RecordIntent.SelectPeriod(it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 期間サマリー
        PeriodSummaryCard(uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // 棒グラフ
        BarChartCard(uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // ジャンル別内訳
        GenreBreakdownCard(
            uiState = uiState,
            onGenreSelect = { viewModel.onIntent(RecordIntent.SelectGenre(it)) }
        )

        Spacer(modifier = Modifier.height(120.dp))
    }
}

// ── ヘッダー + キャラ吹き出し ──────────────────────

@Composable
private fun RecordHeader(uiState: RecordUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // タイトル
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "記録",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
            Text(
                "あなたの冒険の軌跡",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // キャラ吹き出し（小さめ）
        Row(verticalAlignment = Alignment.Bottom) {
            // 吹き出し
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(CardWhite, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .widthIn(max = 140.dp)
            ) {
                Text(
                    uiState.characterMessage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            // キャラ
            Text(uiState.characterEmoji, fontSize = 36.sp)
        }
    }
}

// ── 累計勉強時間ヒーローカード ──────────────────────

@Composable
private fun TotalStudyCard(uiState: RecordUiState) {
    val (hours, mins) = formatHoursMinutes(uiState.totalStudyMinutes)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                "🏆 累計勉強時間",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    hours,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 56.sp
                )
                Text(
                    "h",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    mins,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 56.sp
                )
                Text(
                    "m",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatPill("🔥", "${uiState.streakDays}日連続")
                StatPill("📖", "今日 ${uiState.todaySessions}回")
            }
        }
    }
}

@Composable
private fun StatPill(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ── 期間タブ ──────────────────────────────────────

@Composable
private fun PeriodTabs(selected: RecordPeriod, onSelect: (RecordPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RecordPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) CardWhite else Color.Transparent)
                    .clickable { onSelect(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    period.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AccentBlue else TextSecondary
                )
            }
        }
    }
}

// ── 期間サマリー ──────────────────────────────────

@Composable
private fun PeriodSummaryCard(uiState: RecordUiState) {
    val periodLabel = when (uiState.selectedPeriod) {
        RecordPeriod.TODAY -> "今日の勉強時間"
        RecordPeriod.WEEKLY -> "今週の勉強時間"
        RecordPeriod.MONTHLY -> "今月の勉強時間"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(CardWhite, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(periodLabel, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatMinutes(uiState.periodTotalMinutes),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        // ミニドーナツ（ジャンル比率を可視化）
        if (uiState.genreBreakdown.isNotEmpty()) {
            MiniDonutChart(
                breakdown = uiState.genreBreakdown,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun MiniDonutChart(breakdown: List<GenreStudyTime>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 10f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        var startAngle = -90f

        for (item in breakdown) {
            val sweep = item.ratio * 360f
            drawArc(
                color = genreColor(item.genre),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

// ── 棒グラフ ──────────────────────────────────────

@Composable
private fun BarChartCard(uiState: RecordUiState) {
    val bars = uiState.chartBars
    if (bars.isEmpty()) return

    val maxMinutes = bars.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val selectedGenre = uiState.selectedGenre

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(CardWhite, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            "学習時間の推移",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (selectedGenre != null) "${selectedGenre.emoji} ${selectedGenre.label}のみ表示中"
            else "全ジャンル",
            fontSize = 11.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // スクロール可能なバーチャート
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                BarItem(
                    bar = bar,
                    maxMinutes = maxMinutes,
                    selectedGenre = selectedGenre,
                    barCount = bars.size
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    bar: ChartBar,
    maxMinutes: Int,
    selectedGenre: StudyGenre?,
    barCount: Int
) {
    val fraction = bar.minutes.toFloat() / maxMinutes
    val barHeight = (130 * fraction).coerceAtLeast(4f)
    val barWidth = when {
        barCount <= 7 -> 36.dp
        barCount <= 14 -> 28.dp
        else -> 22.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(barWidth)
    ) {
        // 時間ラベル
        if (bar.minutes > 0) {
            Text(
                "${bar.minutes}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // 積み上げバー
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (selectedGenre != null) {
                // 単色バー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(genreColor(selectedGenre))
                )
            } else {
                // 積み上げバー
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val totalMin = bar.genreMinutes.values.sum().coerceAtLeast(1)
                    bar.genreMinutes.entries.sortedByDescending { it.value }.forEach { (genre, min) ->
                        val segmentHeight = (barHeight * min / totalMin).coerceAtLeast(2f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(segmentHeight.dp)
                                .background(genreColor(genre))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日付ラベル
        Text(
            bar.label,
            fontSize = 8.sp,
            color = TextTertiary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ── ジャンル別内訳 ────────────────────────────────

@Composable
private fun GenreBreakdownCard(
    uiState: RecordUiState,
    onGenreSelect: (StudyGenre?) -> Unit
) {
    val breakdown = uiState.genreBreakdown
    if (breakdown.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(CardWhite, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ジャンル別内訳",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (uiState.selectedGenre != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { onGenreSelect(null) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("全表示", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ジャンルチップ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            breakdown.forEach { item ->
                GenreChip(
                    item = item,
                    isSelected = uiState.selectedGenre == item.genre,
                    onClick = { onGenreSelect(item.genre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 各ジャンルの詳細バー
        breakdown.forEach { item ->
            GenreBarRow(item = item, isHighlighted = uiState.selectedGenre == null || uiState.selectedGenre == item.genre)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GenreChip(item: GenreStudyTime, isSelected: Boolean, onClick: () -> Unit) {
    val color = genreColor(item.genre)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) Modifier.background(color.copy(alpha = 0.15f))
                    .border(1.5.dp, color, RoundedCornerShape(20.dp))
                else Modifier.background(Color(0xFFF1F5F9))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.genre.emoji, fontSize = 14.sp)
            Text(
                item.genre.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else TextSecondary
            )
        }
    }
}

@Composable
private fun GenreBarRow(item: GenreStudyTime, isHighlighted: Boolean) {
    val color = genreColor(item.genre)
    val alpha = if (isHighlighted) 1f else 0.35f
    val percentage = (item.ratio * 100).toInt()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(item.genre.emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.genre.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = alpha)
                )
                Text(
                    "${formatMinutes(item.minutes)} ($percentage%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = alpha)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // プログレスバー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = alpha))
                )
            }
        }
    }
}
