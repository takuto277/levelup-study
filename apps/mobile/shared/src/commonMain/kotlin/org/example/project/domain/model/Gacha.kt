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
    val isActive: Boolean
)

/** バナー種別 */
enum class BannerType {
    CHARACTER,
    WEAPON,
    MIXED
}

/**
 * ガチャ結果（履歴）
 */
data class GachaResult(
    val id: String,
    val userId: String,
    val bannerId: String,
    val resultType: GachaResultType,
    val resultItemId: String,
    val pityCount: Int,
    val createdAt: String
)

/** ガチャ排出タイプ */
enum class GachaResultType {
    CHARACTER,
    WEAPON
}
