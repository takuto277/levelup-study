package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.MasterCharacterListResponse
import org.example.project.data.remote.dto.UserCharacterListResponse

/**
 * キャラクター API Gateway
 * マスタデータ取得・ユーザー所持キャラ管理
 */
class CharacterGateway(private val client: HttpClient) {

    /** GET /api/v1/master/characters — キャラマスタ一覧 */
    suspend fun getMasterCharacters(): NetworkResult<MasterCharacterListResponse> = runCatching {
        val response: MasterCharacterListResponse = client.get(ApiRoutes.MASTER_CHARACTERS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "キャラクターマスタの取得に失敗しました")
    }

    /** GET /api/v1/users/{userId}/characters — ユーザー所持キャラ一覧 */
    suspend fun getUserCharacters(): NetworkResult<UserCharacterListResponse> = runCatching {
        val userId = UserSessionStore.requireUserId()
        val response: UserCharacterListResponse =
            client.get(ApiRoutes.userCharacters(userId)).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "所持キャラクターの取得に失敗しました")
    }
}
