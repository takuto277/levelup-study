package org.example.project.domain.repository

import org.example.project.domain.model.User

/**
 * ユーザー情報リポジトリ
 * 通貨（石・ゴールド）、累計勉強時間等の管理
 */
interface UserRepository {

    /** 現在のユーザー情報を取得 */
    suspend fun getCurrentUser(): User

    /** ユーザー情報を更新（表示名等） */
    suspend fun updateUser(displayName: String): User

    /** サーバーから最新のユーザー情報を同期 */
    suspend fun syncFromServer(): User
}
