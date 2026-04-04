package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.GachaPullRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.GachaGateway
import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResult
import org.example.project.domain.repository.GachaRepository
import org.example.project.domain.repository.UserRepository

class GachaRepositoryImpl(
    private val gateway: GachaGateway,
    private val userRepository: UserRepository
) : GachaRepository {

    override suspend fun getActiveBanners(): List<GachaBanner> {
        return gateway.getActiveBanners().getOrThrow()
            .banners.map { it.toDomain() }
    }

    override suspend fun pullGacha(bannerId: String, count: Int): List<GachaResult> {
        val request = GachaPullRequest(bannerId = bannerId, count = count)
        val response = gateway.pullGacha(request).getOrThrow()
        response.updatedUser?.toDomain()?.let { user ->
            userRepository.updateCachedUser(user)
        }
        return response.results.map { it.toDomain() }
    }

    override suspend fun getGachaHistory(bannerId: String?, limit: Int): List<GachaResult> {
        return emptyList()
    }

    override suspend fun getPityCount(bannerId: String): Int {
        return 0
    }
}
