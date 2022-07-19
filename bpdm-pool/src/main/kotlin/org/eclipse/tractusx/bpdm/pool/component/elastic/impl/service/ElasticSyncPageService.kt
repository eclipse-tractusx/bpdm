package org.eclipse.tractusx.bpdm.pool.component.elastic.impl.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.repository.BusinessPartnerDocRepository
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ElasticSyncPageService(
    val businessPartnerRepository: BusinessPartnerRepository,
    val businessPartnerDocRepository: BusinessPartnerDocRepository,
    val documentMappingService: DocumentMappingService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun exportPartnersToElastic(fromTime: Instant, pageRequest: PageRequest): Page<BusinessPartnerDoc> {
        logger.debug { "Export page ${pageRequest.pageNumber}" }
        val partnersToExport = businessPartnerRepository.findByUpdatedAtAfter(fromTime, pageRequest)
        logger.debug { "Exporting ${partnersToExport.size} records" }
        val createdDocs = businessPartnerDocRepository.saveAll(partnersToExport.map { documentMappingService.toDocument(it) }).toList()
        return PageImpl(createdDocs, partnersToExport.pageable, partnersToExport.totalElements)
    }
}