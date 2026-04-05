package org.example.project.domain.repository

import org.example.project.domain.model.MasterStudyGenre

interface GenreRepository {
    suspend fun getGenres(): List<MasterStudyGenre>
}
