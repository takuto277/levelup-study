package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.PartyResponse
import org.example.project.data.remote.dto.PartySlotResponse
import org.example.project.data.remote.dto.RemovePartySlotRequest
import org.example.project.data.remote.dto.UpdatePartySlotRequest

/**
 * パーティ API Gateway
 * パーティ編成の取得・更新
 */
class PartyGateway(private val client: HttpClient) {

    /** GET /api/user/party — パーティ編成取得 */
    suspend fun getParty(): NetworkResult<PartyResponse> = runCatching {
        val response: PartyResponse = client.get(ApiRoutes.PARTY).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "パーティ情報の取得に失敗しました")
    }

    /** PUT /api/user/party/slot — スロット更新 */
    suspend fun updateSlot(request: UpdatePartySlotRequest): NetworkResult<PartySlotResponse> =
        runCatching {
            val response: PartySlotResponse = client.put(ApiRoutes.PARTY_SLOT) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "パーティスロットの更新に失敗しました")
        }

    /** DELETE /api/user/party/slot — スロットからキャラ除外 */
    suspend fun removeFromSlot(request: RemovePartySlotRequest): NetworkResult<Unit> =
        runCatching {
            client.delete(ApiRoutes.PARTY_SLOT) {
                setBody(request)
            }
            NetworkResult.Success(Unit)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "パーティスロットの解除に失敗しました")
        }
}
