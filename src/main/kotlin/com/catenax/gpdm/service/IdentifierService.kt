package com.catenax.gpdm.service

import com.catenax.gpdm.dto.response.IdentifierResponse
import com.catenax.gpdm.entity.IdentifierStatus
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.IdentifierRepository
import com.catenax.gpdm.repository.IdentifierStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class IdentifierService(
    val identifierRepository: IdentifierRepository,
    val identifierStatusRepository: IdentifierStatusRepository
) {

    @Transactional
    fun updateIdentifiers(uuids: Collection<UUID>, statusKey: String): Collection<IdentifierResponse>{
        val status = identifierStatusRepository.findByTechnicalKey(statusKey) ?: throw BpdmNotFoundException(IdentifierStatus::class, statusKey)
        val identifiers = identifierRepository.findByUuidIn(uuids)

        identifiers.forEach{ it.status = status }

        return identifierRepository.saveAll(identifiers).map { it.toDto() }
    }
}