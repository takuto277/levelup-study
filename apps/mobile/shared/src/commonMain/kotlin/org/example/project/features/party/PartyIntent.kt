package org.example.project.features.party

/**
 * 編成（Party）画面のユーザー操作
 */
sealed interface PartyIntent {
    /** 編成情報をリフレッシュ */
    data object Refresh : PartyIntent

    /** パーティスロットを選択（キャラ配置先の指定） */
    data class SelectSlot(val slotPosition: Int) : PartyIntent

    /** キャラクターをスロットに配置 */
    data class AssignCharacter(val slotPosition: Int, val userCharacterId: String) : PartyIntent

    /** スロットからキャラクターを外す */
    data class RemoveFromSlot(val slotPosition: Int) : PartyIntent

    /** キャラクター詳細を開く */
    data class SelectCharacter(val userCharacterId: String) : PartyIntent

    /** キャラクター詳細を閉じる */
    data object DismissCharacterDetail : PartyIntent

    /** 武器を装備 */
    data class EquipWeapon(val userCharacterId: String, val userWeaponId: String?) : PartyIntent

    /** キャラクターをレベルアップ */
    data class LevelUpCharacter(val userCharacterId: String) : PartyIntent

    /** 武器をレベルアップ */
    data class LevelUpWeapon(val userWeaponId: String) : PartyIntent
}
