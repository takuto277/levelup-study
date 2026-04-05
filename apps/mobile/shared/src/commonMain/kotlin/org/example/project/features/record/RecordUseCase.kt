package org.example.project.features.record

import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.StudySession
import org.example.project.domain.model.User
import org.example.project.domain.repository.GenreRepository
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository

class RecordUseCase(
    private val userRepository: UserRepository,
    private val studyRepository: StudyRepository,
    private val genreRepository: GenreRepository
) {
    data class RecordData(
        val user: User,
        val sessions: List<StudySession>,
        val genres: List<MasterStudyGenre>
    )

    suspend fun loadRecordData(): RecordData {
        val user = userRepository.getCurrentUser()
        val sessions = studyRepository.getSessionHistory(limit = 200)
        val genres = genreRepository.getGenres()
        return RecordData(user = user, sessions = sessions, genres = genres)
    }
}
