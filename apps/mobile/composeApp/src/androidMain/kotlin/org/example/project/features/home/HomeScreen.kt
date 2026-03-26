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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    // 2: Home (Main Tab)
    var selectedTab by remember { mutableStateOf(2) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        containerColor = Color(0xFFF8FAFC) // systemGroupedBackground equivalent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                0 -> PlaceholderScreen("冒険 (Quest)", Icons.Default.Place)
                1 -> PlaceholderScreen("編成 (Party)", Icons.Default.Person)
                2 -> HomeTabContent()
                3 -> PlaceholderScreen("召喚 (Gacha)", Icons.Default.Star)
                4 -> PlaceholderScreen("記録 (Analytics)", Icons.Default.Info)
            }
        }
    }
}

// MARK: - Home Tab
@Composable
fun HomeTabContent() {
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
        
        // 勉強開始ボタン (Home画面直下へ移動)
        StudyStartButton(onClick = {
            // TODO: KMP Viewmodelのイベントを発火して、タイマー画面に遷移する
            // viewModel.onStartStudyClicked() みたいになる
        })
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
            Row(verticalAlignment = Alignment.CenterVertically, spacing = 8.dp) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                Text("特訓スタート", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 20.dp, top = 8.dp)
                .height(60.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Default.Place, "冒険", selectedTab == 0) { onTabSelected(0) }
            NavItem(Icons.Default.Person, "編成", selectedTab == 1) { onTabSelected(1) }
            NavItem(Icons.Default.Home, "ホーム", selectedTab == 2) { onTabSelected(2) }
            NavItem(Icons.Default.Star, "召喚", selectedTab == 3) { onTabSelected(3) }
            NavItem(Icons.Default.Info, "記録", selectedTab == 4) { onTabSelected(4) }
        }
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF3B82F6) else Color.Gray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(50.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
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
