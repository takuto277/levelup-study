package org.example.project.domain.model

/**
 * 衣装マスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 *
 * character_id が null → 全キャラ汎用衣装
 * shop_price_stones が null → ショップ非売品（ガチャ限定 or イベント報酬）
 */
data class MasterCostume(
    val id: String,
    val characterId: String?,
    val name: String,
    val rarity: Int,
    val imageUrl: String,
    val shopPriceStones: Int?,
    val isLimited: Boolean,
    val isActive: Boolean
)

/**
 * ユーザー所持衣装
 * Source of Truth: サーバー
 *
 * 衣装は凸なし。重複排出時は stones +15 に変換される（サーバー側で処理）。
 */
data class UserCostume(
    val id: String,
    val userId: String,
    val costumeId: String,
    val costume: MasterCostume? = null,
    val obtainedAt: String
)
