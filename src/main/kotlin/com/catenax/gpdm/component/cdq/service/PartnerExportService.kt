package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.component.cdq.dto.ExportResponse
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.service.BusinessPartnerService
import com.catenax.gpdm.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PartnerExportService(
    private val partnerExportPageService: PartnerExportPageService,
    private val businessPartnerService: BusinessPartnerService,
    private val idProperties: CdqIdentifierConfigProperties,
    private val bpnConfigProperties: BpnConfigProperties,
    private val adapterProperties: CdqAdapterConfigProperties,
    private val metadataService: MetadataService
) {

    fun export(): ExportResponse {
        createSynchronizedStatusIfNotExists()

        val partnersToSync: Collection<BusinessPartnerResponse> =
            businessPartnerService.findPartnersByIdentifier(idProperties.typeKey, idProperties.statusImportedKey)
        val partnerChunks = partnersToSync.chunked(adapterProperties.exportPageSize)

        var exportedBpns: Collection<String> = emptyList()
        for (partnerChunk: Collection<BusinessPartnerResponse> in partnerChunks) {
            val exported: Collection<BusinessPartnerCdq> = partnerExportPageService.export(partnerChunk)
            exportedBpns = exportedBpns.plus(exported.map { extractBpn(it)!! })
        }
        return ExportResponse(exportedBpns.size, exportedBpns)
    }

    private fun createSynchronizedStatusIfNotExists() {
        metadataService.getIdentifierStati(Pageable.unpaged()).content.find { it.technicalKey == idProperties.statusSynchronizedKey }
            ?: metadataService.createIdentifierStatus(TypeKeyNameDto(idProperties.statusSynchronizedKey, idProperties.statusSynchronizedName))
    }

    private fun extractBpn(it: BusinessPartnerCdq): String? = it.identifiers.find { id -> id.type?.technicalKey == bpnConfigProperties.id }?.value
}