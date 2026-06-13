package org.example.project.data.local

import org.example.project.core.storage.KeyValueStore
import org.example.project.features.reminder.ReminderPermissionStatus

class ReminderSettingsStore(
    private val kv: KeyValueStore = KeyValueStore()
) {
    fun load(): ReminderSettings {
        val enabled = kv.getString("study_reminder_enabled")?.toBooleanStrictOrNull() ?: false
        val hour = kv.getString("study_reminder_hour")?.toIntOrNull() ?: 20
        val minute = kv.getString("study_reminder_minute")?.toIntOrNull() ?: 0
        val requested = kv.getString("study_reminder_permission_requested")?.toBooleanStrictOrNull() ?: false

        return ReminderSettings(
            enabled = enabled,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            permissionStatus = if (requested) ReminderPermissionStatus.DENIED else ReminderPermissionStatus.NOT_DETERMINED,
        )
    }

    fun saveEnabled(enabled: Boolean) {
        kv.putString("study_reminder_enabled", enabled.toString())
    }

    fun saveTime(hour: Int, minute: Int) {
        kv.putString("study_reminder_hour", hour.coerceIn(0, 23).toString())
        kv.putString("study_reminder_minute", minute.coerceIn(0, 59).toString())
    }

    fun markPermissionRequested() {
        kv.putString("study_reminder_permission_requested", "true")
    }
}
