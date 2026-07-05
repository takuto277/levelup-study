package org.example.project.core.session

/**
 * デバッグビルドでの認証セッション種別。
 *
 * - [SEED]: 開発用固定ユーザー ID と開発 JWT を使う既存方式（互換性のためデフォルト）
 * - [GUEST]: Supabase Anonymous Sign-In による端末固有の匿名ユーザー
 */
enum class SessionMode {
    SEED,
    GUEST,
    ;

    companion object {
        fun resolve(raw: String?): SessionMode =
            when (raw?.lowercase()) {
                "guest" -> GUEST
                else -> SEED
            }
    }
}

fun SessionMode.name(): String = name.lowercase()
