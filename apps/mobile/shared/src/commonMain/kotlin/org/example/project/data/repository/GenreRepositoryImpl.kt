package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.GenreGateway
import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.repository.GenreRepository

class GenreRepositoryImpl(
    private val gateway: GenreGateway
) : GenreRepository {

    private var cachedGenres: List<MasterStudyGenre>? = null

    override suspend fun getGenres(): List<MasterStudyGenre> {
        cachedGenres?.let { return it }
        val genres = gateway.listGenres().getOrThrow()
            .genres.map { it.toDomain() }
        cachedGenres = genres
        return genres
    }
}
