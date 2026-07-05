package org.example.project.core.session

import org.example.project.core.storage.KeyValueStore

/**
 * Debug ビルドでの [SessionMode] 選択を永続化する。
 *
 * 値は平文でも問題ない（選択状態のみ。token は [SecureSessionStore] へ）。
 */
object SessionModeStore {

    private const val KEY_SESSION_MODE = "debug_session_mode"

    private val store = KeyValueStore()

    /** 現在のモード。未設定時は [SessionMode.SEED]（既存開発フロー維持） */
    var mode: SessionMode
        get() = SessionMode.resolve(store.getString(KEY_SESSION_MODE))
        set(value) {
            store.putString(KEY_SESSION_MODE, value.name.lowercase())
        }
}
