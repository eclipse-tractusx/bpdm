package org.eclipse.tractusx.bpdm.pool.component.elastic

import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse

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
                      paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>
}