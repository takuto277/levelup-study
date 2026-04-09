package org.example.project.di

import org.example.project.core.network.ApiClient
import org.example.project.data.remote.gateway.CharacterGateway
import org.example.project.data.remote.gateway.DungeonGateway
import org.example.project.data.remote.gateway.GachaGateway
import org.example.project.data.remote.gateway.GenreGateway
import org.example.project.data.remote.gateway.PartyGateway
import org.example.project.data.remote.gateway.StudyGateway
import org.example.project.data.remote.gateway.UserGateway
import org.example.project.data.remote.gateway.WeaponGateway
import org.example.project.data.repository.CharacterRepositoryImpl
import org.example.project.data.repository.DungeonRepositoryImpl
import org.example.project.data.repository.GachaRepositoryImpl
import org.example.project.data.repository.GenreRepositoryImpl
import org.example.project.data.repository.PartyRepositoryImpl
import org.example.project.data.repository.StudyRepositoryImpl
import org.example.project.data.repository.UserRepositoryImpl
import org.example.project.data.repository.WeaponRepositoryImpl
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.DungeonRepository
import org.example.project.domain.repository.GachaRepository
import org.example.project.domain.repository.GenreRepository
import org.example.project.domain.repository.PartyRepository
import org.example.project.domain.repository.StudyRepository
import org.example.project.domain.repository.UserRepository
import org.example.project.domain.repository.WeaponRepository
import org.example.project.features.record.RecordUseCase
import org.example.project.features.record.RecordViewModel
import org.example.project.features.analytics.AnalyticsUseCase
import org.example.project.features.analytics.AnalyticsViewModel
import org.example.project.features.gacha.GachaUseCase
import org.example.project.features.gacha.GachaViewModel
import org.example.project.features.home.HomeUseCase
import org.example.project.features.home.HomeViewModel
import org.example.project.features.party.PartyUseCase
import org.example.project.features.party.PartyViewModel
import org.example.project.features.quest.QuestUseCase
import org.example.project.features.quest.QuestViewModel
import org.example.project.features.study.StudyQuestViewModel
import org.example.project.features.study.StudyUseCase
import org.example.project.features.study.StudyViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {

    // ── Network ─────────────────────────────────
    single { ApiClient.create() }

    // ── Gateway ─────────────────────────────────
    singleOf(::UserGateway)
    singleOf(::StudyGateway)
    singleOf(::CharacterGateway)
    singleOf(::WeaponGateway)
    singleOf(::PartyGateway)
    singleOf(::DungeonGateway)
    singleOf(::GachaGateway)
    singleOf(::GenreGateway)

    // ── Repository ──────────────────────────────
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    single<StudyRepository> { StudyRepositoryImpl(get(), get()) }
    singleOf(::CharacterRepositoryImpl) bind CharacterRepository::class
    singleOf(::WeaponRepositoryImpl) bind WeaponRepository::class
    singleOf(::PartyRepositoryImpl) bind PartyRepository::class
    singleOf(::DungeonRepositoryImpl) bind DungeonRepository::class
    single<GachaRepository> { GachaRepositoryImpl(get(), get()) }
    singleOf(::GenreRepositoryImpl) bind GenreRepository::class

    // ── UseCase ─────────────────────────────────
    factoryOf(::HomeUseCase)
    factoryOf(::QuestUseCase)
    factoryOf(::PartyUseCase)
    factoryOf(::GachaUseCase)
    factoryOf(::AnalyticsUseCase)
    factoryOf(::RecordUseCase)
    factoryOf(::StudyUseCase)

    // ── ViewModel ───────────────────────────────
    singleOf(::HomeViewModel)
    factoryOf(::QuestViewModel)
    factoryOf(::PartyViewModel)
    factoryOf(::GachaViewModel)
    factoryOf(::AnalyticsViewModel)
    factoryOf(::RecordViewModel)
    factoryOf(::StudyViewModel)
    factoryOf(::StudyQuestViewModel)
}
