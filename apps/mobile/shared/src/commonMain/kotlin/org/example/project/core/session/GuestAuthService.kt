package org.example.project.core.session

import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.auth.Auth
import io.github.jan_tennert.supabase.auth.auth
import io.github.jan_tennert.supabase.auth.user.UserSession
import io.github.jan_tennert.supabase.createSupabaseClient
import org.example.project.core.network.GENERATED_SUPABASE_ANON_KEY
import org.example.project.core.network.GENERATED_SUPABASE_URL

/**
 * Supabase Anonymous Sign-In による Guest Session 管理。
 *
 * - 新規 Guest 作成
 * - 現在セッションの復元
 * - access token の refresh
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
     *
     * @param captchaToken 必要に応じて CAPTCHA token（現状は null）
     */
    suspend fun signInAnonymously(captchaToken: String? = null): GuestSession {
        val result = client.auth.signInAnonymously(
            captchaToken = captchaToken,
        )
        return result.toGuestSession()
    }

    /**
     * 保存されている refresh token を使ってセッションを更新する。
     */
    suspend fun refreshSession(refreshToken: String): GuestSession {
        val result = client.auth.refreshCurrentSession(refreshToken)
        return result.toGuestSession()
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
            expiresAtEpochSeconds = expiresAt,
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
