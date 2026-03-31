package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.MasterCharacterListResponse
import org.example.project.data.remote.dto.UserCharacterListResponse
import org.example.project.data.remote.dto.UserCharacterResponse

/**
 * キャラクター API Gateway
 * マスタデータ取得・ユーザー所持キャラ管理
 */
class CharacterGateway(private val client: HttpClient) {

    /** GET /api/master/characters — キャラマスタ一覧 */
    suspend fun getMasterCharacters(): NetworkResult<MasterCharacterListResponse> = runCatching {
        val response: MasterCharacterListResponse = client.get(ApiRoutes.MASTER_CHARACTERS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "キャラクターマスタの取得に失敗しました")
    }

    /** GET /api/user/characters — ユーザー所持キャラ一覧 */
    suspend fun getUserCharacters(): NetworkResult<UserCharacterListResponse> = runCatching {
        val response: UserCharacterListResponse = client.get(ApiRoutes.USER_CHARACTERS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "所持キャラクターの取得に失敗しました")
    }

    /** POST /api/user/characters/{id}/levelup — レベルアップ */
    suspend fun levelUpCharacter(userCharacterId: String): NetworkResult<UserCharacterResponse> =
        runCatching {
            val response: UserCharacterResponse =
                client.post(ApiRoutes.userCharacterLevelUp(userCharacterId)).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "レベルアップに失敗しました")
        }
}
