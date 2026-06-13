package org.example.project.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.core.storage.KeyValueStore
import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.SyncStatus

class PendingStudyQueueStore(
    private val kv: KeyValueStore = KeyValueStore()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key = "pending_study_queue_v1"

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
        val retries = old.retryCount + 1
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
