package org.example.project.features.party

import org.example.project.domain.model.Party
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.model.UserWeapon
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.PartyRepository
import org.example.project.domain.repository.WeaponRepository

/**
 * 編成画面のユースケース
 * パーティ編成・キャラ/武器の強化を管理
 */
class PartyUseCase(
    private val partyRepository: PartyRepository,
    private val characterRepository: CharacterRepository,
    private val weaponRepository: WeaponRepository
) {
    /** 編成画面に必要な全データ */
    data class PartyScreenData(
        val party: Party,
        val ownedCharacters: List<UserCharacter>,
        val ownedWeapons: List<UserWeapon>
    )

    /** 編成画面データを一括取得 */
    suspend fun loadPartyData(): PartyScreenData {
        val party = partyRepository.getParty()
        val characters = characterRepository.getUserCharacters()
        val weapons = weaponRepository.getUserWeapons()
        return PartyScreenData(party, characters, weapons)
    }

    /** スロットにキャラクターを配置 */
    suspend fun assignCharacterToSlot(slotPosition: Int, userCharacterId: String) {
        partyRepository.updateSlot(slotPosition, userCharacterId)
    }

    /** スロットからキャラクターを外す */
    suspend fun removeFromSlot(slotPosition: Int) {
        partyRepository.removeFromSlot(slotPosition)
    }

    /** キャラクターをレベルアップ */
    suspend fun levelUpCharacter(userCharacterId: String): UserCharacter {
        return characterRepository.levelUpCharacter(userCharacterId)
    }

    /** 武器をレベルアップ */
    suspend fun levelUpWeapon(userWeaponId: String): UserWeapon {
        return weaponRepository.levelUpWeapon(userWeaponId)
    }

    /** 武器を装備（null で解除） */
    suspend fun equipWeapon(userCharacterId: String, userWeaponId: String?) {
        weaponRepository.equipWeapon(userCharacterId, userWeaponId)
    }
}
