package com.catenax.gpdm.component.elastic

import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse

interface SearchService {

    fun searchBusinessPartners(searchRequest: BusinessPartnerSearchRequest,
                               paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse>

    fun getSuggestion(field: SuggestionType,
                      text: String?,
                      searchRequest: BusinessPartnerSearchRequest,
                      paginationRequest: PaginationRequest): PageResponse<SuggestionResponse>
}