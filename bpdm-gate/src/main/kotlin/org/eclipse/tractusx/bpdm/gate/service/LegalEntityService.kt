package org.eclipse.tractusx.bpdm.gate.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.tractusx.bpdm.common.dto.cdq.CdqFeatures
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertRequest
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertResponse
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest
import org.eclipse.tractusx.bpdm.gate.exception.CdqRequestException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private const val BUSINESS_PARTNER_PATH = "/businesspartners"

@Service
class LegalEntityService(
    private val webClient: WebClient,
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val cdqConfigProperties: CdqConfigProperties,
    private val objectMapper: ObjectMapper
) {

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityRequest>) {
        val legalEntitiesCdq = legalEntities.map { cdqRequestMappingService.toCdqModel(it) }
        val upsertRequest =
            UpsertRequest(cdqConfigProperties.datasource, legalEntitiesCdq, listOf(CdqFeatures.UPSERT_BY_EXTERNAL_ID, CdqFeatures.API_ERROR_ON_FAILURES))

        try {
            webClient
                .put()
                .uri(BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert request failed.", e)
        }
    }
}