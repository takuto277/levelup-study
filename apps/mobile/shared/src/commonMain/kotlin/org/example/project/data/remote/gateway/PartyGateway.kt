package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.PartyResponse
import org.example.project.data.remote.dto.PartySlotResponse
import org.example.project.data.remote.dto.UpdatePartySlotRequest

/**
 * パーティ API Gateway
 * パーティ編成の取得・更新
 */
class PartyGateway(private val client: HttpClient) {

    /** GET /api/v1/users/{userId}/party — パーティ編成取得 */
    suspend fun getParty(): NetworkResult<PartyResponse> = runCatching {
        val userId = UserSessionStore.requireUserId()
        val response: PartyResponse = client.get(ApiRoutes.party(userId)).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "パーティ情報の取得に失敗しました")
    }

    /** PUT /api/v1/users/{userId}/party/{slot} — スロット更新 */
    suspend fun updateSlot(request: UpdatePartySlotRequest): NetworkResult<PartySlotResponse> =
        runCatching {
            val userId = UserSessionStore.requireUserId()
            val response: PartySlotResponse =
                client.put(ApiRoutes.partySlot(userId, request.slotPosition)) {
                    setBody(request)
                }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "パーティスロットの更新に失敗しました")
        }

    /** DELETE /api/v1/users/{userId}/party/{slot} — スロットからキャラ除外 */
    suspend fun removeFromSlot(slot: Int): NetworkResult<Unit> =
        runCatching {
            val userId = UserSessionStore.requireUserId()
            client.delete(ApiRoutes.partySlot(userId, slot))
            NetworkResult.Success(Unit)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "パーティスロットの解除に失敗しました")
        }
}
