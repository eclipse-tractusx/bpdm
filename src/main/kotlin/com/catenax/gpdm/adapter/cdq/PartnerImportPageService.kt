package com.catenax.gpdm.adapter.cdq

import com.catenax.gpdm.adapter.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.adapter.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.adapter.cdq.dto.*
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.service.BusinessPartnerService
import com.catenax.gpdm.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

open class PartnerImportPageService(
    private val webClient: WebClient,
    private val adapterProperties: CdqAdapterConfigProperties,
    private val cdqIdConfigProperties: CdqIdentifierConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: CdqRequestMappingService,
    private val businessPartnerService: BusinessPartnerService,
    private val cdqIdentifierType: TypeKeyNameUrlCdq,
    private val cdqIdentifierImportedStatus: TypeKeyNameCdq,
    private val cdqIssuer: TypeKeyNameUrlCdq,
) {

    @Transactional
    open fun import(modifiedAfter: OffsetDateTime, startAfter: String?): ImportResponse{
        val partnerCollection = webClient
            .get()
            .uri{ builder -> builder
                .path("/businesspartners")
                .queryParam("modifiedAfter", toModifiedAfterFormat(modifiedAfter))
                .queryParam("limit", adapterProperties.importLimit)
                .queryParam("datasource", adapterProperties.datasource)
                if(startAfter != null) builder.queryParam("startAfter", startAfter)
                builder.build()
            }
            .retrieve()
            .bodyToMono<BusinessPartnerCollectionCdq>()
            .block()!!

        val partners = partnerCollection.values
        val unknownPartners = filterUnknownPartners(partners)

        addNewMetadata(unknownPartners)
        return ImportResponse(partnerCollection.startAfter, addNewPartners(unknownPartners))
    }

    private fun addNewMetadata(partners: Collection<BusinessPartnerCdq>){
        partners
            .flatMap { it.identifiers.mapNotNull { id -> if(id.status?.technicalKey == null) null else id.status } }
            .plus(cdqIdentifierImportedStatus)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierStati(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierStatus(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if(id.type?.technicalKey == null) null else id.type  } }
            .plus(cdqIdentifierType)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierTypes(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierType(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if(id.issuingBody?.technicalKey == null) null else id.issuingBody } }
            .plus(cdqIssuer)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIssuingBodies(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map{ mappingService.toRequest(it) }
            .forEach { metadataService.createIssuingBody(it) }

        partners
            .filter { it.legalForm?.technicalKey != null }
            .map { it.legalForm!! to it }
            .associateBy { it.first.technicalKey }
            .minus(metadataService.getLegalForms(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it.first, it.second) }
            .forEach { metadataService.createLegalForm(it) }
    }

    private fun addNewPartners(partnersCdq: Collection<BusinessPartnerCdq>): Collection<BusinessPartnerResponse>{
        return businessPartnerService.createPartners(partnersCdq.map { mappingService.toRequest(it) })
    }



    private fun filterUnknownPartners(partners: Collection<BusinessPartnerCdq>): Collection<BusinessPartnerCdq>{
        val partnerMap = partners
            .filter { it.identifiers.none{ id -> id.type?.technicalKey == "BPN" } }
            .associateBy { it.id }

        val knownCdqIds = businessPartnerService.findPartnersByIdentifier(cdqIdConfigProperties.typeKey, partnerMap.keys)
            .mapNotNull {  it.identifiers.find { id -> id.type.technicalKey == cdqIdConfigProperties.typeKey}?.value }
            .toSet()

        return partnerMap.minus(knownCdqIds).values
    }




    private fun toModifiedAfterFormat(dateTime: OffsetDateTime): String{
        return DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }



}