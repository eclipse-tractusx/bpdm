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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import javax.persistence.EntityManager

@Service
class PartnerExportService(
    private val partnerExportPageService: PartnerExportPageService,
    private val businessPartnerService: BusinessPartnerService,
    private val idProperties: CdqIdentifierConfigProperties,
    private val bpnConfigProperties: BpnConfigProperties,
    private val adapterProperties: CdqAdapterConfigProperties,
    private val metadataService: MetadataService,
    private val entityManager: EntityManager
) {

    fun export(): ExportResponse {
        createSynchronizedStatusIfNotExists()

        var exportedBpns: Collection<String> = emptyList()
        do {
            // Always request first page of "unsynchronized partners".
            // Since the export logic itself changes the state of the exported partners to "synchronized", the first page always contains the next chunk for the export.
            val partnersToSync: Page<BusinessPartnerResponse> =
                businessPartnerService.findPartnersByIdentifier(
                    idProperties.typeKey,
                    idProperties.statusImportedKey,
                    PageRequest.of(0, adapterProperties.exportPageSize)
                )
            val exported: Collection<BusinessPartnerCdq> = partnerExportPageService.export(partnersToSync.toList())
            exportedBpns = exportedBpns.plus(exported.map { extractBpn(it)!! })

            // Clear session after each page to improve JPA performance
            entityManager.clear()
        } while (!partnersToSync.isLast)

        return ExportResponse(exportedBpns.size, exportedBpns)
    }

    fun clearBpns() {

        var startAfter: String? = null

        do {
            startAfter = partnerExportPageService.clearBpns(startAfter)
        } while (startAfter != null)

    }

    private fun createSynchronizedStatusIfNotExists() {
        metadataService.getIdentifierStati(Pageable.unpaged()).content.find { it.technicalKey == idProperties.statusSynchronizedKey }
            ?: metadataService.createIdentifierStatus(TypeKeyNameDto(idProperties.statusSynchronizedKey, idProperties.statusSynchronizedName))
    }

    private fun extractBpn(it: BusinessPartnerCdq): String? = it.identifiers.find { id -> id.type?.technicalKey == bpnConfigProperties.id }?.value
}