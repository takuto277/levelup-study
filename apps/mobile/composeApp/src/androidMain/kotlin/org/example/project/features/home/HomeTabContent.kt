package org.example.project.features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun HomeTabContent(
    studyMinutes: Int,
    onStudyMinutesChange: (Int) -> Unit,
    selectedGenreSlug: String,
    onGenreChange: (String) -> Unit,
    onStartStudy: () -> Unit,
    homeState: HomeUiState,
    homeViewModel: org.example.project.features.home.HomeViewModel
) {
    var showAddGenreDialog by remember { mutableStateOf(false) }
    var newGenreLabel by remember { mutableStateOf("") }
    var newGenreEmoji by remember { mutableStateOf("📖") }

    val genreTriples = remember(homeState.genres) {
        if (homeState.genres.isEmpty()) listOf(Triple("general", "📚", "総合"))
        else homeState.genres.map { Triple(it.slug, it.emoji, it.label) }
    }

    LaunchedEffect(genreTriples.map { it.first }.joinToString(), selectedGenreSlug) {
        val slugs = genreTriples.map { it.first }.toSet()
        if (selectedGenreSlug !in slugs && slugs.isNotEmpty()) {
            onGenreChange(genreTriples.first().first)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

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
        AddGenreDialog(
            newGenreLabel = newGenreLabel,
            onLabelChange = { newGenreLabel = it },
            newGenreEmoji = newGenreEmoji,
            onEmojiChange = { newGenreEmoji = it },
            onDismiss = { showAddGenreDialog = false },
            onConfirm = {
                if (newGenreLabel.isNotEmpty()) {
                    homeViewModel.onIntent(HomeIntent.AddGenre(newGenreLabel, newGenreEmoji, "#6B7280"))
                    newGenreLabel = ""
                    newGenreEmoji = "📖"
                    showAddGenreDialog = false
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHeader(homeState = homeState)

        HomeAdventureContextRow(
            dungeonName = homeState.selectedDungeonName,
            selectedGenreSlug = selectedGenreSlug,
            genreTriples = genreTriples,
            onGenreChange = onGenreChange,
            onAddGenreClick = { showAddGenreDialog = true }
        )

        Spacer(modifier = Modifier.weight(0.4f))

        HomeCharacterHero(
            messages = messages,
            messageIndex = messageIndex,
            bounceY = bounceY,
            glowAlpha = glowAlpha,
            breathScale = breathScale,
            homeState = homeState
        )

        Spacer(modifier = Modifier.weight(0.3f))

        StudyDurationStepper(
            studyMinutes = studyMinutes,
            onStudyMinutesChange = onStudyMinutesChange
        )

        Spacer(modifier = Modifier.height(28.dp))

        StartAdventureButton(onClick = onStartStudy)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(homeState: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCapsule(emoji = "📖", caption = "累計勉強", value = homeState.formattedStudyTime)
        HeaderCapsule(emoji = "💎", caption = "知識の結晶", value = "${homeState.stones}")
    }
}

@Composable
private fun HeaderCapsule(emoji: String, caption: String, value: String) {
    Row(
        modifier = Modifier
            .background(HomeTheme.CardWhite, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(caption, fontSize = 9.sp, color = HomeTheme.TextSecondary)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = HomeTheme.TextPrimary)
        }
    }
}

@Composable
private fun HomeCharacterHero(
    messages: List<String>,
    messageIndex: Int,
    bounceY: Float,
    glowAlpha: Float,
    breathScale: Float,
    homeState: HomeUiState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size((240 * breathScale).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HomeTheme.AccentBlue.copy(alpha = glowAlpha * 0.3f),
                            HomeTheme.AccentIndigo.copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpeechBubble(text = messages[messageIndex])
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
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("🧙‍♂️", fontSize = 100.sp, modifier = Modifier.offset(y = bounceY.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val name = homeState.mainCharacter?.character?.name ?: "冒険者"
                val lv = homeState.mainCharacter?.level ?: 1
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HomeTheme.TextPrimary)
                Box(
                    modifier = Modifier
                        .background(HomeTheme.AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Lv.$lv", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HomeTheme.AccentBlue)
                }
            }
        }
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .background(HomeTheme.CardWhite, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💬", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "「$text」",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeTheme.TextPrimary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 10.dp)
                .background(HomeTheme.CardWhite)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        )
    }
}

@Composable
private fun StudyDurationStepper(
    studyMinutes: Int,
    onStudyMinutesChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏱ 冒険時間", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HomeTheme.TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .background(HomeTheme.CardWhite, RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = { onStudyMinutesChange(studyMinutesDecrease(studyMinutes)) },
                enabled = studyMinutes > 1
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HomeTheme.AccentBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "減らす",
                        tint = if (studyMinutes > 1) HomeTheme.AccentBlue else HomeTheme.TextSecondary.copy(alpha = 0.35f)
                    )
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
                    color = HomeTheme.TextPrimary
                )
                Text("分", fontSize = 14.sp, color = HomeTheme.TextSecondary, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = { onStudyMinutesChange(studyMinutesIncrease(studyMinutes)) },
                enabled = studyMinutes < 60
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HomeTheme.AccentBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "増やす",
                        tint = if (studyMinutes < 60) HomeTheme.AccentBlue else HomeTheme.TextSecondary.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartAdventureButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
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
                    brush = Brush.linearGradient(listOf(HomeTheme.FireRed, HomeTheme.FireOrange)),
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
}

@Composable
private fun AddGenreDialog(
    newGenreLabel: String,
    onLabelChange: (String) -> Unit,
    newGenreEmoji: String,
    onEmojiChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ジャンル追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newGenreLabel,
                    onValueChange = onLabelChange,
                    label = { Text("ジャンル名") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("📖", "📐", "🧪", "🌍", "🎵", "⚽", "🎮", "✏️").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (newGenreEmoji == emoji) HomeTheme.AccentBlue.copy(alpha = 0.2f) else Color.Transparent
                                )
                                .clickable { onEmojiChange(emoji) }
                                .padding(6.dp)
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
