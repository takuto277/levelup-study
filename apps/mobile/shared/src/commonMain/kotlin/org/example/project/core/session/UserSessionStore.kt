package org.example.project.core.session

import org.example.project.core.storage.KeyValueStore

/**
 * ユーザーセッション管理
 *
 * アプリ起動時にユーザーID を保持し、全 API コールで利用する。
 * expect/actual の KeyValueStore を使用して永続化する。
 */
object UserSessionStore {
    private val store = KeyValueStore()

    private const val KEY_USER_ID = "user_id"
    private const val KEY_AUTH_TOKEN = "auth_token"

    /** 現在ログイン中のユーザー ID */
    var userId: String?
        get() = store.getString(KEY_USER_ID)
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
        this.userId = userId
        this.authToken = token
    }

    /** セッションをクリア（ログアウト時） */
    fun clear() {
        userId = null
        authToken = null
    }

    /** ユーザーIDを取得（未設定の場合は例外） */
    fun requireUserId(): String =
        userId ?: throw IllegalStateException("ユーザーIDが設定されていません。先にログインしてください。")

    /** ユーザーIDが保存されているか確認 */
    fun hasSession(): Boolean = userId != null
}
