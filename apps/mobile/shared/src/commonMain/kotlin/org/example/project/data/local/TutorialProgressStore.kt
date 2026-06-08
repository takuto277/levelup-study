package org.example.project.data.local

import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.project.core.storage.KeyValueStore

class TutorialProgressStore(
    private val kv: KeyValueStore = KeyValueStore()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "tutorial_progress_v1"

    fun isCompleted(topic: String): Boolean = readAll().contains(topic)

    fun markCompleted(topic: String) {
        val next = readAll() + topic
        kv.putString(key, json.encodeToString(SetSerializer(String.serializer()), next))
    }

    fun resetAll() {
        kv.remove(key)
    }

    fun getFirstAvailable(topics: List<String>): String? {
        val completed = readAll()
        return topics.firstOrNull { it !in completed }
    }

    private fun readAll(): Set<String> {
        val raw = kv.getString(key) ?: return emptySet()
        return try {
            json.decodeFromString(SetSerializer(String.serializer()), raw)
        } catch (_: Exception) {
            emptySet()
        }
    }
}

object TutorialTopics {
    const val HOME_START_STUDY = "home_start_study"
    const val STUDY_TIMER = "study_timer"
    const val STUDY_REWARD = "study_reward"
    const val QUEST_SELECT = "quest_select"
    const val GACHA_FIRST_PULL = "gacha_first_pull"
    const val PARTY_SETUP = "party_setup"
    const val RECORD_REVIEW = "record_review"
}
