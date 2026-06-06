package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.example.project.core.storage.KeyValueStore
import org.example.project.core.storage.isOnboardingDone
import org.example.project.core.storage.setOnboardingDone
import org.example.project.core.storage.resetOnboarding
import org.example.project.features.onboarding.OnboardingScreen
import org.example.project.features.home.HomeScreenView

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val kvStore = remember { KeyValueStore() }
            var showOnboarding by remember {
                mutableStateOf(!kvStore.isOnboardingDone())
            }

            if (showOnboarding) {
                OnboardingScreen(
                    onComplete = {
                        kvStore.setOnboardingDone()
                        showOnboarding = false
                    },
                )
            } else {
                HomeScreenView(
                    onOpenOnboarding = {
                        kvStore.resetOnboarding()
                        showOnboarding = true
                    },
                )
            }
        }
    }
}
