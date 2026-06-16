package org.example.project.features.record

import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.StudySession
import org.example.project.domain.model.User
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.GenreRepository
import org.example.project.domain.repository.PartyRepository
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class RecordUseCase(
    private val userRepository: UserRepository,
    private val studyRepository: StudyRepository,
    private val genreRepository: GenreRepository,
    private val partyRepository: PartyRepository,
    private val characterRepository: CharacterRepository
) {
    data class RecordData(
        val user: User,
        val sessions: List<StudySession>,
        val genres: List<MasterStudyGenre>,
        val mainCharacter: UserCharacter?
    )

    suspend fun loadRecordData(): RecordData {
        val user = userRepository.getCurrentUser()
        // 履歴件数に切り詰められないよう、ページングですべてのセッションを取得する
        val sessions = mutableListOf<StudySession>()
        val pageSize = 200
        var offset = 0
        while (true) {
            val page = studyRepository.getSessionHistory(limit = pageSize, offset = offset)
            sessions.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        val genres = genreRepository.getGenres()
        val mainCharacter: UserCharacter? = try {
            val party = partyRepository.getParty()
            party.mainCharacter
                ?: party.slots.firstOrNull()?.userCharacterId?.let { id ->
                    characterRepository.getUserCharacter(id)
                }
        } catch (_: Exception) {
            null
        }
        return RecordData(user = user, sessions = sessions, genres = genres, mainCharacter = mainCharacter)
    }
}
