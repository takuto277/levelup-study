package org.example.project.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.storage.KeyValueStore
import org.example.project.domain.model.User
import org.example.project.domain.repository.UserRepository

/**
 * ユーザーセッション管理
 *
 * アプリ起動時にユーザーID を保持し、全 API コールで利用する。
 * expect/actual の KeyValueStore を使用して永続化する。
 *
 * Debug ビルドでは [SessionModeStore] と連動し、Seed / Guest モードを切り替える。
 * Release ビルドでは常に Guest モード相当（forceDevSeedUserId=false）となる。
 */
object UserSessionStore {

    /** db/seed.sql の user1 と同じ ID（DEV_MODE + シード開発用） */
    const val DEV_SEED_USER_ID: String = "00000000-0000-0000-0000-000000000001"

    private var isDebugBuild: Boolean = false

    /**
     * プラットフォーム起動時に DEBUG / RELEASE を設定する。
     * 呼び出し後は [refreshSessionMode] を呼んでモードを同期すること。
     */
    fun setDebugBuild(value: Boolean) {
        isDebugBuild = value
    }

    private var forceDevSeedUserId: Boolean = false

    /**
     * true のとき、[userId] / [requireUserId] は常に [DEV_SEED_USER_ID] を返す。
     */
    fun isForceDevSeedUserId(): Boolean = forceDevSeedUserId

    /**
     * 低レベル API: Seed 固定の有効/無効を直接設定する。
     * 通常は [setSessionMode] または [refreshSessionMode] を使うこと。
     */
    fun setForceDevSeedUserId(enabled: Boolean) {
        forceDevSeedUserId = enabled
    }

    private val store = KeyValueStore()

    private const val KEY_USER_ID = "user_id"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_DEBUG_ENV = "debug_environment"

    var userRepository: UserRepository? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    /**
     * 現在のセッションモード。
     * Release では常に [SessionMode.GUEST]、Debug では [SessionModeStore.mode] を返す。
     */
    val sessionMode: SessionMode
        get() = if (isDebugBuild) SessionModeStore.mode else SessionMode.GUEST

    /**
     * [SessionModeStore] の値と [isDebugBuild] に応じて [forceDevSeedUserId] を更新する。
     * Debug + Seed のときだけ true、それ以外は false。
     */
    fun refreshSessionMode() {
        forceDevSeedUserId = isDebugBuild && SessionModeStore.mode == SessionMode.SEED
        if (!forceDevSeedUserId) {
            // Seed 固定が外れた場合、古い Seed userId が KeyValueStore に残っていても
            // 次回の setSession で上書きされる。ここでは平文 token もクリアしておく。
            authToken = null
        }
    }

    /**
     * Debug ビルド用: [SessionMode] を切り替える。
     * Release では無視する。
     */
    fun setSessionMode(mode: SessionMode) {
        if (!isDebugBuild) return
        SessionModeStore.mode = mode
        refreshSessionMode()
    }

    suspend fun initializeFromAuth() {
        val user = userRepository?.getOrCreateAuthUser()
        if (user != null) {
            _currentUser.value = user
            setSession(user.id)
        }
    }

    /** 現在ログイン中のユーザー ID */
    var userId: String?
        get() = if (forceDevSeedUserId) DEV_SEED_USER_ID else store.getString(KEY_USER_ID)
        private set(value) {
            if (value != null) store.putString(KEY_USER_ID, value)
            else store.remove(KEY_USER_ID)
        }

    /**
     * 認証トークン (JWT)。
     *
     * Guest モードでは [SecureSessionStore] から復元後にメモリに保持し、
     * 平文 KeyValueStore には保存しない。
     */
    var authToken: String? = null
        private set

    /** ユーザーセッションを設定 */
    fun setSession(userId: String, token: String? = null) {
        if (forceDevSeedUserId && userId != DEV_SEED_USER_ID) {
            println(
                "[LevelUpStudy] dev seed mode: API 用 userId は $DEV_SEED_USER_ID に固定。createUser の $userId は保存しません。",
            )
            if (token != null) this.authToken = token
            return
        }
        this.userId = userId
        this.authToken = token
    }

    /** セッションをクリア（ログアウト時） */
    fun clear() {
        forceDevSeedUserId = false
        userId = null
        authToken = null
    }

    /** ユーザーIDを取得（未設定の場合は例外） */
    fun requireUserId(): String =
        userId ?: throw IllegalStateException("ユーザーIDが設定されていません。先にログインしてください。")

    /** ユーザーIDが保存されているか確認 */
    fun hasSession(): Boolean = userId != null

    /** デバッグビルド用: 保存された環境選択を取得。未保存の場合は null */
    fun getDebugEnvironment(): String? = store.getString(KEY_DEBUG_ENV)

    /** デバッグビルド用: 環境選択を保存 */
    fun setDebugEnvironment(env: String) {
        store.putString(KEY_DEBUG_ENV, env)
    }

    /** 平文で保存された既存 auth_token があれば削除する（移行用・ワンタイム） */
    fun clearLegacyPlainAuthToken() {
        store.remove(KEY_AUTH_TOKEN)
    }
}
