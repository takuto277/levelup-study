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
 */
object UserSessionStore {

    /** db/seed.sql の user1 と同じ ID（DEV_MODE + シード開発用） */
    const val DEV_SEED_USER_ID: String = "00000000-0000-0000-0000-000000000001"

    private var forceDevSeedUserId: Boolean = false

    /**
     * true のとき、[userId] / [requireUserId] は常に [DEV_SEED_USER_ID] を返す。
     * createUser が別 UUID を保存しても API パスはシードユーザーに揃う（setDevSession(..., forceSeedUserId=true) からオン）。
     */
    fun setForceDevSeedUserId(enabled: Boolean) {
        forceDevSeedUserId = enabled
    }

    fun isForceDevSeedUserId(): Boolean = forceDevSeedUserId

    private val store = KeyValueStore()

    private const val KEY_USER_ID = "user_id"
    private const val KEY_AUTH_TOKEN = "auth_token"

    var userRepository: UserRepository? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

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

    /** 認証トークン (JWT) */
    var authToken: String?
        get() = store.getString(KEY_AUTH_TOKEN)
        private set(value) {
            if (value != null) store.putString(KEY_AUTH_TOKEN, value)
            else store.remove(KEY_AUTH_TOKEN)
        }

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
}
