package org.eclipse.tractusx.bpdm.gate.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityWithReferencesDto
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toDto
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqRequestException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private const val BUSINESS_PARTNER_PATH = "/businesspartners"
private const val FETCH_BUSINESS_PARTNER_PATH = "$BUSINESS_PARTNER_PATH/fetch"

@Service
class LegalEntityService(
    private val webClient: WebClient,
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val cdqConfigProperties: CdqConfigProperties,
    private val objectMapper: ObjectMapper
) {

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityWithReferencesDto>) {
        val legalEntitiesCdq = legalEntities.map { cdqRequestMappingService.toCdqModel(it) }
        val upsertRequest =
            UpsertRequest(
                cdqConfigProperties.datasource,
                legalEntitiesCdq,
                listOf(UpsertRequest.CdqFeatures.UPSERT_BY_EXTERNAL_ID, UpsertRequest.CdqFeatures.API_ERROR_ON_FAILURES)
            )

        try {
            webClient
                .put()
                .uri(BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert business partners request failed.", e)
        }
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityWithReferencesDto {
        val fetchRequest = FetchRequest(cdqConfigProperties.datasource, externalId)

        val fetchResponse = try {
            webClient
                .post()
                .uri(FETCH_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<FetchResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Fetch business partners request failed.", e)
        }

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return fetchResponse.businessPartner!!.toDto()
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Legal Entity", externalId)
        }
    }

    fun getLegalEntities(limit: Int, startAfter: String?): PageStartAfterResponse<LegalEntityWithReferencesDto> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(BUSINESS_PARTNER_PATH)
                        .queryParam("limit", limit)
                        .queryParam("datasource", cdqConfigProperties.datasource)
                        .queryParam("featuresOn", "USE_NEXT_START_AFTER")
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    builder.build()
                }
                .retrieve()
                .bodyToMono<BusinessPartnerCollectionCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Get business partners request failed.", e)
        }

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = partnerCollection.values.map { it.toDto() }
        )
    }
}