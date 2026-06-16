package org.example.project.features.reminder

sealed class ReminderIntent {
    data class SetEnabled(val enabled: Boolean) : ReminderIntent()
    data class SetTime(val hour: Int, val minute: Int) : ReminderIntent()
    data object RequestPermission : ReminderIntent()
}
