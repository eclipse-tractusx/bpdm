package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.SearchService
import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.component.elastic.impl.repository.BusinessPartnerDocSearchRepository
import com.catenax.gpdm.component.elastic.impl.repository.TextDocSearchRepository
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse
import com.catenax.gpdm.service.BusinessPartnerFetchService
import com.catenax.gpdm.service.toSearchDto
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Implements search functionality by using Elasticsearch repositories
 */
@Service
@Primary
class SearchServiceImpl(
    val businessPartnerDocSearchRepository: BusinessPartnerDocSearchRepository,
    val textDocSearchRepository: TextDocSearchRepository,
    val businessPartnerFetchService: BusinessPartnerFetchService
) : SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Uses the [searchRequest] to perform an Elasticsearch query for business partners.
     * The BPNs of found partners are used to query the whole business partner records from the database.
     * The records are supplied with relevancy scores of the search hits and returned as a paginated result.
     * In case BPNs found by Elasticsearch can not be found in the database, the [PageResponse] properties are
     * adapted accordingly from the Elasticsearch page information
     *
     */
    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse> {

        logger.debug { "Search index for business partners" }

        val searchResult = businessPartnerDocSearchRepository.findBySearchRequest(
            searchRequest,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Found ${searchResult.searchHits.size} business partners in Elasticsearch. (${searchResult.totalHits} in total)" }

        val bpnHitMap = searchResult.associateBy { it.content.bpn }

        val businessPartners = businessPartnerFetchService.fetchByBpns(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(businessPartners.map { it.bpn }.toSet())

        if (missingPartners.isNotEmpty())
            logger.warn { "Some BPNs could not be found in the database: ${missingPartners.joinToString()}" }

        val responseContent =
            businessPartners.map { it.toSearchDto(bpnHitMap[it.bpn]!!.score) }.sortedByDescending { it.score }

        val totalHits = searchResult.totalHits - missingPartners.size
        val totalPages = totalHits.toInt() / paginationRequest.size

        return PageResponse(totalHits, totalPages, paginationRequest.page, responseContent.size, responseContent)

    }


    /**
     * Query Elasticsearch for [field] values by [text] and [filters]
     *
     * The found values and their hit scores are converted to [SuggestionResponse] and returned as a paginated result.
     */
    override fun getSuggestion(
        field: SuggestionType,
        text: String?,
        filters: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {

        logger.debug { "Search index for suggestion type $field" }

        val hits = textDocSearchRepository.findByFieldAndTextAndFilters(
            field,
            text,
            filters,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Returning ${hits.searchHits.size} suggestions for $field. (${hits.totalHits} found in total)" }

        return PageResponse(
            hits.totalHits,
            hits.totalHits.toInt() / paginationRequest.size,
            paginationRequest.page,
            hits.searchHits.size,
            hits.map { hit ->
                SuggestionResponse(hit.content.text, hit.score)
            }.toList()
        )
    }


}