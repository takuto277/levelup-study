package org.example.project.domain.model

/**
 * 勉強ジャンルマスタデータ
 * Source of Truth: サーバー（ローカルにキャッシュ）
 *
 * サーバーの m_study_genres テーブルに対応。
 * 初期データは is_default = true の6ジャンル。
 * ユーザーは user_genres を通じてカスタムジャンルを追加・非表示化できる。
 */
data class MasterStudyGenre(
    val id: String,
    val slug: String,
    val label: String,
    val emoji: String,
    val colorHex: String,
    val sortOrder: Int,
    val isDefault: Boolean,
    val isActive: Boolean
)

/**
 * ユーザーのジャンル設定
 * Source of Truth: サーバー
 *
 * マスタジャンルのカスタマイズ（表示名・絵文字・色の上書き）と、
 * 完全なカスタムジャンル（genre_id = null）の両方を表現する。
 *
 * ジャンルを「削除」しても is_active = false にするだけ。
 * 過去の study_sessions はマスタ側の genre_id を直接参照しているため影響しない。
 */
data class UserGenre(
    val id: String,
    val userId: String,
    val genreId: String?,
    val genre: MasterStudyGenre? = null,
    val customLabel: String?,
    val customEmoji: String?,
    val customColorHex: String?,
    val sortOrder: Int,
    val isActive: Boolean
) {
    /** 表示用ラベル（カスタム → マスタ → フォールバック） */
    val displayLabel: String
        get() = customLabel ?: genre?.label ?: "不明"

    /** 表示用絵文字 */
    val displayEmoji: String
        get() = customEmoji ?: genre?.emoji ?: "📖"

    /** 表示用カラーHex */
    val displayColorHex: String
        get() = customColorHex ?: genre?.colorHex ?: "#6B7280"
}
