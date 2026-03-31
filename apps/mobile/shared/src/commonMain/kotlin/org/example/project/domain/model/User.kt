package org.example.project.domain.model

/**
 * ユーザー基本情報
 * Source of Truth: サーバー（Go backend model.User に対応）
 */
data class User(
    val id: String,
    val displayName: String,
    val totalStudySeconds: Long,
    val stones: Int,
    val gold: Int,
    val createdAt: String,
    val updatedAt: String
)
