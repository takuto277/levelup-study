package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.example.project.domain.model.BannerType
import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResult
import org.example.project.domain.model.GachaResultType

// ── Response ────────────────────────────────────

@Serializable
data class GachaBannerResponse(
    val id: String,
    val name: String,
    @SerialName("banner_type") val bannerType: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("pity_threshold") val pityThreshold: Int? = null,
    @SerialName("rate_table") val rateTable: JsonElement,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class GachaBannerListResponse(
    val banners: List<GachaBannerResponse>
)

@Serializable
data class GachaPullResultResponse(
    @SerialName("result_type") val resultType: String,
    @SerialName("item_id") val itemId: String,
    val name: String = "",
    val rarity: Int = 0,
    @SerialName("is_new") val isNew: Boolean = false,
    @SerialName("pity_count") val pityCount: Int = 0
)

@Serializable
data class GachaPullResponse(
    val results: List<GachaPullResultResponse>,
    @SerialName("stones_spent") val stonesSpent: Int = 0,
    @SerialName("remaining_stones") val remainingStones: Int = 0,
    @SerialName("updated_user") val updatedUser: UserResponse? = null
)

@Serializable
data class GachaResultResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("banner_id") val bannerId: String,
    @SerialName("result_type") val resultType: String,
    @SerialName("result_item_id") val resultItemId: String,
    @SerialName("pity_count") val pityCount: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class GachaHistoryResponse(
    val history: List<GachaResultResponse>
)

@Serializable
data class PityCountResponse(
    @SerialName("pity_count") val pityCount: Int
)

// ── Request ─────────────────────────────────────

@Serializable
data class GachaPullRequest(
    @SerialName("banner_id") val bannerId: String,
    val count: Int = 1
)

// ── Mapper ──────────────────────────────────────

fun GachaBannerResponse.toDomain(): GachaBanner = GachaBanner(
    id = id,
    name = name,
    bannerType = runCatching { BannerType.valueOf(bannerType.uppercase()) }
        .getOrDefault(BannerType.MIXED),
    startAt = startAt,
    endAt = endAt,
    pityThreshold = pityThreshold,
    rateTable = rateTable.toString(),
    isActive = isActive
)

fun GachaPullResultResponse.toDomain(): GachaResult = GachaResult(
    id = "",
    userId = "",
    bannerId = "",
    resultType = runCatching { GachaResultType.valueOf(resultType.uppercase()) }
        .getOrDefault(GachaResultType.CHARACTER),
    resultItemId = itemId,
    name = name,
    rarity = rarity,
    pityCount = pityCount,
    isNew = isNew,
    createdAt = ""
)

fun GachaResultResponse.toDomain(): GachaResult = GachaResult(
    id = id,
    userId = userId,
    bannerId = bannerId,
    resultType = runCatching { GachaResultType.valueOf(resultType.uppercase()) }
        .getOrDefault(GachaResultType.CHARACTER),
    resultItemId = resultItemId,
    pityCount = pityCount,
    createdAt = createdAt
)
