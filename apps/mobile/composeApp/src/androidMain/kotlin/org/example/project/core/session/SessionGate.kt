package org.example.project.core.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * [SessionManager] の初期化が完了するまでローディング UI を表示し、
 * 初期化に失敗した場合は再試行ボタンを表示するガード Composable。
 */
@Composable
fun SessionGate(
    sessionManager: SessionManager,
    content: @Composable () -> Unit,
) {
    val state by sessionManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    when (state) {
        is SessionState.Initializing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SessionState.Ready -> {
            content()
        }
        is SessionState.RecoverableError,
        is SessionState.ResetRequired -> {
            val message = when (val s = state) {
                is SessionState.RecoverableError -> s.reason.throwable?.message ?: "初期化に失敗しました"
                is SessionState.ResetRequired -> "セッションをリセットしてください"
                else -> ""
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { scope.launch { sessionManager.retry() } }) {
                    Text("セッション初期化に失敗しました。再試行\n$message")
                }
            }
        }
    }
}
