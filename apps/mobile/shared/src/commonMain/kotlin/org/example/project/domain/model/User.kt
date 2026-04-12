package org.example.project.domain.model

import kotlinx.serialization.Serializable

/**
 * ユーザー基本情報
 * Source of Truth: サーバー（Go backend model.User に対応）
 */
@Serializable
data class User(
    val id: String,
    val displayName: String,
    val level: Int = 1,
    val currentXp: Int = 0,
    val totalStudySeconds: Long,
    val stones: Int,
    val gold: Int,
    val selectedDungeonId: String? = null,
    val createdAt: String,
    val updatedAt: String
)
