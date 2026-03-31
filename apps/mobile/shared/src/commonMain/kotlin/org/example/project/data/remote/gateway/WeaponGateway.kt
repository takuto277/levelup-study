package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.EquipWeaponRequest
import org.example.project.data.remote.dto.MasterWeaponListResponse
import org.example.project.data.remote.dto.UserWeaponListResponse
import org.example.project.data.remote.dto.UserWeaponResponse

/**
 * 武器 API Gateway
 * マスタデータ取得・ユーザー所持武器管理・装備
 */
class WeaponGateway(private val client: HttpClient) {

    /** GET /api/master/weapons — 武器マスタ一覧 */
    suspend fun getMasterWeapons(): NetworkResult<MasterWeaponListResponse> = runCatching {
        val response: MasterWeaponListResponse = client.get(ApiRoutes.MASTER_WEAPONS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "武器マスタの取得に失敗しました")
    }

    /** GET /api/user/weapons — ユーザー所持武器一覧 */
    suspend fun getUserWeapons(): NetworkResult<UserWeaponListResponse> = runCatching {
        val response: UserWeaponListResponse = client.get(ApiRoutes.USER_WEAPONS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "所持武器の取得に失敗しました")
    }

    /** POST /api/user/weapons/{id}/levelup — 武器レベルアップ */
    suspend fun levelUpWeapon(userWeaponId: String): NetworkResult<UserWeaponResponse> =
        runCatching {
            val response: UserWeaponResponse =
                client.post(ApiRoutes.userWeaponLevelUp(userWeaponId)).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "武器のレベルアップに失敗しました")
        }

    /** PUT /api/user/characters/{characterId}/equip — 武器装備 */
    suspend fun equipWeapon(
        userCharacterId: String,
        request: EquipWeaponRequest
    ): NetworkResult<Unit> = runCatching {
        client.put(ApiRoutes.equipWeapon(userCharacterId)) {
            setBody(request)
        }
        NetworkResult.Success(Unit)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "武器の装備に失敗しました")
    }
}
