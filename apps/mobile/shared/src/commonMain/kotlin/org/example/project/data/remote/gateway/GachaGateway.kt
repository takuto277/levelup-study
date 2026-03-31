package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.GachaBannerListResponse
import org.example.project.data.remote.dto.GachaPullRequest
import org.example.project.data.remote.dto.GachaPullResponse

/**
 * ガチャ API Gateway
 * バナー取得・ガチャ実行
 */
class GachaGateway(private val client: HttpClient) {

    /** GET /api/v1/master/gacha/banners — 有効バナー一覧 */
    suspend fun getActiveBanners(): NetworkResult<GachaBannerListResponse> = runCatching {
        val response: GachaBannerListResponse =
            client.get(ApiRoutes.MASTER_GACHA_BANNERS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ガチャバナーの取得に失敗しました")
    }

    /** POST /api/v1/users/{userId}/gacha/pull — ガチャ実行 */
    suspend fun pullGacha(request: GachaPullRequest): NetworkResult<GachaPullResponse> =
        runCatching {
            val userId = UserSessionStore.requireUserId()
            val response: GachaPullResponse =
                client.post(ApiRoutes.gachaPull(userId)) {
                    setBody(request)
                }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ガチャの実行に失敗しました")
        }
}
