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
    var selectedTab by remember { mutableStateOf(0) }
    
    // キャラクターのフワフワ浮遊アニメーション
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

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            StudyStartButton(onClick = { /* タイマー機能へ */ })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderView()
            Spacer(modifier = Modifier.weight(1f))
            CharacterView(offsetY = offsetY)
            Spacer(modifier = Modifier.weight(1f))
        }
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
                .background(Color(0xFFF1F5F9))
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

        // 知識の結晶（ガチャ石）
        Row(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Crystals", tint = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Crystals", fontSize = 10.sp, color = Color.Gray)
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
        // 背景のグラデーション円
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
            // 吹き出し
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "今日の特訓も頑張ろうな！",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // キャラクター要素（絵文字）
            Text(
                text = "🧙‍♂️",
                fontSize = 120.sp,
                modifier = Modifier.offset(y = offsetY.dp)
            )
        }
    }
}

@Composable
fun StudyStartButton(onClick: () -> Unit) {
    Box(modifier = Modifier.offset(y = 35.dp)) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(32.dp), tint = Color.White)
                    Text("START", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
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
                .padding(bottom = 20.dp, top = 8.dp) // セーフエリア余白の代わり
                .height(60.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Default.Home, "Home", selectedTab == 0) { onTabSelected(0) }
            NavItem(Icons.Default.Person, "Party", selectedTab == 1) { onTabSelected(1) }
            
            Spacer(modifier = Modifier.width(60.dp)) // STARTボタン用のスペース
            
            NavItem(Icons.Default.ShoppingCart, "Gacha", selectedTab == 3) { onTabSelected(3) }
            NavItem(Icons.Default.Info, "Data", selectedTab == 4) { onTabSelected(4) }
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
