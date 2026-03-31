package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.MasterCharacter
import org.example.project.domain.model.UserCharacter

// ── Response ────────────────────────────────────

@Serializable
data class MasterCharacterResponse(
    val id: String,
    val name: String,
    val rarity: Int,
    val element: String? = null,
    @SerialName("base_hp") val baseHp: Int,
    @SerialName("base_atk") val baseAtk: Int,
    @SerialName("base_def") val baseDef: Int,
    @SerialName("skill_name") val skillName: String? = null,
    @SerialName("skill_description") val skillDescription: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("idle_animation_url") val idleAnimationUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class MasterCharacterListResponse(
    val characters: List<MasterCharacterResponse>
)

@Serializable
data class UserCharacterResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("character_id") val characterId: String,
    val character: MasterCharacterResponse? = null,
    val level: Int,
    @SerialName("current_xp") val currentXp: Int,
    @SerialName("breakthrough_level") val breakthroughLevel: Int = 0,
    @SerialName("equipped_weapon_id") val equippedWeaponId: String? = null,
    @SerialName("equipped_costume_id") val equippedCostumeId: String? = null,
    @SerialName("obtained_at") val obtainedAt: String
)

@Serializable
data class UserCharacterListResponse(
    val characters: List<UserCharacterResponse>
)

// ── Mapper ──────────────────────────────────────

fun MasterCharacterResponse.toDomain(): MasterCharacter = MasterCharacter(
    id = id,
    name = name,
    rarity = rarity,
    element = element,
    baseHp = baseHp,
    baseAtk = baseAtk,
    baseDef = baseDef,
    skillName = skillName,
    skillDescription = skillDescription,
    imageUrl = imageUrl,
    idleAnimationUrl = idleAnimationUrl,
    isActive = isActive
)

fun UserCharacterResponse.toDomain(): UserCharacter = UserCharacter(
    id = id,
    userId = userId,
    characterId = characterId,
    character = character?.toDomain(),
    level = level,
    currentXp = currentXp,
    breakthroughLevel = breakthroughLevel,
    equippedWeaponId = equippedWeaponId,
    equippedCostumeId = equippedCostumeId,
    obtainedAt = obtainedAt
)
