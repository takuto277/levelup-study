package org.example.project.features.home

import org.example.project.core.session.UserSessionStore
import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.User
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.DungeonRepository
import org.example.project.domain.repository.GenreRepository
import org.example.project.domain.repository.PartyRepository
import org.example.project.domain.repository.UserRepository

class HomeUseCase(
    private val userRepository: UserRepository,
    private val partyRepository: PartyRepository,
    private val characterRepository: CharacterRepository,
    private val genreRepository: GenreRepository,
    private val dungeonRepository: DungeonRepository
) {
    data class HomeData(
        val user: User,
        val mainCharacter: UserCharacter?,
        val genres: List<MasterStudyGenre>,
        val selectedDungeonName: String?,
        val selectedDungeonImageUrl: String?
    )

    suspend fun loadHomeData(): HomeData {
        val user = ensureUser()

        val mainCharacter: UserCharacter? = try {
            val party = partyRepository.getParty()
            party.mainCharacter
                ?: party.slots.firstOrNull()?.userCharacterId?.let { id ->
                    characterRepository.getUserCharacter(id)
                }
        } catch (_: Exception) {
            null
        }

        val genres = try {
            genreRepository.getGenres()
        } catch (_: Exception) {
            emptyList()
        }

        val (selectedDungeonName, selectedDungeonImageUrl) = try {
            val dungeonId = user.selectedDungeonId
            if (dungeonId != null) {
                val dungeons = dungeonRepository.getDungeons()
                val d = dungeons.find { it.id == dungeonId }
                Pair(d?.name, d?.imageUrl?.takeIf { it.isNotBlank() })
            } else Pair(null, null)
        } catch (_: Exception) {
            Pair(null, null)
        }

        return HomeData(
            user = user,
            mainCharacter = mainCharacter,
            genres = genres,
            selectedDungeonName = selectedDungeonName,
            selectedDungeonImageUrl = selectedDungeonImageUrl
        )
    }

    suspend fun createGenre(label: String, emoji: String, colorHex: String): MasterStudyGenre {
        return genreRepository.createGenre(label, emoji, colorHex)
    }

    suspend fun deleteGenre(genreId: String) {
        genreRepository.deleteGenre(genreId)
    }

    suspend fun refreshUser(): User {
        return userRepository.syncFromServer()
    }

    private suspend fun ensureUser(): User {
        return if (UserSessionStore.userId != null) {
            userRepository.getCurrentUser()
        } else {
            userRepository.createUser("冒険者")
        }
    }
}
