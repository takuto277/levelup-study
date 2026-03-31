package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.GachaPullRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.GachaGateway
import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResult
import org.example.project.domain.repository.GachaRepository

class GachaRepositoryImpl(
    private val gateway: GachaGateway
) : GachaRepository {

    override suspend fun getActiveBanners(): List<GachaBanner> {
        return gateway.getActiveBanners().getOrThrow()
            .banners.map { it.toDomain() }
    }

    override suspend fun pullGacha(bannerId: String, count: Int): List<GachaResult> {
        val request = GachaPullRequest(bannerId = bannerId, count = count)
        return gateway.pullGacha(request).getOrThrow()
            .results.map { it.toDomain() }
    }

    override suspend fun getGachaHistory(bannerId: String?, limit: Int): List<GachaResult> {
        TODO("ガチャ履歴 API は未実装です")
    }

    override suspend fun getPityCount(bannerId: String): Int {
        TODO("天井カウント API は未実装です")
    }
}
