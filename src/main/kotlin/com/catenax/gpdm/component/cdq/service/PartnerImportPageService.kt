package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.*
import com.catenax.gpdm.dto.BusinessPartnerUpdateDto
import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.service.BusinessPartnerBuildService
import com.catenax.gpdm.service.BusinessPartnerFetchService
import com.catenax.gpdm.service.MetadataService
import com.catenax.gpdm.service.toDto
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class PartnerImportPageService(
    private val webClient: WebClient,
    private val adapterProperties: CdqAdapterConfigProperties,
    private val cdqIdConfigProperties: CdqIdentifierConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: CdqRequestMappingService,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
) {


    private val cdqIdentifierType = TypeKeyNameUrlCdq(cdqIdConfigProperties.typeKey, cdqIdConfigProperties.typeName, "")
    private val cdqIdentifierStatus = TypeKeyNameCdq(cdqIdConfigProperties.statusImportedKey, cdqIdConfigProperties.statusImportedName)
    private val cdqIssuer = TypeKeyNameUrlCdq(cdqIdConfigProperties.issuerKey, cdqIdConfigProperties.issuerName, "")


    @Transactional
    fun import(modifiedAfter: Instant, startAfter: String?): ImportResponsePage {
        val partnerCollection = webClient
            .get()
            .uri { builder ->
                builder
                    .path("/businesspartners")
                    .queryParam("modifiedAfter", toModifiedAfterFormat(modifiedAfter))
                    .queryParam("limit", adapterProperties.importLimit)
                    .queryParam("datasource", adapterProperties.datasource)
                    .queryParam("featuresOn", "USE_NEXT_START_AFTER")
                if (startAfter != null) builder.queryParam("startAfter", startAfter)
                builder.build()
            }
            .retrieve()
            .bodyToMono<BusinessPartnerCollectionCdq>()
            .block()!!


        addNewMetadata( partnerCollection.values)
        val (createRequests, updateRequests) = partitionCreateAndUpdateRequests(partnerCollection.values)
        val upsertedPartners = businessPartnerBuildService.upsertBusinessPartners(createRequests, updateRequests).map { it.toDto() }

        return ImportResponsePage(
            partnerCollection.total,
            partnerCollection.nextStartAfter,
            upsertedPartners
        )
    }

    private fun addNewMetadata(partners: Collection<BusinessPartnerCdq>){
        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.status?.technicalKey == null) null else id.status } }
            .plus(cdqIdentifierStatus)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierStati(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.getOrCreateIdentifierStatus(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if(id.type?.technicalKey == null) null else id.type  } }
            .plus(cdqIdentifierType)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierTypes(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.getOrCreateIdentifierType(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if(id.issuingBody?.technicalKey == null) null else id.issuingBody } }
            .plus(cdqIssuer)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIssuingBodies(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.getOrCreateIssuingBody(it) }

        partners
            .filter { it.legalForm?.technicalKey != null }
            .map { it.legalForm!! to it }
            .associateBy { it.first.technicalKey }
            .minus(metadataService.getLegalForms(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it.first, it.second) }
            .forEach { metadataService.getOrCreateLegalForm(it) }
    }


    private fun toModifiedAfterFormat(dateTime: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }

    private fun partitionCreateAndUpdateRequests(cdqPartners: Collection<BusinessPartnerCdq>): Pair<Collection<BusinessPartnerRequest>, Collection<BusinessPartnerUpdateDto>>{
        val partnersToUpdate = businessPartnerFetchService.fetchByIdentifierValues( cdqIdConfigProperties.typeKey,cdqPartners.map { it.id })
        val cdqIdToPartnerMap = partnersToUpdate.associateBy { it.identifiers.find { id -> id.type.technicalKey == cdqIdConfigProperties.typeKey}!!.value }
        val (knownPartners, unknownPartners) = cdqPartners.partition { cdqIdToPartnerMap.containsKey(it.id) }

        return Pair(
            unknownPartners.map { mappingService.toRequest(it) },
            knownPartners.map { BusinessPartnerUpdateDto(cdqIdToPartnerMap.getValue(it.id), mappingService.toRequest(it)) }
        )
    }


}