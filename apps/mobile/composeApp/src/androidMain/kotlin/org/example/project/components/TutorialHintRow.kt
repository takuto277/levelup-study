package org.example.project.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.project.data.local.TutorialProgressStore

@Composable
fun TutorialHintRow(
    topic: String,
    emoji: String,
    message: String,
) {
    val store = remember { TutorialProgressStore() }
    var visible by remember(topic) { mutableStateOf(!store.isCompleted(topic)) }
    if (!visible) return
    TutorialHintBanner(
        emoji = emoji,
        message = message,
        onDismiss = {
            store.markCompleted(topic)
            visible = false
        },
    )
}
