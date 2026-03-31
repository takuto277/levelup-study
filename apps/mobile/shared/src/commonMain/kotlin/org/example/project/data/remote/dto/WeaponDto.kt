package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.MasterWeapon
import org.example.project.domain.model.UserWeapon

// ── Response ────────────────────────────────────

@Serializable
data class MasterWeaponResponse(
    val id: String,
    val name: String,
    val rarity: Int,
    @SerialName("base_atk") val baseAtk: Int,
    @SerialName("skill_name") val skillName: String? = null,
    @SerialName("skill_description") val skillDescription: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class MasterWeaponListResponse(
    val weapons: List<MasterWeaponResponse>
)

@Serializable
data class UserWeaponResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("weapon_id") val weaponId: String,
    val weapon: MasterWeaponResponse? = null,
    val level: Int,
    @SerialName("refinement_level") val refinementLevel: Int = 0,
    @SerialName("obtained_at") val obtainedAt: String
)

@Serializable
data class UserWeaponListResponse(
    val weapons: List<UserWeaponResponse>
)

// ── Request ─────────────────────────────────────

@Serializable
data class EquipWeaponRequest(
    @SerialName("user_weapon_id") val userWeaponId: String?
)

// ── Mapper ──────────────────────────────────────

fun MasterWeaponResponse.toDomain(): MasterWeapon = MasterWeapon(
    id = id,
    name = name,
    rarity = rarity,
    baseAtk = baseAtk,
    skillName = skillName,
    skillDescription = skillDescription,
    imageUrl = imageUrl,
    isActive = isActive
)

fun UserWeaponResponse.toDomain(): UserWeapon = UserWeapon(
    id = id,
    userId = userId,
    weaponId = weaponId,
    weapon = weapon?.toDomain(),
    level = level,
    refinementLevel = refinementLevel,
    obtainedAt = obtainedAt
)
