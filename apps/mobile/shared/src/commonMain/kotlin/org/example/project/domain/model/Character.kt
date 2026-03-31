package org.example.project.domain.model

/**
 * キャラクターマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
data class MasterCharacter(
    val id: String,
    val name: String,
    val rarity: Int,
    val element: String? = null,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val skillName: String? = null,
    val skillDescription: String? = null,
    val imageUrl: String,
    val idleAnimationUrl: String?,
    val isActive: Boolean
)

/**
 * ユーザー所持キャラクター
 * Source of Truth: サーバー
 *
 * breakthrough_level: 凸（突破）段階 0〜6
 *   ガチャで重複入手するたびに +1。完凸(6) 済みでさらに重複した場合は
 *   ★5: stones+25 / ★4以下: gold 変換される（サーバー側で処理）。
 */
data class UserCharacter(
    val id: String,
    val userId: String,
    val characterId: String,
    val character: MasterCharacter? = null,
    val level: Int,
    val currentXp: Int,
    val breakthroughLevel: Int = 0,
    val equippedWeaponId: String?,
    val equippedCostumeId: String? = null,
    val obtainedAt: String
) {
    /** 凸によるHP倍率 (1.0 〜 1.3) */
    val breakthroughHpMultiplier: Float get() = 1f + breakthroughLevel * 0.05f
    /** 凸によるATK倍率 (1.0 〜 1.25) */
    val breakthroughAtkMultiplier: Float get() = 1f + breakthroughLevel * (if (breakthroughLevel <= 1) 0.03f else 0.04f)
    /** 完凸かどうか */
    val isFullBreakthrough: Boolean get() = breakthroughLevel >= 6
}
