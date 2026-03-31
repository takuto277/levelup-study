package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.core.session.UserSessionStore
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

    override suspend fun createUser(displayName: String): User {
        val response = gateway.createUser(CreateUserRequest(displayName)).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        UserSessionStore.setSession(userId = user.id)
        return user
    }

    override suspend fun getCurrentUser(): User {
        // キャッシュがあればそれを返し、なければサーバーから取得
        return cachedUser ?: syncFromServer()
    }

    override suspend fun updateUser(displayName: String): User {
        val userId = UserSessionStore.requireUserId()
        val response = gateway.updateUser(userId, UpdateUserRequest(displayName)).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        return user
    }

    override suspend fun syncFromServer(): User {
        val userId = UserSessionStore.requireUserId()
        val response = gateway.getUser(userId).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        return user
    }
}
