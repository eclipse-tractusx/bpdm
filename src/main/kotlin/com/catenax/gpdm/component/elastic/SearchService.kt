package com.catenax.gpdm.component.elastic

import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse

/**
 * Provides search functionality on the Catena-x data for the BPDM system
 */
interface SearchService {

    /**
     * Find business partners by matching their field values to [searchRequest] field query texts
     */
    fun searchBusinessPartners(searchRequest: BusinessPartnerSearchRequest,
                               paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse>

    /**
     * In business partners matching the [filters], find [field] values matching [text].
     */
    fun getSuggestion(field: SuggestionType,
                      text: String?,
                      filters: BusinessPartnerSearchRequest,
                      paginationRequest: PaginationRequest): PageResponse<SuggestionResponse>
}