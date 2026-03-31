package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
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

    override suspend fun getCurrentUser(): User {
        // キャッシュがあればそれを返し、なければサーバーから取得
        return cachedUser ?: syncFromServer()
    }

    override suspend fun updateUser(displayName: String): User {
        val response = gateway.updateMe(UpdateUserRequest(displayName)).getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        return user
    }

    override suspend fun syncFromServer(): User {
        val response = gateway.getMe().getOrThrow()
        val user = response.toDomain()
        cachedUser = user
        return user
    }
}
