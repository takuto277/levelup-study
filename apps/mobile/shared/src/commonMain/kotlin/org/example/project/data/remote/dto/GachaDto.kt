package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    @SerialName("rate_table") val rateTable: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class GachaBannerListResponse(
    val banners: List<GachaBannerResponse>
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
data class GachaPullResponse(
    val results: List<GachaResultResponse>,
    @SerialName("updated_user") val updatedUser: UserResponse
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
    rateTable = rateTable,
    isActive = isActive
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
