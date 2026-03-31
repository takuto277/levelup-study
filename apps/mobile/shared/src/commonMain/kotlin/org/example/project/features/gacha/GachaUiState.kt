package org.example.project.features.gacha

import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResultType

/**
 * ガチャ画面のフェーズ（状態遷移）
 * BANNER_SELECT → CONFIRM → PULLING → RESULT → (CONFIRM or BANNER_SELECT)
 */
enum class GachaPhase {
    /** バナー一覧から選択 */
    BANNER_SELECT,
    /** 選択したバナーの詳細・引く確認 */
    CONFIRM,
    /** ガチャ演出中（アニメーション） */
    PULLING,
    /** 結果表示 */
    RESULT
}

/**
 * ガチャ結果の表示用モデル
 * GachaResult (domain) + マスタデータから名前・レアリティを解決したUI表示用
 */
data class GachaResultItem(
    val id: String,
    val name: String,
    val rarity: Int,
    val imageUrl: String = "",
    val type: GachaResultType,
    val isNew: Boolean = false
)

/**
 * 召喚（Gacha）画面の UI 状態
 */
data class GachaUiState(
    val phase: GachaPhase = GachaPhase.BANNER_SELECT,
    val banners: List<GachaBanner> = emptyList(),
    val selectedBanner: GachaBanner? = null,
    val currentStones: Int = 0,
    val pityCount: Int = 0,
    val pullResults: List<GachaResultItem> = emptyList(),
    val lastPullCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    companion object {
        const val SINGLE_PULL_COST = 50
        const val MULTI_PULL_COST = 450
    }

    val canPullSingle: Boolean get() = currentStones >= SINGLE_PULL_COST
    val canPullMulti: Boolean get() = currentStones >= MULTI_PULL_COST
    val highestRarity: Int get() = pullResults.maxOfOrNull { it.rarity } ?: 3
}
