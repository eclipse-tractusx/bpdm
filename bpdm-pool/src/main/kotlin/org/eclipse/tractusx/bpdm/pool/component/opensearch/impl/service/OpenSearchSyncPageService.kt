package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.OpenSearchBusinessPartnerDocRepository
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OpenSearchSyncPageService(
    val businessPartnerRepository: BusinessPartnerRepository,
    val businessPartnerDocRepository: OpenSearchBusinessPartnerDocRepository,
    val documentMappingService: OpenSearchDocumentMappingService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun exportPartnersToOpenSearch(fromTime: Instant, pageRequest: PageRequest): Page<BusinessPartnerDoc> {
        logger.debug { "Export page ${pageRequest.pageNumber}" }
        val partnersToExport = businessPartnerRepository.findByUpdatedAtAfter(fromTime, pageRequest)
        logger.debug { "Exporting ${partnersToExport.size} records" }
        val partnerDocs = partnersToExport.map { documentMappingService.toDocument(it) }.toList()
        businessPartnerDocRepository.saveAll(partnerDocs)
        return PageImpl(partnerDocs, partnersToExport.pageable, partnersToExport.totalElements)
    }
}