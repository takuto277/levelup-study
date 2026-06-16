package org.example.project.features.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.local.ReminderSettingsStore
import org.example.project.domain.reminder.ReminderScheduler

class ReminderViewModel(
    private val store: ReminderSettingsStore = ReminderSettingsStore(),
    private val scheduler: ReminderScheduler? = null,
) : ViewModel() {

    private val settings = store.load()

    private val _uiState = MutableStateFlow(
        ReminderUiState(
            enabled = settings.enabled,
            hour = settings.hour,
            minute = settings.minute,
            permissionStatus = settings.permissionStatus,
        )
    )
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    fun onIntent(intent: ReminderIntent) {
        when (intent) {
            is ReminderIntent.SetEnabled -> setEnabled(intent.enabled)
            is ReminderIntent.SetTime -> setTime(intent.hour, intent.minute)
            is ReminderIntent.RequestPermission -> requestPermission()
        }
    }

    private fun setEnabled(enabled: Boolean) {
        store.saveEnabled(enabled)
        _uiState.update { it.copy(enabled = enabled) }

        viewModelScope.launch {
            if (enabled) {
                scheduler?.scheduleDaily(
                    _uiState.value.hour,
                    _uiState.value.minute
                )
            } else {
                scheduler?.cancelAll()
            }
        }
    }

    private fun setTime(hour: Int, minute: Int) {
        store.saveTime(hour, minute)
        _uiState.update { it.copy(hour = hour, minute = minute) }

        viewModelScope.launch {
            if (_uiState.value.enabled) {
                scheduler?.cancelAll()
                scheduler?.scheduleDaily(hour, minute)
            }
        }
    }

    private fun requestPermission() {
        store.markPermissionRequested()
        viewModelScope.launch {
            val status = scheduler?.permissionStatus() ?: ReminderPermissionStatus.UNKNOWN
            _uiState.update { it.copy(permissionStatus = status) }
        }
    }
}
