package org.example.project.features.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.model.BannerType
import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResultType

/**
 * 召喚（Gacha）画面の ViewModel
 * バナー選択 → 確認 → 演出 → 結果 のフェーズ管理
 */
class GachaViewModel(
    private val gachaUseCase: GachaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GachaUiState())
    val uiState: StateFlow<GachaUiState> = _uiState.asStateFlow()

    companion object {
        /** 演出の最低再生時間（ミリ秒） */
        private const val MIN_ANIMATION_MS = 3200L
    }

    init {
        loadBanners()
    }

    fun onIntent(intent: GachaIntent) {
        when (intent) {
            is GachaIntent.Refresh -> loadBanners()
            is GachaIntent.SelectBanner -> selectBanner(intent.bannerId)
            is GachaIntent.BackToBannerSelect -> _uiState.update {
                it.copy(phase = GachaPhase.BANNER_SELECT, selectedBanner = null, pullResults = emptyList())
            }
            is GachaIntent.PullSingle -> pullGacha(intent.bannerId, 1)
            is GachaIntent.PullMulti -> pullGacha(intent.bannerId, 10)
            is GachaIntent.PullAgain -> _uiState.update {
                it.copy(phase = GachaPhase.CONFIRM, pullResults = emptyList())
            }
            is GachaIntent.DismissResults -> _uiState.update {
                it.copy(phase = GachaPhase.BANNER_SELECT, selectedBanner = null, pullResults = emptyList())
            }
            is GachaIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadBanners() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val banners = gachaUseCase.loadBanners()
                val stones = gachaUseCase.getCurrentStones()
                _uiState.update {
                    it.copy(banners = banners, currentStones = stones, isLoading = false)
                }
            } catch (e: Exception) {
                // API未接続時はモックデータで動作確認可能にする
                _uiState.update {
                    it.copy(
                        banners = mockBanners,
                        currentStones = 1250,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun selectBanner(bannerId: String) {
        viewModelScope.launch {
            val banner = _uiState.value.banners.find { it.id == bannerId }
            val pity = runCatching { gachaUseCase.getPityCount(bannerId) }.getOrDefault(47)
            _uiState.update {
                it.copy(phase = GachaPhase.CONFIRM, selectedBanner = banner, pityCount = pity)
            }
        }
    }

    private fun pullGacha(bannerId: String, count: Int) {
        viewModelScope.launch {
            val cost = if (count == 1) GachaUiState.SINGLE_PULL_COST else GachaUiState.MULTI_PULL_COST
            if (_uiState.value.currentStones < cost) {
                _uiState.update { it.copy(error = "知識の結晶が足りません") }
                return@launch
            }

            _uiState.update { it.copy(phase = GachaPhase.PULLING, lastPullCount = count, error = null) }

            // 演出の最低再生時間を保証しつつ API 呼び出し
            val results = try {
                val apiResults = gachaUseCase.pullGacha(bannerId, count)
                delay(MIN_ANIMATION_MS)
                val stones = gachaUseCase.getCurrentStones()
                _uiState.update { it.copy(currentStones = stones) }

                apiResults.map { result ->
                    GachaResultItem(
                        id = result.id,
                        name = result.resultItemId,
                        rarity = 3,
                        type = result.resultType,
                        isNew = false
                    )
                }
            } catch (e: Exception) {
                // API未接続時はモック結果を生成
                delay(MIN_ANIMATION_MS)
                _uiState.update { it.copy(currentStones = it.currentStones - cost) }
                generateMockResults(count)
            }

            _uiState.update {
                it.copy(phase = GachaPhase.RESULT, pullResults = results)
            }
        }
    }

    // ── モックデータ（API未接続時のUI開発用） ──────────────

    private val mockBanners = listOf(
        GachaBanner(
            id = "banner_1", name = "光の勇者ピックアップ",
            bannerType = BannerType.CHARACTER, startAt = "", endAt = "",
            pityThreshold = 90, rateTable = "", isActive = true
        ),
        GachaBanner(
            id = "banner_2", name = "伝説の聖剣ガチャ",
            bannerType = BannerType.WEAPON, startAt = "", endAt = "",
            pityThreshold = 80, rateTable = "", isActive = true
        ),
        GachaBanner(
            id = "banner_3", name = "新学期スペシャル召喚",
            bannerType = BannerType.MIXED, startAt = "", endAt = "",
            pityThreshold = null, rateTable = "", isActive = true
        )
    )

    private val characterPool = listOf(
        "光の勇者アリア" to 5, "闇の魔王ゼファー" to 5, "聖女セラフィーナ" to 5,
        "炎の魔術師レイ" to 4, "氷の弓使いリナ" to 4, "風の剣士カイト" to 4,
        "見習い戦士タロウ" to 3, "森の精霊コダマ" to 3, "街の商人マルコ" to 3
    )

    private val weaponPool = listOf(
        "聖剣エクスカリバー" to 5, "闇の大鎌デスサイズ" to 5,
        "氷の弓フロストアロー" to 4, "炎の杖ヘルフレイム" to 4,
        "鉄の剣" to 3, "木の杖" to 3, "革の盾" to 3
    )

    private fun generateMockResults(count: Int): List<GachaResultItem> {
        return (1..count).map { i ->
            val rarity = rollRarity(guaranteeFourStar = count >= 10 && i == count)
            val isCharacter = (0..1).random() == 0
            val pool = if (isCharacter) characterPool else weaponPool
            val item = pool.filter { it.second == rarity }.randomOrNull()
                ?: pool.filter { it.second <= rarity }.random()

            GachaResultItem(
                id = "mock_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_$i",
                name = item.first,
                rarity = item.second,
                type = if (isCharacter) GachaResultType.CHARACTER else GachaResultType.WEAPON,
                isNew = (0..3).random() == 0
            )
        }
    }

    private fun rollRarity(guaranteeFourStar: Boolean): Int {
        val roll = (1..1000).random()
        return when {
            roll <= 30 -> 5              // 3%
            roll <= 180 || guaranteeFourStar -> 4  // 15%
            else -> 3                    // 82%
        }
    }
}
