package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.*
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.service.BusinessPartnerService
import com.catenax.gpdm.service.IdentifierService
import com.catenax.gpdm.service.MetadataService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PartnerExportPageService(
    private val webClient: WebClient,
    private val businessPartnerService: BusinessPartnerService,
    private val identifierService: IdentifierService,
    private val idProperties: CdqIdentifierConfigProperties,
    private val bpnProperties: BpnConfigProperties,
    private val adapterProperties: CdqAdapterConfigProperties,
    private val metadataService: MetadataService,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun export(): Collection<BusinessPartnerCdq>{
        createSynchronizedStatusIfNotExists()

        val partnersToSynch = businessPartnerService.findPartnersByIdentifier(idProperties.typeKey, idProperties.statusImportedKey)
            .associateBy { it.identifiers.find { id -> id.type.technicalKey == idProperties.typeKey }!!.value }

        val partnerCollection = webClient
            .get()
            .uri{ builder -> builder
                .path("/businesspartners")
                .queryParam("businessPartnerId", partnersToSynch.keys.joinToString())
                builder.build()
            }
            .retrieve()
            .bodyToMono<BusinessPartnerCollectionCdq>()
            .block()!!

        val cdqPartners = partnerCollection.values

        val (known, unknown) = cdqPartners.partition { it.identifiers.any { id -> id.type?.technicalKey == "BPN" } }

        if(known.isNotEmpty()) updateKnownPartners(partnersToSynch, known)
        return if(unknown.isNotEmpty()) synchronizeUknownPartners(partnersToSynch, unknown) else emptyList()
    }

    private fun synchronizeUknownPartners(partnerMap: Map<String, BusinessPartnerResponse>, unknownPartners: Collection<BusinessPartnerCdq>)
    : Collection<BusinessPartnerCdq>{
        val bpnCdqPairs =  unknownPartners.map {  it to partnerMap[it.id]!!.bpn  }
        val partnersWithBpn = bpnCdqPairs.map { addBpnIdentifier(it.first, it.second) }
        val requestBody = UpsertRequest(adapterProperties.datasource, partnersWithBpn)

        webClient
            .put()
            .uri("/businesspartners")
            .bodyValue(objectMapper.writeValueAsString(requestBody))
            .retrieve()
            .bodyToMono<UpsertResponse>()
            .block()!!

        updateKnownPartners(partnerMap, partnersWithBpn)

        return partnersWithBpn
    }


    private fun updateKnownPartners(partnerMap: Map<String, BusinessPartnerResponse>, knownPartners: Collection<BusinessPartnerCdq>){
        val knownIds = knownPartners.map { it.id }
        val knownPartnerIds = knownIds
            .mapNotNull { partnerMap[it] }
            .mapNotNull { it.identifiers.find { id -> id.type.technicalKey == idProperties.typeKey } }

        identifierService.updateIdentifiers(knownPartnerIds.map { it.uuid }, idProperties.statusSynchronizedKey)
    }

    private fun addBpnIdentifier(partner: BusinessPartnerCdq, bpn: String): BusinessPartnerCdq{
        return partner.copy(record = null, identifiers = partner.identifiers.plus(
            IdentifierCdq(
                TypeKeyNameUrlCdq(bpnProperties.id, bpnProperties.name, ""),
                bpn,
                TypeKeyNameUrlCdq(bpnProperties.agencyKey, bpnProperties.agencyName, ""),
                TypeKeyNameCdq(idProperties.statusSynchronizedKey, idProperties.statusSynchronizedName)
            )
        ))
    }

    private fun createSynchronizedStatusIfNotExists(){
        metadataService.getIdentifierStati(Pageable.unpaged())
            .content
            .find { it.technicalKey == idProperties.statusSynchronizedKey }
            ?: metadataService.createIdentifierStatus(
                TypeKeyNameDto(idProperties.statusSynchronizedKey, idProperties.statusSynchronizedName))
    }

}