package org.example.project.features.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.features.home.HomeTheme
import org.example.project.features.home.HomeTheme.AccentBlue

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        emoji = "\uD83D\uDCDA",
        title = "勉強を始めよう",
        description = "勉強時間があなたの冒険の力になります。\n集中して学べば学ぶほど、戦闘力が上がります。",
    ),
    OnboardingPage(
        emoji = "\u2694\uFE0F",
        title = "冒険に出かけよう",
        description = "勉強が進むと新しいダンジョンが解放されます。\n敵を倒して経験値と報酬を手に入れましょう。",
    ),
    OnboardingPage(
        emoji = "\uD83C\uDFC6",
        title = "報酬を集めよう",
        description = "冒険で得た石とゴールドで召喚や装備強化ができます。\n最強のパーティを編成しましょう。",
    ),
    OnboardingPage(
        emoji = "\u2601\uFE0F",
        title = "いつでも同期",
        description = "オフラインでも進捗は端末に保存されます。\nネット接続時に自動でサーバーと同期します。",
    ),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val isLastPage = currentPage >= pages.lastIndex
    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeTheme.BgColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = page.emoji,
                fontSize = 72.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTheme.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                fontSize = 15.sp,
                color = HomeTheme.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentPage) AccentBlue
                                else HomeTheme.TextSecondary.copy(alpha = 0.4f)
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        currentPage++
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                Text(
                    text = if (isLastPage) "勉強を始める" else "次へ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (!isLastPage) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "スキップ",
                        fontSize = 14.sp,
                        color = HomeTheme.TextSecondary,
                    )
                }
            }
        }
    }
}
