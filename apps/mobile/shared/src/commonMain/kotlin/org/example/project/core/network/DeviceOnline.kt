package org.example.project.core.network

/**
 * 端末のネットワーク接続の目安（厳密な「サーバー到達」ではない）。
 * オフライン時は訓練場フロー・未送信セッションのローカル保存に利用する。
 */
expect fun isDeviceOnline(): Boolean
