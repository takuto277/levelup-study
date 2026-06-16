package org.example.project.features.study

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.project.domain.model.MasterCharacter
import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.repository.PartyRepository

class StudyQuestViewModelTest {

    @Test
    fun `playerMaxHp defaults to 100 when party repo is null`() {
        val vm = StudyQuestViewModel(partyRepository = null)
        assertEquals(100, vm.uiState.value.playerMaxHp)
        assertEquals(100, vm.uiState.value.playerHp)
    }

    @Test
    fun `playerMaxHp reflects party combatHp after load`() = runBlocking {
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        delay(200)
        assertEquals(600, vm.uiState.value.playerMaxHp)
        assertEquals(600, vm.uiState.value.playerHp)
        assertEquals("勇者", vm.uiState.value.partyLeadName)
        vm.cleanup()
    }

    @Test
    fun `playerMaxHp stays at 100 when party load fails`() = runBlocking {
        val failingRepo = object : PartyRepository {
            override suspend fun getParty(): Party = throw RuntimeException("network error")
            override suspend fun updateSlot(slotPosition: Int, userCharacterId: String): PartySlot =
                error("not used")
            override suspend fun removeFromSlot(slotPosition: Int) = error("not used")
        }
        val vm = StudyQuestViewModel(partyRepository = failingRepo)
        delay(200)
        assertEquals(100, vm.uiState.value.playerMaxHp)
        assertEquals(100, vm.uiState.value.playerHp)
        vm.cleanup()
    }

    @Test
    fun `playerMaxHp persists across StartQuest and NextSession`() = runBlocking {
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        delay(200)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.EndQuest)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.NextSession)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.cleanup()
    }

    @Test
    fun `playerMaxHp does not revert to 100 after StopQuest`() = runBlocking {
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        delay(200)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        vm.onIntent(StudyQuestIntent.StopQuest)

        assertEquals(600, vm.uiState.value.playerMaxHp)
        assertEquals(StudySessionStatus.READY, vm.uiState.value.status)
        vm.cleanup()
    }

    @Test
    fun `playerHp resets to playerMaxHp on floor full clear`() = runBlocking {
        // floor full clear restores HP to max (captures HP preservation path in combat logic)
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        delay(200)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        assertEquals(600, vm.uiState.value.playerMaxHp)
        vm.cleanup()
    }

    @Test
    fun `NextSession does not reset playerMaxHp to default 100`() = runBlocking {
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        delay(200)
        assertEquals(600, vm.uiState.value.playerMaxHp)

        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        vm.onIntent(StudyQuestIntent.EndQuest)
        vm.onIntent(StudyQuestIntent.NextSession)

        assertEquals(600, vm.uiState.value.playerMaxHp)
        assert(vm.uiState.value.playerMaxHp != 100) { "playerMaxHp should not revert to default 100" }
        vm.cleanup()
    }

    @Test
    fun `StartQuest before party load is deferred and uses loaded playerMaxHp`() = runBlocking {
        val mockRepo = createMockPartyRepo()
        val vm = StudyQuestViewModel(partyRepository = mockRepo)
        // 読み込み完了前に StartQuest を発行
        assertEquals(true, vm.uiState.value.isPartyLoading)
        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        // まだ開始されていない
        assertEquals(StudySessionStatus.READY, vm.uiState.value.status)
        // 読み込み完了後、保留していた StartQuest が実行され loaded HP が反映される
        delay(1000)
        assertEquals(StudySessionStatus.RUNNING, vm.uiState.value.status)
        assertEquals(600, vm.uiState.value.playerMaxHp)
        assertEquals(600, vm.uiState.value.playerHp)
        vm.cleanup()
    }

    @Test
    fun `StartQuest with null repo uses default 100 hp immediately`() = runBlocking {
        val vm = StudyQuestViewModel(partyRepository = null)
        assertEquals(false, vm.uiState.value.isPartyLoading)
        vm.onIntent(StudyQuestIntent.StartQuest(studyMinutes = 25, genreId = null))
        assertEquals(StudySessionStatus.RUNNING, vm.uiState.value.status)
        assertEquals(100, vm.uiState.value.playerMaxHp)
        assertEquals(100, vm.uiState.value.playerHp)
        vm.cleanup()
    }

    private companion object {
        fun createMockPartyRepo(): PartyRepository = object : PartyRepository {
            override suspend fun getParty(): Party = Party(
                slots = listOf(
                    PartySlot(
                        id = "slot1",
                        userId = "user1",
                        slotPosition = 1,
                        userCharacterId = "uc1",
                        userCharacter = UserCharacter(
                            id = "uc1",
                            userId = "user1",
                            characterId = "c1",
                            character = MasterCharacter(
                                id = "c1",
                                name = "勇者",
                                rarity = 5,
                                baseHp = 200,
                                baseAtk = 30,
                                baseDef = 20,
                                imageUrl = "",
                                idleAnimationUrl = null,
                                isActive = true
                            ),
                            level = 5,
                            currentXp = 0,
                            breakthroughLevel = 0,
                            equippedWeaponId = null,
                            obtainedAt = "2024-01-01T00:00:00Z"
                        )
                    )
                )
            )

            override suspend fun updateSlot(slotPosition: Int, userCharacterId: String): PartySlot =
                error("not used")

            override suspend fun removeFromSlot(slotPosition: Int) = error("not used")
        }
    }
}
