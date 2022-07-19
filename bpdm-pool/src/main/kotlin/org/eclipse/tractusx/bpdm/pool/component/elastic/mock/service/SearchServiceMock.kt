package org.eclipse.tractusx.bpdm.pool.component.elastic.mock.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.elastic.SearchService
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.eclipse.tractusx.bpdm.pool.service.toSearchDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Mock implementation of the [SearchService] in case the system is configured to run without Elasticsearch
 */
@Service
class SearchServiceMock(
    val businessPartnerRepository: BusinessPartnerRepository
) : SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Ignores [searchRequest] and returns an unfiltered result of business partners in the database,
     * adding a default relevancy score to each entry
     */
    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse> {
        val resultPage =
            businessPartnerRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        logger.info { "Mock search: Returning ${resultPage.size} business partners from database" }

        return resultPage.toDto(resultPage.content.map { it.toSearchDto(1f) })
    }

    /**
     * Ignores [field], [text] as well as [filters] and returns empty page of suggestions
     */
    override fun getSuggestion(
        field: SuggestionType,
        text: String?,
        filters: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        val emptyPage = Page.empty<SuggestionResponse>(PageRequest.of(paginationRequest.page, paginationRequest.size))

        logger.info { "Mock search: Returning no suggestions" }

        return emptyPage.toDto(emptyPage.content)
    }
}