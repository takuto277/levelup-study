package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.MasterStudyGenre

@Serializable
data class GenreResponse(
    val id: String,
    val slug: String,
    val label: String,
    val emoji: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class GenreListResponse(
    val genres: List<GenreResponse>
)

fun GenreResponse.toDomain(): MasterStudyGenre = MasterStudyGenre(
    id = id,
    slug = slug,
    label = label,
    emoji = emoji,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isDefault = isDefault,
    isActive = isActive
)
