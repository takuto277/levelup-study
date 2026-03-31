package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.EquipWeaponRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.WeaponGateway
import org.example.project.domain.model.MasterWeapon
import org.example.project.domain.model.UserWeapon
import org.example.project.domain.repository.WeaponRepository

class WeaponRepositoryImpl(
    private val gateway: WeaponGateway
) : WeaponRepository {

    /** マスタデータのメモリキャッシュ */
    private var masterCache: List<MasterWeapon>? = null

    override suspend fun getMasterWeapons(): List<MasterWeapon> {
        masterCache?.let { return it }
        val weapons = gateway.getMasterWeapons().getOrThrow()
            .weapons.map { it.toDomain() }
        masterCache = weapons
        return weapons
    }

    override suspend fun getUserWeapons(): List<UserWeapon> {
        return gateway.getUserWeapons().getOrThrow()
            .weapons.map { it.toDomain() }
    }

    override suspend fun getUserWeapon(id: String): UserWeapon? {
        return getUserWeapons().find { it.id == id }
    }

    override suspend fun levelUpWeapon(userWeaponId: String): UserWeapon {
        TODO("武器レベルアップ API は未実装です")
    }

    override suspend fun equipWeapon(userCharacterId: String, userWeaponId: String?) {
        gateway.equipWeapon(userCharacterId, EquipWeaponRequest(userWeaponId)).getOrThrow()
    }
}
