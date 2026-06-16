package org.example.project.domain.reminder

import org.example.project.features.reminder.ReminderPermissionStatus

interface ReminderScheduler {
    suspend fun permissionStatus(): ReminderPermissionStatus
    suspend fun scheduleDaily(hour: Int, minute: Int)
    suspend fun cancelAll()
    suspend fun openSystemNotificationSettings()
}
