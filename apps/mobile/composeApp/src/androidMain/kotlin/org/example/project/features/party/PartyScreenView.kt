package org.example.project.features.party

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.components.PlayerSprite
import org.example.project.components.PlayerSpriteMode
import org.example.project.components.hasPartyPlayerSprite
import org.example.project.data.local.TutorialTopics
import org.example.project.components.TutorialHintRow
import org.example.project.domain.model.UserCharacter

// ── カラー（青テーマ）─────────────────────────────────
private val BgColor = Color(0xFF0B1120)
private val CardWhite = Color(0xFF111B2E)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentCyan = Color(0xFF22D3EE)
private val HpRed = Color(0xFFEF4444)
private val AtkOrange = Color(0xFFF59E0B)
private val DefBlue = Color(0xFF3B82F6)
private val HealGreen = Color(0xFF10B981)
private val BgSurface = Color(0xFF1A2744)

private fun rarityColor(rarity: Int): Color = when (rarity) {
    5 -> Color(0xFFFFD700)
    4 -> Color(0xFF8B5CF6)
    3 -> Color(0xFF3B82F6)
    else -> Color(0xFF94A3B8)
}

/** とりあえず編成画面のキャラ絵はユーザー（冒険者）スプライトに統一 */
@Composable
private fun PartyPlayerAvatar(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasSprite = remember { hasPartyPlayerSprite(context) }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (hasSprite) {
            PlayerSprite(
                mode = PlayerSpriteMode.Idle,
                size = size,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                "🧙‍♂️",
                fontSize = (size.value * 0.52f).coerceIn(22f, 80f).sp
            )
        }
    }
}

private fun rarityGradient(rarity: Int): List<Color> = when (rarity) {
    5 -> listOf(Color(0xFFFFD700), Color(0xFFFBBF24))
    4 -> listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
    3 -> listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
    else -> listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1))
}

private fun weaponDisplayName(weapon: org.example.project.domain.model.UserWeapon): String =
    weapon.weapon?.name?.takeIf { it.isNotBlank() } ?: "武器（Lv.${weapon.level}）"

private fun weaponEmoji(weapon: org.example.project.domain.model.UserWeapon): String {
    val name = weapon.weapon?.name.orEmpty()
    return when {
        name.contains("杖") || name.contains("スタッフ") -> "🪄"
        name.contains("剣") || name.contains("刀") -> "⚔️"
        name.contains("弓") -> "🏹"
        name.contains("聖") || name.contains("ワンド") -> "✨"
        else -> "🗡️"
    }
}

// ── メイン画面 ──────────────────────────────────────
@Composable
fun PartyScreenView() {
    val viewModel = remember { org.example.project.di.getPartyViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var showCharacterPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgColor, Color(0xFF0F172A))))) {
        Column(modifier = Modifier.fillMaxSize()) {
            TutorialHintRow(TutorialTopics.PARTY_SETUP, "\uD83E\uDDE0", "キャラや武器を編成して冒険の戦力をアップ！")

            // ヘッダー + 変更ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(colors = listOf(AccentIndigo.copy(alpha = 0.08f), Color.Transparent))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("編成", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                    Text("メインキャラクターを選択しよう", fontSize = 13.sp, color = TextSecondary)
                }
                Button(
                    onClick = { showCharacterPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("🔄 変更", fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                uiState.party?.mainCharacter?.let { mainChar ->
                    MainCharacterSection(mainChar, uiState.ownedWeapons)
                }

                Spacer(modifier = Modifier.height(20.dp))

                uiState.party?.let { party ->
                    PartySlotSection(
                        party = party,
                        selectedSlot = uiState.selectedSlot,
                        onSlotClick = { slot -> viewModel.onIntent(PartyIntent.SelectSlot(slot)) },
                        onRemoveSlot = { slot -> viewModel.onIntent(PartyIntent.RemoveFromSlot(slot)) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CharacterListSection(
                    characters = uiState.ownedCharacters,
                    selectedSlot = uiState.selectedSlot,
                    onCharacterClick = { charId ->
                        val slot = uiState.selectedSlot
                        if (slot != null) {
                            viewModel.onIntent(PartyIntent.AssignCharacter(slot, charId))
                            viewModel.onIntent(PartyIntent.SelectSlot(slot))
                        } else {
                            viewModel.onIntent(PartyIntent.SelectCharacter(charId))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        uiState.selectedCharacter?.let { character ->
            CharacterDetailScreen(
                character = character,
                weapon = uiState.ownedWeapons.find { it.id == character.equippedWeaponId },
                weapons = uiState.ownedWeapons,
                onDismiss = { viewModel.onIntent(PartyIntent.DismissCharacterDetail) },
                onEquipWeapon = { weaponId ->
                    viewModel.onIntent(PartyIntent.EquipWeapon(character.id, weaponId))
                },
                onLevelUpCharacter = {
                    viewModel.onIntent(PartyIntent.LevelUpCharacter(character.id))
                },
                onLevelUpWeapon = { weaponId ->
                    viewModel.onIntent(PartyIntent.LevelUpWeapon(weaponId))
                }
            )
        }

        uiState.selectedSlot?.let { slot ->
            SlotSelectionBanner(
                slotPosition = slot,
                onCancel = { viewModel.onIntent(PartyIntent.SelectSlot(slot)) }
            )
        }

        if (showCharacterPicker) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .clickable { showCharacterPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .padding(20.dp)
                        .clickable(enabled = false) {}
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("メインキャラクターを選択", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("閉じる", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentBlue,
                            modifier = Modifier.clickable { showCharacterPicker = false })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(uiState.ownedCharacters, key = { it.id }) { char ->
                            val master = char.character ?: return@items
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BgColor)
                                    .clickable {
                                        viewModel.onIntent(PartyIntent.AssignCharacter(1, char.id))
                                        showCharacterPicker = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                PartyPlayerAvatar(size = 44.dp)
                                Text(master.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Lv.${char.level}", fontSize = 11.sp, color = rarityColor(master.rarity))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── ヘッダー ─────────────────────────────────────────
@Composable
private fun PartyHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AccentIndigo.copy(alpha = 0.08f), Color.Transparent)
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("🛡️", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("編成", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("キャラクターを配置してパーティを強化しよう", fontSize = 14.sp, color = TextSecondary)
        }
    }
}

// ── メインキャラクター ──────────────────────────────
@Composable
private fun MainCharacterSection(
    mainChar: UserCharacter,
    weapons: List<org.example.project.domain.model.UserWeapon>
) {
    val master = mainChar.character ?: return
    val weapon = weapons.find { it.id == mainChar.equippedWeaponId }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            // ヒーロー部分
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        brush = Brush.linearGradient(
                            rarityGradient(master.rarity).map { it.copy(alpha = 0.15f) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // キャラ絵
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        rarityColor(master.rarity).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PartyPlayerAvatar(size = 96.dp)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // レアリティ
                        Row {
                            repeat(master.rarity) {
                                Text("⭐", fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(master.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Lv.${mainChar.level}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = rarityColor(master.rarity)
                        )
                        if (weapon != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(weaponEmoji(weapon), fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    weaponDisplayName(weapon),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // メインキャラバッジ
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AccentBlue
                ) {
                    Text(
                        "MAIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // ステータスバー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("❤️", "HP", "${mainChar.combatHp}", HpRed)
                StatItem("⚔️", "ATK", "${mainChar.combatAtk}", AtkOrange)
                StatItem("🛡️", "DEF", "${mainChar.combatDef}", DefBlue)
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── パーティスロット ─────────────────────────────────
@Composable
private fun PartySlotSection(
    party: org.example.project.domain.model.Party?,
    selectedSlot: Int?,
    onSlotClick: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("パーティ編成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            (1..4).forEach { slot ->
                val partySlot = party?.slots?.find { it.slotPosition == slot }
                val isSelected = selectedSlot == slot
                PartySlotCard(
                    slotPosition = slot,
                    userCharacter = partySlot?.userCharacter,
                    isSelected = isSelected,
                    onClick = { onSlotClick(slot) },
                    onRemove = if (partySlot != null) {
                        { onRemoveSlot(slot) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PartySlotCard(
    slotPosition: Int,
    userCharacter: UserCharacter?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AccentBlue else Color.Transparent
    val bgColor = if (userCharacter != null) CardWhite else Color(0xFFF1F5F9)

    Surface(
        modifier = modifier
            .aspectRatio(0.75f)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onRemove?.invoke() }
            ),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (userCharacter != null) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (userCharacter != null) {
                val master = userCharacter.character
                // レアリティドット
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(master?.rarity ?: 0) {
                        Text("⭐", fontSize = 8.sp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                PartyPlayerAvatar(size = 36.dp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    master?.name ?: "",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Lv.${userCharacter.level}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = rarityColor(master?.rarity ?: 1)
                )
            } else {
                // 空スロット
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(2.dp, Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "追加",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("空きスロット", fontSize = 9.sp, color = TextSecondary)
            }

            // スロット番号
            Text(
                "Slot $slotPosition",
                fontSize = 8.sp,
                color = if (isSelected) AccentBlue else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ── スロット選択バナー ──────────────────────────────
@Composable
private fun SlotSelectionBanner(slotPosition: Int, onCancel: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = AccentBlue,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "スロット${slotPosition}に配置するキャラクターを選んでください",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) {
                    Text("キャンセル", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ── キャラクター一覧 ─────────────────────────────────
@Composable
private fun CharacterListSection(
    characters: List<UserCharacter>,
    selectedSlot: Int?,
    onCharacterClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            if (selectedSlot != null) "配置するキャラクターを選択" else "所持キャラクター",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedSlot != null) AccentBlue else TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // グリッドは LazyVerticalGrid だと ScrollView 内で高さ制約が必要
        // 固定高さで表示
        val rows = characters.chunked(3)
        rows.forEach { rowChars ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowChars.forEach { char ->
                    CharacterGridCard(
                        character = char,
                        isSelecting = selectedSlot != null,
                        onClick = { onCharacterClick(char.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 残りの空白を埋める
                repeat(3 - rowChars.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CharacterGridCard(
    character: UserCharacter,
    isSelecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val master = character.character ?: return

    Surface(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = CardWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 上部: グラデーション + アイコン
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(
                            rarityGradient(master.rarity).map { it.copy(alpha = 0.15f) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PartyPlayerAvatar(size = 56.dp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        repeat(master.rarity) {
                            Text("⭐", fontSize = 9.sp)
                        }
                    }
                }

                // 選択モードマーク
                if (isSelecting) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(AccentBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "配置",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 下部: 情報
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    master.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Lv.${character.level}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = rarityColor(master.rarity)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // ミニステータス
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("❤️", "${character.combatHp}")
                    MiniStat("⚔️", "${character.combatAtk}")
                    MiniStat("🛡️", "${character.combatDef}")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 9.sp)
        Text(value, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
    }
}

private fun levelUpGoldCost(level: Int): Int = level.coerceAtLeast(1) * 50

// ── キャラクター詳細シート ──────────────────────────
@Composable
private fun CharacterDetailScreen(
    character: UserCharacter,
    weapon: org.example.project.domain.model.UserWeapon?,
    weapons: List<org.example.project.domain.model.UserWeapon>,
    onDismiss: () -> Unit,
    onEquipWeapon: (String?) -> Unit,
    onLevelUpCharacter: () -> Unit,
    onLevelUpWeapon: (String) -> Unit,
) {
    val master = character.character ?: return
    var pickingWeapon by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgColor,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (pickingWeapon) pickingWeapon = false else onDismiss()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = TextPrimary)
                    }
                    Text(
                        if (pickingWeapon) "武器を選択" else "キャラクター詳細",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }

                if (pickingWeapon) {
                    WeaponPickerContent(
                        weapons = weapons,
                        equippedWeaponId = character.equippedWeaponId,
                        onSelect = { weaponId ->
                            onEquipWeapon(weaponId)
                            pickingWeapon = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding(),
                    ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(160.dp)
                        .background(
                            brush = Brush.linearGradient(
                                rarityGradient(master.rarity).map { it.copy(alpha = 0.12f) }
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PartyPlayerAvatar(size = 112.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            repeat(master.rarity) { Text("⭐", fontSize = 16.sp) }
                        }
                    }
                }

                // 名前・レベル
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(master.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rarityColor(master.rarity).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Lv.${character.level}  ·  XP ${character.currentXp}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = rarityColor(master.rarity),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ステータス詳細
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = BgSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ステータス", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        val hp = character.combatHp
                        val atk = character.combatAtk
                        val def = character.combatDef
                        val totalPower = hp + atk * 2 + def

                        StatBar("❤️ HP", hp, 2000, HpRed)
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("⚔️ ATK", atk, 600, AtkOrange)
                        Spacer(modifier = Modifier.height(10.dp))
                        StatBar("🛡️ DEF", def, 500, DefBlue)

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💪 総合戦闘力", fontSize = 14.sp, color = TextSecondary)
                            Text(
                                "$totalPower",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 装備
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = BgSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("装備中の武器", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (weapons.isNotEmpty()) {
                                TextButton(onClick = { pickingWeapon = true }) {
                                    Text("変更", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (weapon != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            rarityColor(weapon.weapon?.rarity ?: 1).copy(alpha = 0.18f),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(weaponEmoji(weapon), fontSize = 26.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        weaponDisplayName(weapon),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 2,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(weapon.weapon?.rarity ?: 0) {
                                            Text("★", fontSize = 11.sp, color = Color(0xFFFFD700))
                                        }
                                        if ((weapon.weapon?.rarity ?: 0) > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            "Lv.${weapon.level}  ·  ATK +${weapon.weapon?.baseAtk ?: 0}",
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(2.dp, Color(0xFFCBD5E1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("—", fontSize = 20.sp, color = Color(0xFFCBD5E1))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("武器未装備", fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLevelUpCharacter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text(
                        "LvUP（${levelUpGoldCost(character.level)} G）",
                        fontWeight = FontWeight.Bold
                    )
                }

                weapon?.let { equipped ->
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onLevelUpWeapon(equipped.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Text(
                            "武器 LvUP（${levelUpGoldCost(equipped.level)} G）",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponPickerContent(
    weapons: List<org.example.project.domain.model.UserWeapon>,
    equippedWeaponId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("装備を外す", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(weapons, key = { it.id }) { w ->
                val selected = w.id == equippedWeaponId
                Surface(
                    onClick = { onSelect(w.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) AccentBlue.copy(alpha = 0.18f) else BgSurface,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) AccentBlue else Color(0xFF334155),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(weaponEmoji(w), fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                weaponDisplayName(w),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 2,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(w.weapon?.rarity ?: 0) {
                                    Text("★", fontSize = 10.sp, color = Color(0xFFFFD700))
                                }
                                if ((w.weapon?.rarity ?: 0) > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    "Lv.${w.level}  ·  ATK +${w.weapon?.baseAtk ?: 0}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "装備中",
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, maxValue: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = TextSecondary)
            Text("$value", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFF334155)
        )
    }
}

