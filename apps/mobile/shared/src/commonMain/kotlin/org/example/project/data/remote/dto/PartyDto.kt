package org.example.project.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot

// ── Response ────────────────────────────────────

@Serializable
data class PartySlotResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("slot_position") val slotPosition: Int,
    @SerialName("user_character_id") val userCharacterId: String,
    @SerialName("user_character") val userCharacter: UserCharacterResponse? = null
)

@Serializable
data class PartyResponse(
    val slots: List<PartySlotResponse>
)

// ── Request ─────────────────────────────────────

@Serializable
data class UpdatePartySlotRequest(
    @SerialName("slot_position") val slotPosition: Int,
    @SerialName("user_character_id") val userCharacterId: String
)

@Serializable
data class RemovePartySlotRequest(
    @SerialName("slot_position") val slotPosition: Int
)

// ── Mapper ──────────────────────────────────────

fun PartySlotResponse.toDomain(): PartySlot = PartySlot(
    id = id,
    userId = userId,
    slotPosition = slotPosition,
    userCharacterId = userCharacterId,
    userCharacter = userCharacter?.toDomain()
)

fun PartyResponse.toDomain(): Party = Party(
    slots = slots.map { it.toDomain() }
)
