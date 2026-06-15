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
    val charRepo = remember { org.example.project.di.getCharacterRepository() }
    val weaponRepo = remember { org.example.project.di.getWeaponRepository() }
    var characters by remember { mutableStateOf(emptyList<org.example.project.domain.model.UserCharacter>()) }
    var weapons by remember { mutableStateOf(emptyList<org.example.project.domain.model.UserWeapon>()) }

    LaunchedEffect(Unit) {
        characters = charRepo.getUserCharacters()
        weapons = weaponRepo.getUserWeapons()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120)).padding(16.dp)) {
        Text("コレクション図鑑", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE))
        Spacer(modifier = Modifier.height(16.dp))

        Text("所持キャラクター (${characters.size})", color = Color(0xFF94A3B8), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (characters.isEmpty()) {
            Text("まだキャラクターがいません。ガチャを引こう！", color = Color(0xFF64748B))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(characters.size) { i ->
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(characters[i].character?.emoji ?: "?", fontSize = 24.sp)
                            Text(characters[i].character?.name ?: "不明", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            Text("Lv.${characters[i].level}", fontSize = 10.sp, color = Color(0xFF64748B))
                            if (characters[i].breakthroughLevel > 0) {
                                Text("凸${characters[i].breakthroughLevel}", fontSize = 10.sp, color = Color(0xFFF59E0B))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("所持武器 (${weapons.size})", color = Color(0xFF94A3B8), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        if (weapons.isEmpty()) {
            Text("まだ武器がありません。ガチャを引こう！", color = Color(0xFF64748B))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weapons.size) { i ->
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚔️", fontSize = 24.sp)
                            Text(weapons[i].weapon?.name ?: "不明", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            Text("精錬${weapons[i].refinementLevel}", fontSize = 10.sp, color = Color(0xFF22D3EE))
                        }
                    }
                }
            }
        }
    }
}
