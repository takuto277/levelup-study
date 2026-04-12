package org.example.project.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ダンジョン名・ジャンル選択を横一列で表示（勉強時間はヘッダーの累計勉強で表示）。 */
@Composable
internal fun HomeAdventureContextRow(
    dungeonName: String?,
    /** 樽打ち訓練の勉強シーン（オフライン or 訓練場ダンジョン選択） */
    isTrainingStudySession: Boolean = false,
    selectedGenreSlug: String,
    genreTriples: List<Triple<String, String, String>>,
    onGenreChange: (String) -> Unit,
    onAddGenreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContextStatChip(
            label = if (isTrainingStudySession) "モード" else "ダンジョン",
            value = if (isTrainingStudySession) "訓練場" else (dungeonName ?: "—"),
            modifier = Modifier.defaultMinSize(minWidth = 96.dp)
        )
        GenrePickerChip(
            genreTriples = genreTriples,
            selectedSlug = selectedGenreSlug,
            onGenreChange = onGenreChange,
            modifier = Modifier.widthIn(min = 108.dp, max = 168.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HomeTheme.CardWhite)
                .border(1.dp, HomeTheme.AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable(onClick = onAddGenreClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "ジャンル管理",
                tint = HomeTheme.AccentIndigo,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ContextStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HomeTheme.CardWhite)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 9.sp, color = HomeTheme.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = HomeTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GenrePickerChip(
    genreTriples: List<Triple<String, String, String>>,
    selectedSlug: String,
    onGenreChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = genreTriples.find { it.first == selectedSlug } ?: genreTriples.first()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HomeTheme.CardWhite)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Text("ジャンル", fontSize = 9.sp, color = HomeTheme.TextSecondary)
                Text(
                    "${selected.second} ${selected.third}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeTheme.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = HomeTheme.AccentCyan)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            genreTriples.forEach { (slug, emoji, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "$emoji $label",
                            fontWeight = if (slug == selectedSlug) FontWeight.Bold else FontWeight.Medium,
                            color = if (slug == selectedSlug) HomeTheme.AccentCyan else HomeTheme.TextPrimary
                        )
                    },
                    onClick = {
                        onGenreChange(slug)
                        expanded = false
                    }
                )
            }
        }
    }
}
