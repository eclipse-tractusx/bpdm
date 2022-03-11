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
import com.catenax.gpdm.repository.BusinessPartnerRepository
import com.catenax.gpdm.service.toSearchDto
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@Primary
class SearchServiceImpl(
    val businessPartnerDocSearchRepository: BusinessPartnerDocSearchRepository,
    val textDocSearchRepository: TextDocSearchRepository,
    val businessPartnerRepository: BusinessPartnerRepository
): SearchService {


    override fun searchBusinessPartners(searchRequest: BusinessPartnerSearchRequest,
                               paginationRequest: PaginationRequest): PageResponse<BusinessPartnerSearchResponse>{
        val searchResult = businessPartnerDocSearchRepository.findBySearchRequest(searchRequest, PageRequest.of(paginationRequest.page, paginationRequest.size))
        val bpnHitMap = searchResult.associateBy { it.content.bpn }

        val businessPartners = businessPartnerRepository.findDistinctByBpnIn(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(businessPartners.map { it.bpn }.toSet()).size

        val responseContent = businessPartners.map { it.toSearchDto(bpnHitMap[it.bpn]!!.score) }

        val totalHits = searchResult.totalHits - missingPartners
        val totalPages = totalHits.toInt()/paginationRequest.size

        return PageResponse(totalHits, totalPages, paginationRequest.page, responseContent.size, responseContent)

    }


    override fun getSuggestion(field: SuggestionType,
                               text: String?,
                               searchRequest: BusinessPartnerSearchRequest,
                               paginationRequest: PaginationRequest): PageResponse<SuggestionResponse>{

        val hits = textDocSearchRepository.findByFieldAndTextAndFilters(field, text, searchRequest,  PageRequest.of(paginationRequest.page, paginationRequest.size))

        return PageResponse(hits.totalHits, hits.totalHits.toInt() / paginationRequest.size, paginationRequest.page, hits.searchHits.size, hits.map { hit ->
            SuggestionResponse(hit.content.text, hit.score)
        }.toList())
    }



}