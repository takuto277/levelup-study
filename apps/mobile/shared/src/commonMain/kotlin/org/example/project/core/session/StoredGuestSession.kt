package org.example.project.core.session

import kotlinx.serialization.Serializable

/**
 * 安全なストレージに保存する Guest Session。
 *
 * access token / refresh token / 有効期限を 1 レコードとして原子的に保存し、
 * 環境ごとに分離する。
 */
@Serializable
 data class StoredGuestSession(
    val environment: String,
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)
