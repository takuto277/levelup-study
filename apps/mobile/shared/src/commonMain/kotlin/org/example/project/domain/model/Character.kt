package org.example.project.domain.model

/**
 * キャラクターマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 */
data class MasterCharacter(
    val id: String,
    val name: String,
    val rarity: Int,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val imageUrl: String,
    val idleAnimationUrl: String?,
    val isActive: Boolean
)

/**
 * ユーザー所持キャラクター
 * Source of Truth: サーバー
 */
data class UserCharacter(
    val id: String,
    val userId: String,
    val characterId: String,
    val character: MasterCharacter? = null,
    val level: Int,
    val currentXp: Int,
    val equippedWeaponId: String?,
    val obtainedAt: String
)
