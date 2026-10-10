package org.example.project.core.network

/**
 * Guest Session 用の Supabase 設定を API 環境に応じて切り替える。
 *
 * ApiClient / GuestAuthService はリクエスト時・サインイン時に現在の設定を参照する。
 */
object SupabaseConfigSelector {

    /** 現在アクティブな Supabase URL */
    var currentUrl: String = GENERATED_PROD_SUPABASE_URL

    /** 現在アクティブな Supabase anon key */
    var currentAnonKey: String = GENERATED_PROD_SUPABASE_ANON_KEY

    /**
     * 現在のビルド種別に応じた初期化。
     * Release ビルドでは常に本番 Supabase を使用する。
     */
    fun initialize(isDebug: Boolean) {
        currentUrl = if (isDebug) GENERATED_SUPABASE_URL else GENERATED_PROD_SUPABASE_URL
        currentAnonKey = if (isDebug) GENERATED_SUPABASE_ANON_KEY else GENERATED_PROD_SUPABASE_ANON_KEY
    }

    /** 環境キー（dev/stg）に基づいて Supabase 設定を切り替える（デバッグビルド用） */
    fun selectForEnvironment(envName: String) {
        when (envName.lowercase()) {
            "stg" -> {
                currentUrl = GENERATED_STG_SUPABASE_URL
                currentAnonKey = GENERATED_STG_SUPABASE_ANON_KEY
            }
            else -> {
                currentUrl = GENERATED_SUPABASE_URL
                currentAnonKey = GENERATED_SUPABASE_ANON_KEY
            }
        }
    }
}
