package org.example.project.features.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionScreenView() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120)).padding(16.dp)) {
        Text("コレクション図鑑", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE))
        Spacer(modifier = Modifier.height(16.dp))
        Text("ガチャで入手したキャラや武器が表示されます", color = Color(0xFF64748B), fontSize = 14.sp)
    }
}
