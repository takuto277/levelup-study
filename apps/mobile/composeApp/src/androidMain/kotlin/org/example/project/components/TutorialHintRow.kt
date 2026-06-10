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
    val completed = store.isCompleted(topic)
    var dismissed by remember(topic, completed) { mutableStateOf(false) }
    if (completed || dismissed) return
    TutorialHintBanner(
        emoji = emoji,
        message = message,
        onDismiss = {
            store.markCompleted(topic)
            dismissed = true
        },
    )
}
