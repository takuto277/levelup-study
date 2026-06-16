package org.example.project.features.reminder

data class ReminderUiState(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
    val permissionStatus: ReminderPermissionStatus = ReminderPermissionStatus.NOT_DETERMINED,
)
