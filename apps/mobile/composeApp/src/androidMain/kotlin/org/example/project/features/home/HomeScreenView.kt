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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreenView() {
    // 2: Home (Main Tab)
    var selectedTab by remember { mutableStateOf(2) }
    var showStudySheet by remember { mutableStateOf(false) }
    var studyMinutes by remember { mutableStateOf(25) }

    if (showStudySheet) {
        org.example.project.features.study.StudyQuestScreenView(
            initialStudyMinutes = studyMinutes,
            onDismiss = { showStudySheet = false }
        )
    } else {
        Scaffold(
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content
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
                            onStartStudy = { showStudySheet = true }
                        )
                        3 -> org.example.project.features.gacha.GachaScreenView()
                        4 -> org.example.project.features.record.RecordScreenView()
                    }
                }

                // Floating Tab Bar
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }
}

// MARK: - Home Tab
@Composable
fun HomeTabContent(
    studyMinutes: Int,
    onStudyMinutesChange: (Int) -> Unit,
    onStartStudy: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_anim"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderView()
        Spacer(modifier = Modifier.weight(1f))
        CharacterView(offsetY = offsetY)
        Spacer(modifier = Modifier.weight(1f))
        
        // 勉強時間の調整
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("今回の冒険時間", fontSize = 12.sp, color = Color.Gray)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = { if (studyMinutes > 1) onStudyMinutesChange(studyMinutes - 1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Decrease", tint = Color.Blue)
                }
                Text(
                    text = "$studyMinutes 分",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { if (studyMinutes < 120) onStudyMinutesChange(studyMinutes + 1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Increase", tint = Color.Blue)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 勉強開始ボタン
        StudyStartButton(onClick = onStartStudy)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 累計勉強時間
        Row(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Time", tint = Color.Blue)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Total Study", fontSize = 10.sp, color = Color.Gray)
                Text("124h 30m", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        // 知識の結晶
        Row(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Crystals", tint = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("知識の結晶", fontSize = 10.sp, color = Color.Gray)
                Text("1,250", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Color(0xFF10B981))
        }
    }
}

@Composable
fun CharacterView(offsetY: Float) {
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Blue.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("今日の特訓も頑張ろうな！", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("🧙‍♂️", fontSize = 120.sp, modifier = Modifier.offset(y = offsetY.dp))
        }
    }
}

@Composable
fun StudyStartButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(vertical = 16.dp, horizontal = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                Text("特訓スタート", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.95f),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp,
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

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, isCenter: Boolean = false, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF3B82F6) else Color.Gray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isCenter) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .offset(y = (-16).dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            } else {
                if (isSelected) {
                    Surface(
                        color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 44.dp, height = 32.dp)
                    ) {}
                }
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label, 
            fontSize = 10.sp, 
            color = if (isCenter) Color.DarkGray else color, 
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
