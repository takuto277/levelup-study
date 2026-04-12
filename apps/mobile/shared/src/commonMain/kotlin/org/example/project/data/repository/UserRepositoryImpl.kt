package org.example.project.data.repository

import kotlinx.serialization.json.Json
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
}
