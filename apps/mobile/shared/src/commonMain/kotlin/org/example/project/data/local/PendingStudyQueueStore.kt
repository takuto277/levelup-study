package org.example.project.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.core.storage.KeyValueStore
import org.example.project.domain.model.PendingStudyCompletion

/**
 * 未送信の勉強完了を端末ローカルに永続化（Key-Value・JSON 配列）。
 * 同期成功後に該当レコードを削除する。
 */
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

    fun append(item: PendingStudyCompletion) {
        val next = readAll() + item
        kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), next))
    }

    fun remove(localId: String) {
        val next = readAll().filterNot { it.localId == localId }
        if (next.isEmpty()) kv.remove(key)
        else kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), next))
    }

    fun replaceAll(items: List<PendingStudyCompletion>) {
        if (items.isEmpty()) kv.remove(key)
        else kv.putString(key, json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), items))
    }
}
