package com.catenax.gpdm.component.elastic.impl.repository

import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface BusinessPartnerDocRepository : ElasticsearchRepository<BusinessPartnerDoc, String> {
}