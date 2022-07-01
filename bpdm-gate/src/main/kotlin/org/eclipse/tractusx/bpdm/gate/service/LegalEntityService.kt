package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class LegalEntityService(
    private val webClient: WebClient
) {

    fun upsertLegalEntities(legalEntityRequest: LegalEntityRequest) {

    }
}