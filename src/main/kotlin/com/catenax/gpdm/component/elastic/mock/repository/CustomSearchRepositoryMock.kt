package com.catenax.gpdm.component.elastic.mock.repository

import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.repository.elastic.CustomSearchRepository
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.repository.entity.BusinessPartnerRepository
import com.catenax.gpdm.service.DocumentMappingService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CustomSearchRepositoryMock(
    val businessPartnerRepository: BusinessPartnerRepository,
    val documentMappingService: DocumentMappingService
): CustomSearchRepository {
    override fun findBySearchRequest(
        searchRequest: BusinessPartnerSearchRequest,
        pageable: Pageable
    ): Page<BusinessPartnerDoc> {
        return businessPartnerRepository.findAll(pageable).map { documentMappingService.toDocument(it) }
    }


}