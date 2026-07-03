package org.example.project.data.remote.dto

import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.example.project.core.network.ApiClient

class DungeonDtoParseTest {

    @Test
    fun enemyCompositionEmptyArrayDeserializesAsJsonArray() {
        val json = """
        {
          "id": "s1",
          "dungeon_id": "d1",
          "stage_number": 1,
          "recommended_power": 100,
          "enemy_composition": [],
          "drop_table": [],
          "enemies": []
        }
        """.trimIndent()

        val stage = ApiClient.json.decodeFromString<DungeonStageResponse>(json)
        assertTrue(stage.enemyComposition is JsonElement)
        assertEquals("[]", stage.enemyComposition?.toString())

        val domain = stage.toDomain()
        assertEquals("[]", domain.enemyComposition)
    }

    @Test
    fun enemyCompositionStringValueDeserializesCorrectly() {
        val json = """
        {
          "id": "s1",
          "dungeon_id": "d1",
          "stage_number": 1,
          "recommended_power": 100,
          "enemy_composition": "slime:2",
          "drop_table": [],
          "enemies": []
        }
        """.trimIndent()

        val stage = ApiClient.json.decodeFromString<DungeonStageResponse>(json)
        assertEquals("slime:2", stage.enemyComposition?.toString())
        val domain = stage.toDomain()
        assertEquals("slime:2", domain.enemyComposition)
    }

    @Test
    fun rewardsNullDeserializesToEmptyList() {
        val json = """
        {
          "session_id": "00000000-0000-0000-0000-000000000001",
          "rewards": null,
          "updated_user": {
            "id": "u1",
            "display_name": "test",
            "total_study_seconds": 0,
            "stones": 0,
            "gold": 0,
            "level": 1,
            "current_xp": 0,
            "selected_dungeon_id": null,
            "created_at": "2026-01-01T00:00:00Z",
            "updated_at": "2026-01-01T00:00:00Z"
          }
        }
        """.trimIndent()

        val response = ApiClient.json.decodeFromString<StudyCompleteResponse>(json)
        assertEquals(true, response.rewards?.isEmpty())
        val domain = response.toDomain()
        assertEquals(true, domain.rewards.isEmpty())
    }

    @Test
    fun rewardsEmptyArrayDeserializesCorrectly() {
        val json = """
        {
          "session_id": "00000000-0000-0000-0000-000000000001",
          "rewards": [],
          "updated_user": {
            "id": "u1",
            "display_name": "test",
            "total_study_seconds": 0,
            "stones": 0,
            "gold": 0,
            "level": 1,
            "current_xp": 0,
            "selected_dungeon_id": null,
            "created_at": "2026-01-01T00:00:00Z",
            "updated_at": "2026-01-01T00:00:00Z"
          }
        }
        """.trimIndent()

        val response = ApiClient.json.decodeFromString<StudyCompleteResponse>(json)
        assertEquals(true, response.rewards?.isEmpty())
        val domain = response.toDomain()
        assertEquals(true, domain.rewards.isEmpty())
    }
}
