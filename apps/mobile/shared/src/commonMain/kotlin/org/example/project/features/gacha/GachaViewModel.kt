package org.example.project.features.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
                _uiState.update {
                    it.copy(
                        banners = emptyList(),
                        isLoading = false,
                        error = e.message ?: "バナーの取得に失敗しました"
                    )
                }
            }
        }
    }

    private fun selectBanner(bannerId: String) {
        val banner = _uiState.value.banners.find { it.id == bannerId }
        _uiState.update {
            it.copy(phase = GachaPhase.CONFIRM, selectedBanner = banner)
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

            try {
                val apiResults = gachaUseCase.pullGacha(bannerId, count)
                delay(MIN_ANIMATION_MS)
                val stones = gachaUseCase.getCurrentStones()

                val results = apiResults.map { result ->
                    GachaResultItem(
                        id = result.resultItemId,
                        name = result.name.ifEmpty { result.resultItemId },
                        rarity = result.rarity.takeIf { it > 0 } ?: 3,
                        type = result.resultType,
                        isNew = result.isNew
                    )
                }

                _uiState.update {
                    it.copy(
                        phase = GachaPhase.RESULT,
                        pullResults = results,
                        currentStones = stones
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        phase = GachaPhase.CONFIRM,
                        error = e.message ?: "ガチャの実行に失敗しました"
                    )
                }
            }
        }
    }
}
