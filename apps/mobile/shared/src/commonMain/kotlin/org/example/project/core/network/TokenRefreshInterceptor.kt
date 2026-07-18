package org.example.project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.example.project.core.network.ApiRoutes
import org.example.project.core.session.GuestAuthService
import org.example.project.core.session.GuestSession
import org.example.project.core.session.SecureSessionStore
import org.example.project.core.session.SessionMode
import org.example.project.core.session.StoredGuestSession
import org.example.project.core.session.UserSessionStore

/**
 * Guest モード時に access token の有効期限をリクエスト前に確認し、
 * 必要に応じて refresh する Ktor インターセプター。
 *
 * 単一の [HttpClient] 内で single-flight（Mutex）に refresh を行い、
 * 更新後の token pair をメモリ（[UserSessionStore]）と [SecureSessionStore]
 * の両方に反映する。
 */
class TokenRefreshInterceptor(
    private val guestAuthService: GuestAuthService,
    private val secureSessionStore: SecureSessionStore,
) {

    private val mutex = Mutex()

    /**
     * [client] の send pipeline にインストールする。
     */
    fun install(client: HttpClient) {
        client.sendPipeline.intercept(HttpSendPipeline.State) {
            if (UserSessionStore.sessionMode != SessionMode.GUEST) return@intercept

            val token = ensureFreshToken()
            token?.let {
                context.headers.remove(HttpHeaders.Authorization)
                context.header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    private suspend fun ensureFreshToken(): String? {
        val expiry = UserSessionStore.authTokenExpiresAt ?: return UserSessionStore.authToken
        val now = Clock.System.now().epochSeconds
        if (now < expiry - REFRESH_MARGIN_SECONDS) {
            return UserSessionStore.authToken
        }

        // 期限が近い / 切れているので single-flight で refresh
        mutex.withLock {
            val currentExpiry = UserSessionStore.authTokenExpiresAt ?: return UserSessionStore.authToken
            val currentNow = Clock.System.now().epochSeconds
            if (currentNow < currentExpiry - REFRESH_MARGIN_SECONDS) {
                return UserSessionStore.authToken
            }

            val envKey = ApiRoutes.BASE_URL.hashCode().toString()
            val stored = secureSessionStore.load(envKey) ?: return UserSessionStore.authToken
            return try {
                val session = guestAuthService.refreshSession(stored.refreshToken)
                secureSessionStore.save(envKey, session.toStored(envKey))
                UserSessionStore.setSession(session.userId, session.accessToken, session.expiresAtEpochSeconds)
                session.accessToken
            } catch (_: Exception) {
                // refresh 失敗時は既存 token をそのまま使い、401 時に再初期化を促す。
                UserSessionStore.authToken
            }
        }
    }

    private fun GuestSession.toStored(envKey: String): StoredGuestSession = StoredGuestSession(
        environment = envKey,
        userId = userId,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochSeconds = expiresAtEpochSeconds,
    )

    companion object {
        /** 有効期限前に refresh を行う余裕（秒） */
        private const val REFRESH_MARGIN_SECONDS = 300L
    }
}
