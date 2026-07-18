import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

val generateLevelupClientApiKey = tasks.register("generateLevelupClientApiKey") {
    notCompatibleWithConfigurationCache("Reads local.properties / env at execution time to emit Kotlin source")
    val outDir = layout.buildDirectory.dir("generated/sources/levelupApiKey/commonMain/kotlin")
    val localProperties = rootProject.layout.projectDirectory.file("local.properties")
    // local.properties を変更したのにタスクが UP-TO-DATE のままだと api.key / dev.jwt が反映されない
    inputs.file(localProperties).optional()
    inputs.property("LEVELUP_API_KEY", providers.environmentVariable("LEVELUP_API_KEY").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_DEV_JWT", providers.environmentVariable("LEVELUP_DEV_JWT").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_STG_DEV_JWT", providers.environmentVariable("LEVELUP_STG_DEV_JWT").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_SUPABASE_URL", providers.environmentVariable("LEVELUP_SUPABASE_URL").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_SUPABASE_ANON_KEY", providers.environmentVariable("LEVELUP_SUPABASE_ANON_KEY").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_STG_SUPABASE_URL", providers.environmentVariable("LEVELUP_STG_SUPABASE_URL").orElse(""))
        .optional(true)
    inputs.property("LEVELUP_STG_SUPABASE_ANON_KEY", providers.environmentVariable("LEVELUP_STG_SUPABASE_ANON_KEY").orElse(""))
        .optional(true)
    outputs.dir(outDir)
    doLast {
        fun normalizeApiKey(raw: String): String {
            var s = raw.trim().trimStart('\uFEFF')
            if (s.length >= 2) {
                val f = s.first()
                val l = s.last()
                if ((f == '"' && l == '"') || (f == '\'' && l == '\'')) {
                    s = s.substring(1, s.lastIndex)
                }
            }
            return s.trim()
        }

        val props = Properties()
        val lp = rootProject.projectDir.resolve("local.properties")
        if (lp.isFile) lp.reader().use { props.load(it) }
        val fromEnv = System.getenv("LEVELUP_API_KEY")?.trim().orEmpty()
        val apiKeyRaw = fromEnv.ifBlank {
            props.getProperty("api.key")?.trim().orEmpty().ifBlank {
                // backend/.env や Render の変数名に合わせて誤記が多いためフォールバック
                props.getProperty("API_KEY")?.trim().orEmpty()
            }
        }
        val apiKey = normalizeApiKey(apiKeyRaw)
        fun escapeKotlinStringLiteral(s: String): String = buildString(s.length + 8) {
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append('\\').append('$')
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(ch)
                }
            }
        }
        val escaped = escapeKotlinStringLiteral(apiKey)

        val devJwtFromEnv = System.getenv("LEVELUP_DEV_JWT")?.trim().orEmpty()
        val devJwtRaw = devJwtFromEnv.ifBlank {
            props.getProperty("dev.jwt")?.trim().orEmpty()
        }
        val devJwt = normalizeApiKey(devJwtRaw)
        val escapedJwt = escapeKotlinStringLiteral(devJwt)

        val stgDevJwtFromEnv = System.getenv("LEVELUP_STG_DEV_JWT")?.trim().orEmpty()
        val stgDevJwtRaw = stgDevJwtFromEnv.ifBlank {
            props.getProperty("stg.dev.jwt")?.trim().orEmpty()
        }
        val stgDevJwt = normalizeApiKey(stgDevJwtRaw)
        val escapedStgJwt = escapeKotlinStringLiteral(stgDevJwt)

        val supabaseUrlFromEnv = System.getenv("LEVELUP_SUPABASE_URL")?.trim().orEmpty()
        val supabaseUrlRaw = supabaseUrlFromEnv.ifBlank {
            props.getProperty("supabase.url")?.trim().orEmpty()
        }
        val supabaseUrl = normalizeApiKey(supabaseUrlRaw)
        val escapedSupabaseUrl = escapeKotlinStringLiteral(supabaseUrl)

        val supabaseAnonKeyFromEnv = System.getenv("LEVELUP_SUPABASE_ANON_KEY")?.trim().orEmpty()
        val supabaseAnonKeyRaw = supabaseAnonKeyFromEnv.ifBlank {
            props.getProperty("supabase.anon.key")?.trim().orEmpty()
        }
        val supabaseAnonKey = normalizeApiKey(supabaseAnonKeyRaw)
        val escapedSupabaseAnonKey = escapeKotlinStringLiteral(supabaseAnonKey)

        val stgSupabaseUrlFromEnv = System.getenv("LEVELUP_STG_SUPABASE_URL")?.trim().orEmpty()
        val stgSupabaseUrlRaw = stgSupabaseUrlFromEnv.ifBlank {
            props.getProperty("stg.supabase.url")?.trim().orEmpty()
        }
        val stgSupabaseUrl = normalizeApiKey(stgSupabaseUrlRaw)
        val escapedStgSupabaseUrl = escapeKotlinStringLiteral(stgSupabaseUrl)

        val stgSupabaseAnonKeyFromEnv = System.getenv("LEVELUP_STG_SUPABASE_ANON_KEY")?.trim().orEmpty()
        val stgSupabaseAnonKeyRaw = stgSupabaseAnonKeyFromEnv.ifBlank {
            props.getProperty("stg.supabase.anon.key")?.trim().orEmpty()
        }
        val stgSupabaseAnonKey = normalizeApiKey(stgSupabaseAnonKeyRaw)
        val escapedStgSupabaseAnonKey = escapeKotlinStringLiteral(stgSupabaseAnonKey)

        val file = outDir.get().asFile.resolve("org/example/project/core/network/GeneratedApiKey.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.example.project.core.network

            /**
             * Gradle タスク `generateLevelupClientApiKey` が生成（コミットしない）。
             *
             * [GENERATED_CLIENT_API_KEY]
             * 優先: 環境変数 `LEVELUP_API_KEY`、次に local.properties の `api.key`、なければ `API_KEY`。
             * バックエンドの `API_KEY` と同一にすること。
             *
             * [GENERATED_DEV_JWT]（任意・ローカル開発向け）
             * 本番 API（DEV_MODE=false）でユーザー系ルートを叩くとき、Supabase の access_token が無い場合のフォールバック。
             * 優先: 環境変数 `LEVELUP_DEV_JWT`、次に local.properties の `dev.jwt`（JWT 文字列のみ、Bearer プレフィックス不要）。
             * HS256 かつ `sub` がリクエストの userId（例: seed の 00000000-0000-0000-0000-000000000001）と一致し、
             * 署名鍵はサーバーの `JWT_SECRET`（= Supabase の JWT Secret）と同じもので署名された JWT を入れる。
             *
             * [GENERATED_STG_DEV_JWT]（任意・stg 環境用）
             * stg 環境切替時に使用。優先: 環境変数 `LEVELUP_STG_DEV_JWT`、次に local.properties の `stg.dev.jwt`。
             * 署名鍵は stg サーバーの `JWT_SECRET` と一致させること。
             *
             * [GENERATED_SUPABASE_URL] / [GENERATED_SUPABASE_ANON_KEY]
             * Guest Session（Supabase Anonymous Sign-In）用。dev / デフォルト環境用。
             * 優先: 環境変数 `LEVELUP_SUPABASE_URL` / `LEVELUP_SUPABASE_ANON_KEY`、
             * 次に local.properties の `supabase.url` / `supabase.anon.key`。
             *
             * [GENERATED_STG_SUPABASE_URL] / [GENERATED_STG_SUPABASE_ANON_KEY]
             * Guest Session（Supabase Anonymous Sign-In）用。stg 環境切替時に使用。
             * 優先: 環境変数 `LEVELUP_STG_SUPABASE_URL` / `LEVELUP_STG_SUPABASE_ANON_KEY`、
             * 次に local.properties の `stg.supabase.url` / `stg.supabase.anon.key`。
             */
            internal const val GENERATED_CLIENT_API_KEY: String = "$escaped"

            internal const val GENERATED_DEV_JWT: String = "$escapedJwt"

            internal const val GENERATED_STG_DEV_JWT: String = "$escapedStgJwt"

            internal const val GENERATED_SUPABASE_URL: String = "$escapedSupabaseUrl"

            internal const val GENERATED_SUPABASE_ANON_KEY: String = "$escapedSupabaseAnonKey"

            internal const val GENERATED_STG_SUPABASE_URL: String = "$escapedStgSupabaseUrl"

            internal const val GENERATED_STG_SUPABASE_ANON_KEY: String = "$escapedStgSupabaseAnonKey"
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        named("commonMain") {
            kotlin.srcDir(layout.buildDirectory.dir("generated/sources/levelupApiKey/commonMain/kotlin"))
            dependencies {
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // DateTime
                implementation(libs.kotlinx.datetime)

                // Ktor (HTTP Client)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                // DI
                implementation(libs.koin.core)

                // ViewModel
                implementation(libs.androidx.lifecycle.viewmodel)

                // Supabase Auth
                implementation(libs.supabase.auth)
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                dependsOn("generateLevelupClientApiKey")
            }
        }
    }
}

android {
    namespace = "org.example.project.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
