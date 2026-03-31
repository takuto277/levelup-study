package org.example.project.features.party

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.domain.model.MasterCharacter
import org.example.project.domain.model.MasterWeapon
import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.model.UserWeapon

/**
 * 編成（Party）画面の ViewModel
 *
 * - MutableStateFlow 内部保持、外部には StateFlow のみ公開
 * - UI からの入力は onIntent() に集約
 * - 現在はモックデータで動作。将来的に PartyUseCase / Repository を DI で注入
 */
class PartyViewModel {

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        loadPartyData()
    }

    fun onIntent(intent: PartyIntent) {
        when (intent) {
            is PartyIntent.Refresh -> loadPartyData()
            is PartyIntent.SelectSlot -> _uiState.update { it.copy(selectedSlot = intent.slotPosition) }
            is PartyIntent.AssignCharacter -> assignCharacter(intent.slotPosition, intent.userCharacterId)
            is PartyIntent.RemoveFromSlot -> removeFromSlot(intent.slotPosition)
            is PartyIntent.SelectCharacter -> selectCharacter(intent.userCharacterId)
            is PartyIntent.DismissCharacterDetail -> _uiState.update { it.copy(selectedCharacter = null) }
            is PartyIntent.EquipWeapon -> { /* TODO: 将来実装 */ }
            is PartyIntent.LevelUpCharacter -> { /* TODO: 将来実装 */ }
            is PartyIntent.LevelUpWeapon -> { /* TODO: 将来実装 */ }
        }
    }

    // ── ロード ──────────────────────────────────────

    private fun loadPartyData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        // TODO: 将来的に PartyUseCase 経由でサーバーから取得
        val characters = getDefaultCharacters()
        val weapons = getDefaultWeapons()
        val party = getDefaultParty(characters)

        _uiState.update {
            it.copy(
                party = party,
                ownedCharacters = characters,
                ownedWeapons = weapons,
                isLoading = false
            )
        }
    }

    // ── 操作 ──────────────────────────────────────

    private fun assignCharacter(slotPosition: Int, userCharacterId: String) {
        val currentParty = _uiState.value.party ?: return
        val character = _uiState.value.ownedCharacters.find { it.id == userCharacterId } ?: return

        val updatedSlots = currentParty.slots.toMutableList()
        val existingIndex = updatedSlots.indexOfFirst { it.slotPosition == slotPosition }

        val newSlot = PartySlot(
            id = "slot_$slotPosition",
            userId = "user_001",
            slotPosition = slotPosition,
            userCharacterId = userCharacterId,
            userCharacter = character
        )

        if (existingIndex >= 0) {
            updatedSlots[existingIndex] = newSlot
        } else {
            updatedSlots.add(newSlot)
        }

        _uiState.update {
            it.copy(
                party = Party(slots = updatedSlots),
                selectedSlot = null
            )
        }
    }

    private fun removeFromSlot(slotPosition: Int) {
        val currentParty = _uiState.value.party ?: return
        val updatedSlots = currentParty.slots.filter { it.slotPosition != slotPosition }
        _uiState.update {
            it.copy(party = Party(slots = updatedSlots))
        }
    }

    private fun selectCharacter(userCharacterId: String) {
        val character = _uiState.value.ownedCharacters.find { it.id == userCharacterId }
        _uiState.update { it.copy(selectedCharacter = character) }
    }

    // ── モックデータ ────────────────────────────────

    companion object {
        fun getDefaultCharacters(): List<UserCharacter> = listOf(
            UserCharacter(
                id = "uc_001",
                userId = "user_001",
                characterId = "char_wizard",
                character = MasterCharacter(
                    id = "char_wizard",
                    name = "賢者アルマ",
                    rarity = 4,
                    baseHp = 850,
                    baseAtk = 320,
                    baseDef = 180,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 12,
                currentXp = 2400,
                equippedWeaponId = "uw_001",
                obtainedAt = "2026-01-15"
            ),
            UserCharacter(
                id = "uc_002",
                userId = "user_001",
                characterId = "char_knight",
                character = MasterCharacter(
                    id = "char_knight",
                    name = "騎士レオン",
                    rarity = 3,
                    baseHp = 1200,
                    baseAtk = 250,
                    baseDef = 350,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 8,
                currentXp = 1200,
                equippedWeaponId = "uw_002",
                obtainedAt = "2026-02-01"
            ),
            UserCharacter(
                id = "uc_003",
                userId = "user_001",
                characterId = "char_archer",
                character = MasterCharacter(
                    id = "char_archer",
                    name = "弓使いリナ",
                    rarity = 3,
                    baseHp = 680,
                    baseAtk = 380,
                    baseDef = 150,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 10,
                currentXp = 1800,
                equippedWeaponId = null,
                obtainedAt = "2026-02-10"
            ),
            UserCharacter(
                id = "uc_004",
                userId = "user_001",
                characterId = "char_healer",
                character = MasterCharacter(
                    id = "char_healer",
                    name = "癒し手ミラ",
                    rarity = 5,
                    baseHp = 720,
                    baseAtk = 180,
                    baseDef = 280,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 15,
                currentXp = 4200,
                equippedWeaponId = "uw_003",
                obtainedAt = "2026-01-20"
            ),
            UserCharacter(
                id = "uc_005",
                userId = "user_001",
                characterId = "char_ninja",
                character = MasterCharacter(
                    id = "char_ninja",
                    name = "忍者カゲ",
                    rarity = 4,
                    baseHp = 600,
                    baseAtk = 420,
                    baseDef = 120,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 5,
                currentXp = 600,
                equippedWeaponId = null,
                obtainedAt = "2026-03-01"
            ),
            UserCharacter(
                id = "uc_006",
                userId = "user_001",
                characterId = "char_dragon",
                character = MasterCharacter(
                    id = "char_dragon",
                    name = "竜騎士ドラク",
                    rarity = 5,
                    baseHp = 1500,
                    baseAtk = 400,
                    baseDef = 300,
                    imageUrl = "",
                    idleAnimationUrl = null,
                    isActive = true
                ),
                level = 3,
                currentXp = 200,
                equippedWeaponId = null,
                obtainedAt = "2026-03-20"
            )
        )

        fun getDefaultWeapons(): List<UserWeapon> = listOf(
            UserWeapon(
                id = "uw_001",
                userId = "user_001",
                weaponId = "wpn_staff",
                weapon = MasterWeapon(
                    id = "wpn_staff",
                    name = "叡智の杖",
                    rarity = 4,
                    baseAtk = 85,
                    imageUrl = "",
                    isActive = true
                ),
                level = 5,
                obtainedAt = "2026-01-15"
            ),
            UserWeapon(
                id = "uw_002",
                userId = "user_001",
                weaponId = "wpn_sword",
                weapon = MasterWeapon(
                    id = "wpn_sword",
                    name = "勇者の剣",
                    rarity = 3,
                    baseAtk = 65,
                    imageUrl = "",
                    isActive = true
                ),
                level = 3,
                obtainedAt = "2026-02-01"
            ),
            UserWeapon(
                id = "uw_003",
                userId = "user_001",
                weaponId = "wpn_wand",
                weapon = MasterWeapon(
                    id = "wpn_wand",
                    name = "癒しの聖杖",
                    rarity = 5,
                    baseAtk = 45,
                    imageUrl = "",
                    isActive = true
                ),
                level = 7,
                obtainedAt = "2026-01-20"
            )
        )

        fun getDefaultParty(characters: List<UserCharacter>): Party {
            return Party(
                slots = listOf(
                    PartySlot(
                        id = "slot_1",
                        userId = "user_001",
                        slotPosition = 1,
                        userCharacterId = "uc_001",
                        userCharacter = characters.find { it.id == "uc_001" }
                    ),
                    PartySlot(
                        id = "slot_2",
                        userId = "user_001",
                        slotPosition = 2,
                        userCharacterId = "uc_002",
                        userCharacter = characters.find { it.id == "uc_002" }
                    ),
                    PartySlot(
                        id = "slot_3",
                        userId = "user_001",
                        slotPosition = 3,
                        userCharacterId = "uc_004",
                        userCharacter = characters.find { it.id == "uc_004" }
                    )
                )
            )
        }
    }
}
