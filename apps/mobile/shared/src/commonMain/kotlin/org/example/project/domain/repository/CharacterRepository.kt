package org.example.project.domain.repository

import org.example.project.domain.model.MasterCharacter
import org.example.project.domain.model.UserCharacter

/**
 * キャラクターリポジトリ
 * マスタデータ取得・ユーザー所持キャラ管理
 */
interface CharacterRepository {

    /** キャラクターマスタ一覧を取得（ローカルキャッシュ優先） */
    suspend fun getMasterCharacters(): List<MasterCharacter>

    /** ユーザーの所持キャラクター一覧を取得 */
    suspend fun getUserCharacters(): List<UserCharacter>

    /** 特定のユーザーキャラクターを取得 */
    suspend fun getUserCharacter(id: String): UserCharacter?

    /** キャラクターをレベルアップ */
    suspend fun levelUpCharacter(userCharacterId: String): UserCharacter
}
