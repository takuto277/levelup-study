package org.example.project.core.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import org.example.project.core.network.GENERATED_SUPABASE_ANON_KEY
import org.example.project.core.network.GENERATED_SUPABASE_URL

/**
 * Supabase Anonymous Sign-In による Guest Session 管理。
 *
 * - 新規 Guest 作成
 * - refresh token によるセッション更新
 * - 現在セッションの復元
 */
class GuestAuthService {

    private val client: SupabaseClient by lazy {
        val url = GENERATED_SUPABASE_URL
        val key = GENERATED_SUPABASE_ANON_KEY
        require(url.isNotBlank() && key.isNotBlank()) {
            "Supabase URL / anon key が設定されていません。local.properties または環境変数を確認してください。"
        }
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            install(Auth)
        }
    }

    /**
     * 匿名ユーザーを新規作成し、セッションを返す。
     */
    suspend fun signInAnonymously(): GuestSession {
        client.auth.signInAnonymously()
        return client.auth.currentSessionOrNull()?.toGuestSession()
            ?: error("匿名サインイン後にセッションが取得できません")
    }

    /**
     * 保存されている refresh token を使ってセッションを更新する。
     */
    suspend fun refreshSession(refreshToken: String): GuestSession {
        val session = client.auth.refreshSession(refreshToken)
        return session.toGuestSession()
    }

    /**
     * 現在の Supabase セッションがあれば返す。
     */
    fun currentSessionOrNull(): GuestSession? {
        return client.auth.currentSessionOrNull()?.toGuestSession()
    }

    private fun UserSession.toGuestSession(): GuestSession {
        return GuestSession(
            userId = user?.id ?: error("匿名サインイン後に user ID が取得できません"),
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt.epochSeconds,
        )
    }
}

/**
 * Supabase Auth から取得した匿名 Guest セッション。
 */
data class GuestSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)
