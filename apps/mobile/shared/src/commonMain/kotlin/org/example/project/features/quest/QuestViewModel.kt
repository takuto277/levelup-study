package org.example.project.features.quest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 冒険（ダンジョン選択）画面のViewModel
 *
 * - ローカルにデフォルトダンジョンを保持（オフライン対応）
 * - 将来的にサーバーから追加ダンジョンを取得してマージ
 */
class QuestViewModel {
    private val _uiState = MutableStateFlow(QuestUiState())
    val uiState: StateFlow<QuestUiState> = _uiState.asStateFlow()

    init {
        loadDungeons()
    }

    fun onIntent(intent: QuestIntent) {
        when (intent) {
            is QuestIntent.SelectDungeon -> selectDungeon(intent.dungeonId)
            is QuestIntent.DismissDetail -> _uiState.update { it.copy(selectedDungeon = null) }
            is QuestIntent.RefreshDungeons -> refreshDungeons()
        }
    }

    private fun selectDungeon(dungeonId: String) {
        val dungeon = _uiState.value.dungeons.find { it.id == dungeonId }
        _uiState.update { it.copy(selectedDungeon = dungeon) }
    }

    private fun refreshDungeons() {
        _uiState.update { it.copy(isLoading = true) }
        loadDungeons()
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadDungeons() {
        val localDungeons = getDefaultDungeons()
        _uiState.update { it.copy(dungeons = localDungeons, isLoading = false) }
    }

    companion object {
        fun getDefaultDungeons(): List<Dungeon> = listOf(
            Dungeon(
                id = "forest_of_beginnings", name = "はじまりの森",
                description = "新米冒険者の修行場。穏やかな森で基礎を固めよう。",
                difficulty = DungeonDifficulty.BEGINNER, category = DungeonCategory.GENERAL,
                totalStages = 10, clearedStages = 5, recommendedMinutes = 25,
                rewards = DungeonReward(gold = 100, exp = 50, gachaStones = 5),
                iconEmoji = "🌲"
            ),
            Dungeon(
                id = "crystal_cave", name = "水晶の洞窟",
                description = "輝く水晶に囲まれた神秘的な洞窟。集中力が試される。",
                difficulty = DungeonDifficulty.INTERMEDIATE, category = DungeonCategory.MATH,
                totalStages = 15, clearedStages = 3, recommendedMinutes = 30,
                rewards = DungeonReward(gold = 200, exp = 120, gachaStones = 10, bonusItemName = "知恵のかけら", bonusItemDropRate = 0.15f),
                iconEmoji = "💎"
            ),
            Dungeon(
                id = "flame_tower", name = "炎の塔",
                description = "灼熱の試練が待つ高層塔。長時間の集中力が鍵となる。",
                difficulty = DungeonDifficulty.ADVANCED, category = DungeonCategory.SCIENCE,
                totalStages = 20, clearedStages = 0, recommendedMinutes = 45,
                rewards = DungeonReward(gold = 350, exp = 200, gachaStones = 15, bonusItemName = "炎の紋章", bonusItemDropRate = 0.10f),
                iconEmoji = "🔥"
            ),
            Dungeon(
                id = "sky_sanctuary", name = "天空の聖域",
                description = "雲の上に広がる聖なる修行場。精神統一が求められる。",
                difficulty = DungeonDifficulty.EXPERT, category = DungeonCategory.LANGUAGE,
                totalStages = 25, clearedStages = 0, recommendedMinutes = 60,
                rewards = DungeonReward(gold = 500, exp = 350, gachaStones = 25, bonusItemName = "天翼のペンダント", bonusItemDropRate = 0.08f),
                iconEmoji = "⛅", isLocked = true
            ),
            Dungeon(
                id = "code_labyrinth", name = "コードの迷宮",
                description = "プログラミングの論理で解き進む知的ダンジョン。",
                difficulty = DungeonDifficulty.INTERMEDIATE, category = DungeonCategory.PROGRAMMING,
                totalStages = 15, clearedStages = 7, recommendedMinutes = 30,
                rewards = DungeonReward(gold = 250, exp = 150, gachaStones = 12, bonusItemName = "バグ退治の書", bonusItemDropRate = 0.20f),
                iconEmoji = "🏰"
            ),
            Dungeon(
                id = "abyss_of_knowledge", name = "深淵の図書館",
                description = "古代の知識が眠る禁断の地。伝説級の試練に挑め。",
                difficulty = DungeonDifficulty.LEGENDARY, category = DungeonCategory.GENERAL,
                totalStages = 50, clearedStages = 0, recommendedMinutes = 120,
                rewards = DungeonReward(gold = 1000, exp = 800, gachaStones = 50, bonusItemName = "叡智の王冠", bonusItemDropRate = 0.03f),
                iconEmoji = "📖", isLocked = true
            )
        )
    }
}
