package org.example.project.features.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.di.getCollectionViewModel

@Composable
fun CollectionScreenView() {
    val viewModel = remember { getCollectionViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120)).padding(16.dp)) {
        Text("コレクション図鑑", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE))
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Text("読み込み中...", color = Color(0xFF64748B), fontSize = 14.sp)
        } else {
            Text("所持キャラクター (${uiState.characters.size})", color = Color(0xFF94A3B8), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.characters.isEmpty()) {
                Text("まだキャラクターがいません。ガチャを引こう！", color = Color(0xFF64748B))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.characters, key = { it.id }) { uc ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val stars = with(uc.character) { "★".repeat(this?.rarity ?: 0) }.ifEmpty { "?" }
                                Text(stars, fontSize = 16.sp, color = Color(0xFFFBBF24))
                                Text(
                                    uc.character?.name ?: "不明",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0),
                                    textAlign = TextAlign.Center
                                )
                                Text("Lv.${uc.level}", fontSize = 10.sp, color = Color(0xFF64748B))
                                if (uc.breakthroughLevel > 0) {
                                    Text(
                                        "凸${uc.breakthroughLevel}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("所持武器 (${uiState.weapons.size})", color = Color(0xFF94A3B8), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.weapons.isEmpty()) {
                Text("まだ武器がありません。ガチャを引こう！", color = Color(0xFF64748B))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.weapons, key = { it.id }) { uw ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("\u2694\uFE0F", fontSize = 24.sp)
                                Text(
                                    uw.weapon?.name ?: "不明",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "精錬${uw.refinementLevel}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF22D3EE)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
