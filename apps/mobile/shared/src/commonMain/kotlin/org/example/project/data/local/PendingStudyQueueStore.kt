package org.example.project.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.core.storage.KeyValueStorage
import org.example.project.core.storage.KeyValueStore
import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.SyncStatus

class PendingStudyQueueStore(
    private val kv: KeyValueStorage = KeyValueStore(),
    private val userIdProvider: () -> String = { "" }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key: String get() = "pending_study_queue_v2_${userIdProvider()}"
    private val legacyKey = "pending_study_queue_v1"
    private val migrationFlagKey = "pending_study_queue_v1_migrated"

    /**
     * 旧キーからの移行を userId が確定してから一度だけ実行する。
     * init で即時移行すると Koin singleton 生成時に userId 未確定で空キーへ誤移行するため、
     * readAll() の初回呼び出し時に遅延実行する。
     */
    private fun ensureMigrated() {
        val userId = userIdProvider()
        if (userId.isEmpty()) return
        migrateLegacyData()
    }

    /**
     * 旧キー `pending_study_queue_v1` のデータを、現在のユーザーへ一度だけ移行する。
     * ユーザー切替後の誤同期を防ぐため、移行後は旧キーを削除する。
     */
    fun migrateLegacyData() {
        if (kv.getString(migrationFlagKey) == "true") return
        val legacyRaw = kv.getString(legacyKey) ?: run {
            kv.putString(migrationFlagKey, "true")
            return
        }
        val legacyItems = try {
            json.decodeFromString(ListSerializer(PendingStudyCompletion.serializer()), legacyRaw)
        } catch (_: Exception) {
            emptyList()
        }
        if (legacyItems.isNotEmpty()) {
            val current = readAll()
            val merged = (current + legacyItems).distinctBy { it.localId }
            replaceAll(merged)
        }
        kv.remove(legacyKey)
        kv.putString(migrationFlagKey, "true")
    }

    fun readAll(): List<PendingStudyCompletion> {
        val raw = kv.getString(key) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(PendingStudyCompletion.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun count(): Int = readAll().size

    fun countByStatus(status: String): Int = readAll().count { it.syncStatus == status }

    fun hasFailedItems(): Boolean = readAll().any { it.syncStatus == SyncStatus.FAILED }

    fun append(item: PendingStudyCompletion) {
        val next = readAll() + item
        kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), next))
    }

    fun remove(localId: String) {
        val next = readAll().filterNot { it.localId == localId }
        if (next.isEmpty()) kv.remove(key)
        else kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), next))
    }

    fun updateStatus(localId: String, status: String, error: String? = null) {
        val items = readAll().toMutableList()
        val idx = items.indexOfFirst { it.localId == localId }
        if (idx == -1) return
        val old = items[idx]
        val attemptAt = kotlinx.datetime.Clock.System.now().toString()
        val retries = if (status == SyncStatus.FAILED) old.retryCount + 1 else old.retryCount
        items[idx] = old.copy(
            syncStatus = status,
            retryCount = retries,
            lastError = error,
            lastAttemptAt = attemptAt,
        )
        replaceAll(items)
    }

    fun replaceAll(items: List<PendingStudyCompletion>) {
        if (items.isEmpty()) kv.remove(key)
        else kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), items))
    }
}
