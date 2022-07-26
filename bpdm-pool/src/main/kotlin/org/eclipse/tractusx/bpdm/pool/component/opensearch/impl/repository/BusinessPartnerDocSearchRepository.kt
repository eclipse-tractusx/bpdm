package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.util.BpdmOpenSearchQueryBuilder
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.opensearch.action.search.SearchRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.SearchHits
import org.opensearch.search.builder.SearchSourceBuilder
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Creates and executes OpenSearch queries for querying [BusinessPartnerDoc] entries
 */
@Repository("OpenSearchBusinessPartnerDocSearchRepository")
class BusinessPartnerDocSearchRepository(
    val restHighLevelClient: RestHighLevelClient,
    val bpdmQueryBuilder: BpdmOpenSearchQueryBuilder
) {

    /**
     * Find [BusinessPartnerDoc] entries by [partnerSearchRequest] field query texts.
     *
     * Query semantic:  For every non-null [partnerSearchRequest] field query text: the corresponding [BusinessPartnerDoc] field value needs to
     * either contain the whole query text exactly, or contain some words of it or has matching prefixes.
     *
     * Quality of the result is determined by the type of match: full phrase match > word match > prefix match.
     *
     * OpenSearch query structure:
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
    fun findBySearchRequest(partnerSearchRequest: BusinessPartnerSearchRequest, pageable: Pageable): SearchHits {
        val lowerCaseSearchRequest = bpdmQueryBuilder.toLowerCaseSearchRequest(partnerSearchRequest)

        val boolQuery = QueryBuilders.boolQuery()
        val mustQuery = boolQuery.must()

        bpdmQueryBuilder.toFieldTextPairs(lowerCaseSearchRequest)
            .map { (fieldName, queryText) -> bpdmQueryBuilder.buildNestedQuery(fieldName, queryText, false) }
            .forEach { mustQuery.add(it) }

        val searchRequest = SearchRequest()
        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder
            .query(boolQuery)
            .from(pageable.pageNumber * pageable.pageSize)
            .size(pageable.pageSize)
        searchRequest.source(searchSourceBuilder)

        val searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT)

        return searchResponse.hits
    }
}