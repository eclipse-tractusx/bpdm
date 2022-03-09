package com.catenax.gpdm.repository.elastic

import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface BusinessPartnerDocRepository : ElasticsearchRepository<BusinessPartnerDoc, String> {
}