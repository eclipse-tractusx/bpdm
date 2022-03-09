package com.catenax.gpdm.repository.elastic

import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CustomSearchRepository {

    fun findBySearchRequest(searchRequest: BusinessPartnerSearchRequest, pageable: Pageable): Page<BusinessPartnerDoc>

}