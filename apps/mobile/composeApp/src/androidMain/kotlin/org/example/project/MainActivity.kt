package org.example.project

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.storage.initKeyValueStore
import org.example.project.di.initKoin
import org.example.project.di.setDevSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initKeyValueStore(this)
        initKoin()
        val useSeedUser = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        setDevSession(useSeedUser = useSeedUser)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}