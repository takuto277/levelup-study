package org.example.project.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.core.storage.KeyValueStorage
import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.SyncStatus

class PendingStudyQueueStoreTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private class MemoryKeyValueStorage : KeyValueStorage {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
        override fun clear() { map.clear() }
    }

    private fun createItem(localId: String): PendingStudyCompletion = PendingStudyCompletion(
        localId = localId,
        category = null,
        startedAt = "2026-06-01T00:00:00Z",
        endedAt = "2026-06-01T00:25:00Z",
        durationSeconds = 1500,
        isCompleted = true,
        userCharacterId = null,
        defeatNormalCount = 0,
        defeatBossCount = 0,
        difficultyMultiplier = 1.0,
        isTrainingGround = false,
        syncStatus = SyncStatus.PENDING,
        retryCount = 0,
        lastError = null,
        lastAttemptAt = null
    )

    @Test
    fun `pending data is isolated per user`() {
        val kv = MemoryKeyValueStorage()
        val storeA = PendingStudyQueueStore(kv = kv, userIdProvider = { "user_a" })
        val storeB = PendingStudyQueueStore(kv = kv, userIdProvider = { "user_b" })

        storeA.append(createItem("item_a"))

        assertEquals(1, storeA.count())
        assertEquals(0, storeB.count())
        assertTrue(storeA.readAll().any { it.localId == "item_a" })
        assertFalse(storeB.readAll().any { it.localId == "item_a" })
    }

    @Test
    fun `legacy v1 data is migrated to current user and removed`() {
        val kv = MemoryKeyValueStorage()
        val legacyItems = listOf(createItem("legacy_1"), createItem("legacy_2"))
        kv.putString(
            "pending_study_queue_v1",
            json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), legacyItems)
        )

        val store = PendingStudyQueueStore(kv = kv, userIdProvider = { "user_c" })

        val items = store.readAll()
        assertEquals(2, items.size)
        assertTrue(items.any { it.localId == "legacy_1" })
        assertTrue(items.any { it.localId == "legacy_2" })
        assertEquals(null, kv.getString("pending_study_queue_v1"))
        assertEquals("true", kv.getString("pending_study_queue_v1_migrated"))
    }

    @Test
    fun `legacy migration runs only once`() {
        val kv = MemoryKeyValueStorage()
        val legacyItems = listOf(createItem("legacy_1"))
        kv.putString(
            "pending_study_queue_v1",
            json.encodeToString(ListSerializer(PendingStudyCompletion.serializer()), legacyItems)
        )

        val store1 = PendingStudyQueueStore(kv = kv, userIdProvider = { "user_d" })
        assertEquals(1, store1.count())

        // 2回目のインスタンス化でも重複移行しない
        val store2 = PendingStudyQueueStore(kv = kv, userIdProvider = { "user_d" })
        assertEquals(1, store2.count())
    }
}
