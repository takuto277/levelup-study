package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.GachaBannerListResponse
import org.example.project.data.remote.dto.GachaHistoryResponse
import org.example.project.data.remote.dto.GachaPullRequest
import org.example.project.data.remote.dto.GachaPullResponse
import org.example.project.data.remote.dto.PityCountResponse

/**
 * ガチャ API Gateway
 * バナー取得・ガチャ実行・履歴取得
 */
class GachaGateway(private val client: HttpClient) {

    /** GET /api/gacha/banners — 有効バナー一覧 */
    suspend fun getActiveBanners(): NetworkResult<GachaBannerListResponse> = runCatching {
        val response: GachaBannerListResponse = client.get(ApiRoutes.GACHA_BANNERS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ガチャバナーの取得に失敗しました")
    }

    /** POST /api/gacha/pull — ガチャ実行 */
    suspend fun pullGacha(request: GachaPullRequest): NetworkResult<GachaPullResponse> =
        runCatching {
            val response: GachaPullResponse = client.post(ApiRoutes.GACHA_PULL) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ガチャの実行に失敗しました")
        }

    /** GET /api/gacha/history — ガチャ履歴取得 */
    suspend fun getHistory(
        bannerId: String?,
        limit: Int
    ): NetworkResult<GachaHistoryResponse> = runCatching {
        val response: GachaHistoryResponse = client.get(ApiRoutes.GACHA_HISTORY) {
            bannerId?.let { parameter("banner_id", it) }
            parameter("limit", limit)
        }.body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ガチャ履歴の取得に失敗しました")
    }

    /** GET /api/gacha/history?banner_id={id}&count_only=true — 天井カウント */
    suspend fun getPityCount(bannerId: String): NetworkResult<PityCountResponse> = runCatching {
        val response: PityCountResponse = client.get(ApiRoutes.GACHA_HISTORY) {
            parameter("banner_id", bannerId)
            parameter("count_only", true)
        }.body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "天井カウントの取得に失敗しました")
    }
}
