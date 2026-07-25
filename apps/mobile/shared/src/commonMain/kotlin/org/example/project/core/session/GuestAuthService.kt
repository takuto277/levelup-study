package org.example.project.core.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.SupabaseConfigSelector

/**
 * Supabase Anonymous Sign-In による Guest Session 管理。
 *
 * - 新規 Guest 作成
 * - refresh token によるセッション更新
 *
 * 使用する Supabase URL / anon key は [SupabaseConfigSelector] から取得し、
 * API 環境（dev/stg）切替に追従する。
 */
class GuestAuthService {

    private fun createClient(): SupabaseClient {
        val url = SupabaseConfigSelector.currentUrl
        val key = SupabaseConfigSelector.currentAnonKey
        println("[GuestAuth] Supabase URL: $url")
        println("[GuestAuth] Anon key prefix: ${key.take(20)}...")
        require(url.isNotBlank() && key.isNotBlank()) {
            "Supabase URL / anon key が設定されていません。local.properties または環境変数を確認してください（現在環境: ${ApiRoutes.BASE_URL}）。"
        }
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            install(Auth) {
                sessionManager = MemorySessionManager()
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                autoSaveToStorage = false
                enableLifecycleCallbacks = false
            }
        }
    }

    /**
     * 匿名ユーザーを新規作成し、セッションを返す。
     */
    suspend fun signInAnonymously(): GuestSession {
        println("[GuestAuth] 匿名サインイン開始...")
        val client = createClient()
        client.auth.signInAnonymously()
        val session = client.auth.currentSessionOrNull()
            ?: error("匿名サインイン後にセッションが取得できません")
        println("[GuestAuth] 匿名サインイン成功 userId=${session.user?.id}")
        return session.toGuestSession()
    }

    /**
     * 保存されている refresh token を使ってセッションを更新する。
     */
    suspend fun refreshSession(refreshToken: String): GuestSession {
        val client = createClient()
        val session = client.auth.refreshSession(refreshToken)
        return session.toGuestSession()
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
