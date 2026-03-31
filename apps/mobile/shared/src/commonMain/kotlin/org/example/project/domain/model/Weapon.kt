package org.example.project.domain.model

/**
 * 武器マスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
data class MasterWeapon(
    val id: String,
    val name: String,
    val rarity: Int,
    val baseAtk: Int,
    val skillName: String? = null,
    val skillDescription: String? = null,
    val imageUrl: String,
    val isActive: Boolean
)

/**
 * ユーザー所持武器
 * Source of Truth: サーバー
 *
 * refinement_level: 精錬段階 0〜4
 *   ガチャで重複入手するたびに +1。精錬MAX(4) 済みでさらに重複した場合は
 *   ★5: stones+15 / ★4以下: gold 変換される（サーバー側で処理）。
 */
data class UserWeapon(
    val id: String,
    val userId: String,
    val weaponId: String,
    val weapon: MasterWeapon? = null,
    val level: Int,
    val refinementLevel: Int = 0,
    val obtainedAt: String
) {
    /** 精錬MAX かどうか */
    val isMaxRefinement: Boolean get() = refinementLevel >= 4
}
