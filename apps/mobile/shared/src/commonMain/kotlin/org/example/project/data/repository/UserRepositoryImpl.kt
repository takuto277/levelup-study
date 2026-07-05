package org.example.project.data.repository

import kotlinx.serialization.json.Json
import org.example.project.core.network.NetworkResult
import org.example.project.core.network.getOrThrow
import org.example.project.core.session.UserSessionStore
import org.example.project.core.storage.KeyValueStore
import org.example.project.data.remote.dto.CreateUserRequest
import org.example.project.data.remote.dto.UpdateUserRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.UserGateway
import org.example.project.domain.model.User
import org.example.project.domain.repository.UserRepository

class UserRepositoryImpl(
    private val gateway: UserGateway
) : UserRepository {

    companion object {
        /**
         * true: [syncFromServer] のたびに GET /users 相当の呼び出し元を Xcode / Logcat に出す（調査用）。
         * 原因が分かったら false に戻すこと。
         */
        private const val DEBUG_LOG_SYNC_FROM_SERVER = false
        private const val DEBUG_STACK_FRAMES = 18
    }

    /** メモリキャッシュ（起動中の一時保持） */
    private var cachedUser: User? = null

    private val kv = KeyValueStore()
    private val json = Json { ignoreUnknownKeys = true }
    private val keyCachedUser = "cached_user_json_v1"

    private fun persistUserOffline(user: User) {
        try {
            kv.putString(keyCachedUser, json.encodeToString(User.serializer(), user))
        } catch (_: Exception) { }
    }

    private fun loadPersistedUser(): User? =
        try {
            val raw = kv.getString(keyCachedUser) ?: return null
            json.decodeFromString(User.serializer(), raw)
        } catch (_: Exception) {
            null
        }

    override suspend fun getOrCreateAuthUser(): User? {
        return when (val result = gateway.getOrCreateAuthUser()) {
            is NetworkResult.Success -> result.data.toDomain()
            else -> null
        }
    }

    override suspend fun createUser(displayName: String): User {
        val response = gateway.createUser(CreateUserRequest(displayName)).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        persistUserOffline(user)
        UserSessionStore.setSession(userId = user.id)
        return user
    }

    override suspend fun getCurrentUser(): User {
        cachedUser?.let { return it }
        return try {
            syncFromServer()
        } catch (_: Exception) {
            loadPersistedUser()
                ?: throw IllegalStateException("オフラインでユーザー情報がありません。一度オンラインで起動してください。")
        }
    }

    override suspend fun updateUser(displayName: String): User {
        val userId = UserSessionStore.requireUserId()
        val response = gateway.updateUser(userId, UpdateUserRequest(displayName = displayName)).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        persistUserOffline(user)
        return user
    }

    override suspend fun updateSelectedDungeon(dungeonId: String?): User {
        val userId = UserSessionStore.requireUserId()
        val response = gateway.updateUser(userId, UpdateUserRequest(selectedDungeonId = dungeonId ?: "")).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        persistUserOffline(user)
        return user
    }

    override suspend fun syncFromServer(): User {
        if (DEBUG_LOG_SYNC_FROM_SERVER) {
            debugLogSyncFromServer("syncFromServer: GET user (explicit or via getCurrentUser)")
        }
        val userId = UserSessionStore.requireUserId()
        val response = gateway.getUser(userId).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        persistUserOffline(user)
        return user
    }

    override fun updateCachedUser(user: User) {
        cachedUser = user
        persistUserOffline(user)
    }

    override fun clearCache() {
        cachedUser = null
        kv.remove(keyCachedUser)
    }

    private fun debugLogSyncFromServer(hint: String) {
        val stack = runCatching {
            Exception()
                .stackTraceToString()
                .lineSequence()
                .drop(1)
                .take(DEBUG_STACK_FRAMES)
                .joinToString("\n    ")
        }.getOrElse { e -> "(stack: ${e.message})" }
        println("🌱 [UserRepo] $hint\n    $stack")
    }
}
