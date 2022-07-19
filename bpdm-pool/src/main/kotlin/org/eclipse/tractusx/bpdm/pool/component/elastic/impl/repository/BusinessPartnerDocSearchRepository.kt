package org.eclipse.tractusx.bpdm.pool.component.elastic.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.util.BpdmQueryBuilder
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Repository

/**
 * Creates and executes Elasticsearch queries for querying [BusinessPartnerDoc] entries
 */
@Repository
class BusinessPartnerDocSearchRepository(
    val template: ElasticsearchOperations,
    val bpdmQueryBuilder: BpdmQueryBuilder
){

    /**
     * Find [BusinessPartnerDoc] entries by [searchRequest] field query texts.
     *
     * Query semantic:  For every non-null [searchRequest] field query text: the corresponding [BusinessPartnerDoc] field value needs to
     * either contain the whole query text exactly, or contain some words of it or has matching prefixes.
     *
     * Quality of the result is determined by the type of match: full phrase match > word match > prefix match.
     *
     * Elasticsearch query structure:
     *  {
     *      "query":{
     *          "bool": {
     *              "must":[
     *                  "nested": {
     *                      "path": path of [searchRequest] field
     *                      "query": {
     *                          "bool":{
     *                              "should": [
     *                                  {"match_phrase": [searchRequest] field query text ...},
     *                                  {"match": [searchRequest] field query text ...}
     *                                  {"prefix": ...}
     *                                  .
     *                                  . for every word in [searchRequest] field query text
     *                                  .
     *                                  {"prefix": ...}
     *                              ]
     *                          }
     *                      }
     *                  }
     *                  .
     *                  . for every non-null [searchRequest] field
     *                  .
     *                  {"nested": ...}
     *              ]
     *          }
     *      }
     *  }
     */
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