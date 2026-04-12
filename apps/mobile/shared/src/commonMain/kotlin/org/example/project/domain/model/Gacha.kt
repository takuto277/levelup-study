package org.example.project.domain.model

/**
 * ガチャバナーマスタデータ
 * Source of Truth: サーバー
 */
data class GachaBanner(
    val id: String,
    val name: String,
    val bannerType: BannerType,
    val startAt: String,
    val endAt: String,
    val pityThreshold: Int?,
    val rateTable: String,
    val isActive: Boolean,
    /** ピックアップ行（サーバー `m_gacha_banner_featured`） */
    val featured: List<GachaBannerFeatured> = emptyList(),
    /** 表示用: ピックアップの `item_name` を「 · 」で連結（空なら UI でフォールバック） */
    val featuredSummary: String = ""
)

/** バナー種別 */
enum class BannerType {
    CHARACTER,
    WEAPON,
    COSTUME,
    MIXED
}

/**
 * ガチャピックアップ対象
 */
data class GachaBannerFeatured(
    val id: String,
    val bannerId: String,
    val itemId: String,
    val itemType: GachaResultType,
    val rateUp: Float,
    val itemName: String = "",
    val rarity: Int = 0,
    val imageUrl: String = ""
)

/**
 * ガチャ結果（履歴）
 */
data class GachaResult(
    val id: String,
    val userId: String,
    val bannerId: String,
    val resultType: GachaResultType,
    val resultItemId: String,
    val name: String = "",
    val rarity: Int = 0,
    val pityCount: Int,
    val isNew: Boolean = true,
    val createdAt: String
)

/** ガチャ排出タイプ */
enum class GachaResultType {
    CHARACTER,
    WEAPON,
    COSTUME
}

/**
 * 召喚 UI のメイン立ち絵用。画像 URL があるピックアップを優先し、なければキャラ枠→先頭。
 */
fun GachaBanner.primaryFeaturedForHero(): GachaBannerFeatured? {
    if (featured.isEmpty()) return null
    featured.firstOrNull { it.imageUrl.isNotBlank() }?.let { return it }
    featured.firstOrNull { it.itemType == GachaResultType.CHARACTER }?.let { return it }
    return featured.first()
}
