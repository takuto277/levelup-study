package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.UpdatePartySlotRequest
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.PartyGateway
import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot
import org.example.project.domain.repository.PartyRepository

class PartyRepositoryImpl(
    private val gateway: PartyGateway
) : PartyRepository {

    override suspend fun getParty(): Party {
        return gateway.getParty().getOrThrow().toDomain()
    }

    override suspend fun updateSlot(slotPosition: Int, userCharacterId: String): PartySlot {
        val request = UpdatePartySlotRequest(
            slotPosition = slotPosition,
            userCharacterId = userCharacterId
        )
        return gateway.updateSlot(request).getOrThrow().toDomain()
    }

    override suspend fun removeFromSlot(slotPosition: Int) {
        gateway.removeFromSlot(slotPosition).getOrThrow()
    }
}
