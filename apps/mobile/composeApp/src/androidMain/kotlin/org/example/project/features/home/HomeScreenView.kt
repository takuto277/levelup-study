package org.example.project.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.example.project.core.storage.KeyValueStore
import org.example.project.features.study.StudyQuestScreenView

@Composable
fun HomeScreenView() {
    var selectedTab by remember { mutableStateOf(2) }
    var showStudySheet by remember { mutableStateOf(false) }
    val kvStore = remember { KeyValueStore() }
    var studyMinutes by remember {
        mutableStateOf(
            kvStore.getString(HOME_STUDY_MINUTES_KEY)
                ?.toIntOrNull()
                ?.let { snapStudyMinutesToValid(it.coerceIn(1, 60)) }
                ?: 25
        )
    }
    var selectedGenreSlug by remember { mutableStateOf("general") }

    val homeViewModel = remember { org.example.project.di.getHomeViewModel() }
    val homeState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(studyMinutes) {
        kvStore.putString(HOME_STUDY_MINUTES_KEY, studyMinutes.toString())
    }

    if (showStudySheet) {
        StudyQuestScreenView(
            initialStudyMinutes = studyMinutes.coerceIn(1, 60),
            genreId = selectedGenreSlug,
            dungeonName = homeState.selectedDungeonName,
            dungeonImageUrl = if (homeState.isTrainingStudySession) null else homeState.selectedDungeonImageUrl,
            isTrainingGround = homeState.isTrainingStudySession,
            onDismiss = {
                showStudySheet = false
                homeViewModel.onIntent(HomeIntent.Refresh)
            }
        )
    } else {
        Scaffold(
            containerColor = HomeTheme.BgColor,
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> selectedTab = tab }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Brush.verticalGradient(listOf(HomeTheme.BgColor, HomeTheme.BgDark2)))
            ) {
                when (selectedTab) {
                    0 -> org.example.project.features.quest.QuestScreenView()
                    1 -> org.example.project.features.party.PartyScreenView()
                    2 -> HomeTabContent(
                        studyMinutes = studyMinutes,
                        onStudyMinutesChange = { studyMinutes = it.coerceIn(1, 60) },
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
        }
    }
}
