package com.catenax.gpdm.component.elastic.impl.repository

import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.component.elastic.impl.util.BpdmQueryBuilder
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Repository


@Repository
class BusinessPartnerDocSearchRepository(
    val template: ElasticsearchOperations,
    val bpdmQueryBuilder: BpdmQueryBuilder
){

    fun findBySearchRequest(searchRequest: BusinessPartnerSearchRequest, pageable: Pageable): SearchHits<BusinessPartnerDoc>{
        val lowerCaseSearchRequest = bpdmQueryBuilder.toLowerCaseSearchRequest(searchRequest)

        val boolQuery = QueryBuilders.boolQuery()
        val mustQuery = boolQuery.must()

        bpdmQueryBuilder.toFieldTextPairs(lowerCaseSearchRequest)
            .map { (fieldName, queryText) -> bpdmQueryBuilder.buildNestedQuery(fieldName, queryText, false) }
            .forEach{ mustQuery.add(it) }

        val query = NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withPageable(pageable)
            .build()

        return template.search(query, BusinessPartnerDoc::class.java)
    }
}