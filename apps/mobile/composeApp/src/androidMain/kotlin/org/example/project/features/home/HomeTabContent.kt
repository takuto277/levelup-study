package org.example.project.features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import org.example.project.domain.model.MasterStudyGenre
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
        GenreManageDialog(
            genres = homeState.genres,
            apiError = homeState.error,
            newGenreLabel = newGenreLabel,
            onLabelChange = { newGenreLabel = it },
            onDismiss = {
                homeViewModel.clearError()
                showAddGenreDialog = false
            },
            onAdd = {
                if (newGenreLabel.isNotEmpty()) {
                    homeViewModel.onIntent(HomeIntent.AddGenre(newGenreLabel, "", "#6B7280"))
                    newGenreLabel = ""
                    showAddGenreDialog = false
                }
            },
            onDeleteGenre = { id -> homeViewModel.onIntent(HomeIntent.DeleteGenre(id)) }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHeader(homeState = homeState)

        HomeAdventureContextRow(
            dungeonName = homeState.selectedDungeonName,
            isTrainingStudySession = homeState.isTrainingStudySession,
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

        StartAdventureButton(
            isTrainingStudySession = homeState.isTrainingStudySession,
            onClick = onStartStudy
        )

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
        Text("冒険時間", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HomeTheme.TextSecondary)
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
private fun StartAdventureButton(isTrainingStudySession: Boolean, onClick: () -> Unit) {
    val label = "勉強を始める"
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
                    brush = Brush.linearGradient(
                        if (isTrainingStudySession) {
                            listOf(HomeTheme.AccentIndigo, HomeTheme.AccentBlue)
                        } else {
                            listOf(HomeTheme.FireRed, HomeTheme.FireOrange)
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GenreManageDialog(
    genres: List<MasterStudyGenre>,
    apiError: String?,
    newGenreLabel: String,
    onLabelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onDeleteGenre: (String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<MasterStudyGenre?>(null) }
    val sortedGenres = remember(genres) { genres.sortedBy { it.sortOrder } }

    pendingDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = HomeTheme.BgDark2,
            titleContentColor = HomeTheme.TextPrimary,
            textContentColor = HomeTheme.TextSecondary,
            title = { Text("削除の確認", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${g.label}\" を削除しますか？\n記録の勉強時間は「削除済み課題」として残ります。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGenre(g.id)
                        pendingDelete = null
                    }
                ) { Text("削除", color = HomeTheme.FireRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("キャンセル", color = HomeTheme.TextSecondary)
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = HomeTheme.BgDark2,
            shadowElevation = 10.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("ジャンル管理", fontSize = 20.sp, fontWeight = FontWeight.Black, color = HomeTheme.TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!apiError.isNullOrBlank()) {
                        Text(
                            apiError,
                            fontSize = 12.sp,
                            color = HomeTheme.FireRed,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text("新しいジャンルを追加", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HomeTheme.AccentCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    val fieldBg = Color(0xFF1E293B)
                    OutlinedTextField(
                        value = newGenreLabel,
                        onValueChange = onLabelChange,
                        label = { Text("ジャンル名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HomeTheme.AccentCyan,
                            unfocusedBorderColor = HomeTheme.BarStroke,
                            focusedLabelColor = HomeTheme.AccentCyan,
                            unfocusedLabelColor = HomeTheme.TextSecondary,
                            cursorColor = HomeTheme.AccentCyan,
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAdd,
                        enabled = newGenreLabel.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeTheme.AccentBlue,
                            disabledContainerColor = HomeTheme.TextSecondary.copy(alpha = 0.25f),
                            contentColor = Color.White,
                            disabledContentColor = HomeTheme.TextSecondary
                        )
                    ) {
                        Text("ジャンルを追加", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = HomeTheme.BarStroke)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ジャンル一覧", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HomeTheme.AccentCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (sortedGenres.size > 1) {
                            "※左にスワイプして削除できます。"
                        } else {
                            "※ジャンルは最低1件必要です。"
                        },
                        fontSize = 11.sp,
                        color = HomeTheme.TextSecondary,
                        lineHeight = 15.sp
                    )
                }
                items(sortedGenres, key = { it.id }) { g ->
                    GenreManageSwipeRow(
                        genre = g,
                        allowSwipeDelete = sortedGenres.size > 1,
                        onSwipeDeleteRequest = { pendingDelete = it }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("閉じる", color = HomeTheme.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreManageSwipeRow(
    genre: MasterStudyGenre,
    allowSwipeDelete: Boolean,
    onSwipeDeleteRequest: (MasterStudyGenre) -> Unit
) {
    val swipeable = allowSwipeDelete
    var offsetX by remember(genre.id, allowSwipeDelete) { mutableFloatStateOf(0f) }
    val maxLeft = -168f
    val triggerAt = -96f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (swipeable) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(HomeTheme.FireRed.copy(alpha = 0.88f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "削除",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .then(
                    if (swipeable) {
                        Modifier.pointerInput(genre.id, allowSwipeDelete) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount).coerceIn(maxLeft, 0f)
                                },
                                onDragEnd = {
                                    if (offsetX <= triggerAt) {
                                        onSwipeDeleteRequest(genre)
                                    }
                                    offsetX = 0f
                                },
                                onDragCancel = { offsetX = 0f }
                            )
                        }
                    } else Modifier
                )
                .fillMaxWidth()
                .background(HomeTheme.CardWhite)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                genre.label,
                modifier = Modifier.weight(1f),
                color = HomeTheme.TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
