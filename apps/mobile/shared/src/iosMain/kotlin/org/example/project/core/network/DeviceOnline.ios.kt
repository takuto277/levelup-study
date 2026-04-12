package org.example.project.core.network

/**
 * iOS では NWPathMonitor 等の本格実装が必要なため、現状は常に true。
 * オフライン時の未送信セッションは [StudyRepository] 側の送信失敗時にローカル保存される。
 */
actual fun isDeviceOnline(): Boolean = true
