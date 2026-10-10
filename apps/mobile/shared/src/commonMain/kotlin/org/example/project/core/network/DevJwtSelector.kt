package org.example.project.core.network

/**
 * デバッグビルド用の開発 JWT を環境に応じて切り替える。
 * ApiClient がリクエスト時に現在の JWT を参照する。
 */
object DevJwtSelector {

    /** 現在アクティブな dev JWT。リクエストごとに ApiClient が読み取る。 */
    var current: String = GENERATED_DEV_JWT

    /** 環境キー（dev/stg/prod）に基づいて JWT を切り替える */
    fun selectForEnvironment(envName: String) {
        current = when (envName.lowercase()) {
            "stg" -> GENERATED_STG_DEV_JWT
            // 本番は Guest 認証（Supabase）を使用するため開発用 JWT は使用しない
            "prod" -> ""
            else -> GENERATED_DEV_JWT
        }
    }
}
