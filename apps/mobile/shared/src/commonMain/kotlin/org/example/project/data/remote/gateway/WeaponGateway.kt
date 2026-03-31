package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.EquipWeaponRequest
import org.example.project.data.remote.dto.MasterWeaponListResponse
import org.example.project.data.remote.dto.UserWeaponListResponse

/**
 * 武器 API Gateway
 * マスタデータ取得・ユーザー所持武器管理・装備
 */
class WeaponGateway(private val client: HttpClient) {

    /** GET /api/v1/master/weapons — 武器マスタ一覧 */
    suspend fun getMasterWeapons(): NetworkResult<MasterWeaponListResponse> = runCatching {
        val response: MasterWeaponListResponse = client.get(ApiRoutes.MASTER_WEAPONS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "武器マスタの取得に失敗しました")
    }

    /** GET /api/v1/users/{userId}/weapons — ユーザー所持武器一覧 */
    suspend fun getUserWeapons(): NetworkResult<UserWeaponListResponse> = runCatching {
        val userId = UserSessionStore.requireUserId()
        val response: UserWeaponListResponse =
            client.get(ApiRoutes.userWeapons(userId)).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "所持武器の取得に失敗しました")
    }

    /** PUT /api/v1/users/{userId}/characters/{characterId}/equip — 武器装備 */
    suspend fun equipWeapon(
        userCharacterId: String,
        request: EquipWeaponRequest
    ): NetworkResult<Unit> = runCatching {
        val userId = UserSessionStore.requireUserId()
        client.put(ApiRoutes.equipWeapon(userId, userCharacterId)) {
            setBody(request)
        }
        NetworkResult.Success(Unit)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "武器の装備に失敗しました")
    }
}
