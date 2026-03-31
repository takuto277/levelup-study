package org.example.project.domain.model

/**
 * パーティ編成
 * Source of Truth: サーバー
 */
data class Party(
    val slots: List<PartySlot>
) {
    /** メインキャラクター（スロット1のキャラ） */
    val mainCharacter: UserCharacter?
        get() = slots.find { it.slotPosition == 1 }?.userCharacter
}

/**
 * パーティスロット（1〜4）
 */
data class PartySlot(
    val id: String,
    val userId: String,
    val slotPosition: Int,
    val userCharacterId: String,
    val userCharacter: UserCharacter? = null
)
