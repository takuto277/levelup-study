package org.example.project.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import org.example.project.core.network.ApiRoutes

/**
 * ビルド種別と [SessionMode] に応じた認証状態を管理する。
 *
 * - Release: 常に Guest モード。Supabase 匿名サインインを行う。
 * - Debug Seed: 開発用固定ユーザー ID と dev JWT を使用。
 * - Debug Guest: Supabase 匿名サインインを行う。
 */
class SessionManager(
    private val guestAuthService: GuestAuthService,
    private val secureSessionStore: SecureSessionStore,
    private val userRepository: org.example.project.domain.repository.UserRepository,
) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Initializing)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * アプリ起動時に呼び出す。
     */
    suspend fun initialize(isDebug: Boolean) {
        UserSessionStore.setDebugBuild(isDebug)
        // 平文保存されていた既存 auth_token を削除（移行用）
        UserSessionStore.clearLegacyPlainAuthToken()
        when {
            !isDebug -> initializeGuest()
            UserSessionStore.sessionMode == SessionMode.SEED -> initializeSeed()
            else -> initializeGuest()
        }
    }

    /**
     * Debug ビルド用: Seed / Guest を切り替える。
     * 切替前に現在のセッションとローカルキャッシュをクリアし、
     * モード間のユーザー情報の混在を防ぐ。
     */
    suspend fun switchMode(mode: SessionMode) {
        clearCurrentSession()
        UserSessionStore.setSessionMode(mode)
        when (mode) {
            SessionMode.SEED -> initializeSeed()
            SessionMode.GUEST -> initializeGuest()
        }
    }

    /**
     * API 環境切り替え時に呼び出す。
     */
    suspend fun onEnvironmentChanged() {
        if (UserSessionStore.sessionMode == SessionMode.GUEST) {
            initializeGuest()
        }
    }

    private suspend fun initializeSeed() {
        val seedId = UserSessionStore.DEV_SEED_USER_ID
        UserSessionStore.setSession(seedId)
        _state.value = SessionState.Ready(SessionMode.SEED, seedId)
    }

    private suspend fun initializeGuest() {
        _state.value = SessionState.Initializing
        try {
            val envKey = environmentKey()
            val stored = secureSessionStore.load(envKey)
            val session = if (stored != null) {
                // 有効期限が近いか切れていれば refresh
                val now = Clock.System.now().epochSeconds
                if (now >= stored.expiresAtEpochSeconds - REFRESH_MARGIN_SECONDS) {
                    guestAuthService.refreshSession(stored.refreshToken)
                } else {
                    stored.toGuestSession()
                }
            } else {
                guestAuthService.signInAnonymously()
            }
            secureSessionStore.save(envKey, session.toStored(envKey))
            UserSessionStore.setSession(session.userId, session.accessToken)
            // public.users を冪等に作成・取得
            runCatching { userRepository.getOrCreateAuthUser() }
            _state.value = SessionState.Ready(SessionMode.GUEST, session.userId)
        } catch (e: Exception) {
            _state.value = SessionState.RecoverableError(SessionError.InitializationFailed(e))
        }
    }

    /**
     * 無効な Guest Session から新しい Guest を作成する（ユーザー確認後）。
     */
    suspend fun resetGuestSession() {
        clearCurrentSession()
        initializeGuest()
    }

    private suspend fun clearCurrentSession() {
        val envKey = environmentKey()
        secureSessionStore.remove(envKey)
        userRepository.clearCache()
        UserSessionStore.clear()
    }

    private fun environmentKey(): String = ApiRoutes.BASE_URL.hashCode().toString()

    companion object {
        /** 有効期限前に refresh を行う余裕（秒） */
        private const val REFRESH_MARGIN_SECONDS = 300L
    }
}

/** セッション状態 */
sealed interface SessionState {
    data object Initializing : SessionState
    data class Ready(val mode: SessionMode, val userId: String) : SessionState
    data class RecoverableError(val reason: SessionError) : SessionState
    data class ResetRequired(val previousUserId: String?) : SessionState
}

/** セッションエラー */
sealed interface SessionError {
    val throwable: Throwable?

    data class InitializationFailed(override val throwable: Throwable?) : SessionError
    data class RefreshFailed(override val throwable: Throwable?) : SessionError
}

private fun StoredGuestSession.toGuestSession(): GuestSession = GuestSession(
    userId = userId,
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
)

private fun GuestSession.toStored(envKey: String): StoredGuestSession = StoredGuestSession(
    environment = envKey,
    userId = userId,
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
)
