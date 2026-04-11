package org.example.project.domain.repository

import org.example.project.domain.model.MasterStudyGenre

interface GenreRepository {
    suspend fun getGenres(): List<MasterStudyGenre>
    suspend fun createGenre(label: String, emoji: String, colorHex: String): MasterStudyGenre
    suspend fun deleteGenre(genreId: String)
}
