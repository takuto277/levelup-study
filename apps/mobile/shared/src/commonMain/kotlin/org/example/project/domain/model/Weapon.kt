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
    val imageUrl: String,
    val isActive: Boolean
)

/**
 * ユーザー所持武器
 * Source of Truth: サーバー
 */
data class UserWeapon(
    val id: String,
    val userId: String,
    val weaponId: String,
    val weapon: MasterWeapon? = null,
    val level: Int,
    val obtainedAt: String
)
