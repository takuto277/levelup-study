package org.example.project.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = HomeTheme.BarBg.copy(alpha = 0.96f),
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 16.dp,
            border = BorderStroke(0.5.dp, HomeTheme.BarStroke),
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
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isCenter: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isSelected) HomeTheme.NavCyan else HomeTheme.NavDim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .width(60.dp)
    ) {
        Box(
            modifier = Modifier.height(40.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isCenter) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(HomeTheme.AccentBlue, HomeTheme.AccentIndigo)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            } else {
                if (isSelected) {
                    Surface(
                        color = HomeTheme.AccentBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 44.dp, height = 30.dp)
                    ) {}
                }
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            label,
            fontSize = 10.sp,
            color = if (isCenter) Color.White else color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = if (isCenter) 4.dp else 0.dp)
        )
    }
}
