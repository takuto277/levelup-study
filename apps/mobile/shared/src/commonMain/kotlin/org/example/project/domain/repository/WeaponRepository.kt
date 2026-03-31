package org.example.project.domain.repository

import org.example.project.domain.model.MasterWeapon
import org.example.project.domain.model.UserWeapon

/**
 * 武器リポジトリ
 * マスタデータ取得・ユーザー所持武器管理・装備
 */
interface WeaponRepository {

    /** 武器マスタ一覧を取得（ローカルキャッシュ優先） */
    suspend fun getMasterWeapons(): List<MasterWeapon>

    /** ユーザーの所持武器一覧を取得 */
    suspend fun getUserWeapons(): List<UserWeapon>

    /** 特定のユーザー武器を取得 */
    suspend fun getUserWeapon(id: String): UserWeapon?

    /** 武器をレベルアップ */
    suspend fun levelUpWeapon(userWeaponId: String): UserWeapon

    /** キャラクターに武器を装備（null で解除） */
    suspend fun equipWeapon(userCharacterId: String, userWeaponId: String?)
}
