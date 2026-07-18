package org.example.project

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.example.project.core.session.SessionGate
import org.example.project.core.session.UserSessionStore
import org.example.project.core.storage.initKeyValueStore
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.AppEnvironment
import org.example.project.core.network.DevJwtSelector
import org.example.project.core.network.SupabaseConfigSelector
import org.example.project.di.getSessionManager
import org.example.project.di.initKoin
import org.example.project.di.initializeSessionMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initKeyValueStore(this)
        initKoin()
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        initializeSessionMode(isDebug)

        // DEBUG 環境を復元してから SessionManager を初期化する（Guest Supabase config を正しく紐付けるため）
        if (isDebug) {
            val savedEnv = UserSessionStore.getDebugEnvironment()
            val env = savedEnv?.let { s ->
                AppEnvironment.entries.firstOrNull { it.name.lowercase() == s }
            } ?: AppEnvironment.DEV
            val url = if (env == AppEnvironment.DEV) AppEnvironment.ANDROID_DEV_URL else env.url
            ApiRoutes.BASE_URL = url
            DevJwtSelector.selectForEnvironment(env.name.lowercase())
            SupabaseConfigSelector.selectForEnvironment(env.name.lowercase())
        }

        lifecycleScope.launch {
            getSessionManager().initialize(isDebug)
        }

        setContent {
            SessionGate(sessionManager = getSessionManager()) {
                App()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
