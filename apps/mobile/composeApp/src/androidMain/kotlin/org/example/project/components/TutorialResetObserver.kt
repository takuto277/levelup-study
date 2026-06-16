package org.example.project.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object TutorialResetObserver {
    var revision by mutableIntStateOf(0)
        private set

    fun notifyReset() {
        revision++
    }
}
