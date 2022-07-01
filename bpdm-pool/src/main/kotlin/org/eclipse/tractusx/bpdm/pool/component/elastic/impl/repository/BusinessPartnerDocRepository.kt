package org.eclipse.tractusx.bpdm.pool.component.elastic.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.BusinessPartnerDoc
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * Standard Elasticsearch repository for accessing and querying the [BusinessPartnerDoc] index
 */
interface BusinessPartnerDocRepository : ElasticsearchRepository<BusinessPartnerDoc, String> {
}