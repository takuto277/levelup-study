package org.example.project.features.settings

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import org.example.project.features.reminder.ReminderIntent
import org.example.project.features.reminder.ReminderViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.di.getSettingsViewModel
import org.example.project.features.home.HomeIntent
import org.example.project.features.home.HomeTheme
import org.example.project.features.home.HomeViewModel
import org.example.project.core.network.AppEnvironment

@Composable
fun SettingsScreenDialog(
    onDismiss: () -> Unit,
    homeViewModel: HomeViewModel,
    onShowOnboarding: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val vm = remember { getSettingsViewModel() }
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshFromPlatform()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = HomeTheme.CardWhite),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("設定", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("API ベース URL", style = MaterialTheme.typography.labelSmall, color = HomeTheme.TextSecondary)
                Text(state.apiBaseUrl.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isDebug) {
                    Text("デバッグ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HomeTheme.AccentIndigo)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ユーザー ID（API に送る値）", style = MaterialTheme.typography.labelSmall, color = HomeTheme.TextSecondary)
                    Text(state.displayedUserId.ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "シード固定: ${if (state.forceDevSeed) "ON" else "OFF"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HomeTheme.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("石: ${state.stones}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-100, -10, 10, 100).forEach { d ->
                            OutlinedButton(onClick = { vm.patchCurrenciesFromPlatform(d, 0) }) {
                                Text(if (d > 0) "+$d" else "$d")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ゴールド: ${state.gold}", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-500, -100, 100, 500).forEach { d ->
                            OutlinedButton(onClick = { vm.patchCurrenciesFromPlatform(0, d) }) {
                                Text(if (d > 0) "+$d" else "$d")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "POST /api/v1/debug/users/{id}/currencies は Go の DEV_MODE=true のときのみ有効です。",
                        style = MaterialTheme.typography.labelSmall,
                        color = HomeTheme.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("環境", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppEnvironment.entries.forEach { env ->
                            val selected = state.selectedEnvironment == env.name.lowercase()
                            OutlinedButton(
                                onClick = { vm.setEnvironmentFromPlatform(env.name.lowercase(), AppEnvironment.ANDROID_DEV_URL) },
                            ) {
                                Text(
                                    env.displayName,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                } else {
                    Text("デバッグメニューはデバッグビルドでのみ表示されます。", color = HomeTheme.TextSecondary)
                }

                state.toast?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (onShowOnboarding != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onShowOnboarding()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("オンボーディングを再表示")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 学習リマインダー
                ReminderSettingsSection()

                OutlinedButton(
                    onClick = {
                        org.example.project.data.local.TutorialProgressStore().resetAll()
                        org.example.project.components.TutorialResetObserver.notifyReset()
                        homeViewModel.onIntent(HomeIntent.Refresh)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("チュートリアルをリセット")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            vm.clearToastFromPlatform()
                            homeViewModel.onIntent(HomeIntent.Refresh)
                            onDismiss()
                        },
                    ) {
                        Text("閉じる")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSettingsSection() {
    val viewModel = remember { ReminderViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    Text("学習リマインダー", style = MaterialTheme.typography.titleSmall)

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("通知 ON/OFF", modifier = Modifier.weight(1f))
        Switch(
            checked = uiState.enabled,
            onCheckedChange = { viewModel.onIntent(ReminderIntent.SetEnabled(it)) }
        )
    }

    if (uiState.enabled) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("通知時刻", modifier = Modifier.weight(1f))
            var showPicker by remember { mutableStateOf(false) }
            TextButton(onClick = { showPicker = true }) {
                Text("%02d:%02d".format(uiState.hour, uiState.minute))
            }
            if (showPicker) {
                // simplified: hour/minute text fields
                var h by remember { mutableStateOf(uiState.hour.toString()) }
                var m by remember { mutableStateOf(uiState.minute.toString()) }
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text("通知時刻を設定") },
                    text = {
                        Row {
                            OutlinedTextField(
                                value = h,
                                onValueChange = { h = it },
                                modifier = Modifier.width(64.dp),
                                label = { Text("時") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = m,
                                onValueChange = { m = it },
                                modifier = Modifier.width(64.dp),
                                label = { Text("分") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.onIntent(
                                ReminderIntent.SetTime(
                                    h.toIntOrNull() ?: 20,
                                    m.toIntOrNull() ?: 0
                                )
                            )
                            showPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showPicker = false }) { Text("キャンセル") } }
                )
            }
        }
    }
}
