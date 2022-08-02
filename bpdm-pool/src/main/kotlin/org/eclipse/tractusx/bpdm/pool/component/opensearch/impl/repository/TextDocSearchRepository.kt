package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.util.BpdmOpenSearchQueryBuilder
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.opensearch.action.search.SearchRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.SearchHits
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Creates and executes OpenSearch queries for querying [BusinessPartnerDoc] properties of type [TextDoc]
 */
@Repository
class TextDocSearchRepository(
    val restHighLevelClient: RestHighLevelClient,
    val bpdmQueryBuilder: BpdmOpenSearchQueryBuilder
) {

    /**
     * Find TextDoc property [field] values matching keywords in [queryText], in business partners filtered by [filters]
     *
     * [queryText] and [filters] are converted to lowercase representation in order to work correctly with the case-sensitive prefix query.
     *
     * Query semantic: Return [BusinessPartnerDoc] [field] values that either match [queryText] exactly, or contain words in [queryText]
     * or have matching prefixes of [queryText]. Only look in [field] values that belong to business partners that
     * match the [filters]: For every non-null filter set the corresponding business partner field value needs to
     * either match [queryText] exactly, or contain words in [queryText], or have matching prefixes of [queryText]
     *
     * Results are only ranked by the quality of matches from the [field], and not by the [filters].
     * Quality is also determined by the type of match: full phrase match > word match > prefix match.
     *
     * OpenSearch always returns the whole [BusinessPartnerDoc] as a result. Therefore, we extract the inner
     * [TextDoc] hits and create a custom [SearchHits] collection based on the extracted hits.
     *
     * OpenSearch query structure:
     *  {
     *      "query":{
     *          "bool": {
     *              "must": [
     *                  "nested": {
     *                      "path": [field] path
     *                      "query": {
     *                          "bool":{
     *                              "should": [
     *                                  {"match_phrase": [queryText] ...},
     *                                  {"match": [queryText] ...}
     *                                  {"prefix": ...}
     *                                  .
     *                                  . for every keyword in [queryText]
     *                                  .
     *                                  {"prefix": ...}
     *                              ]
     *                          }
     *                      }
     *                  }
     *              ],
     *              "filter":[
     *                  "nested": {
     *                      "path": path of [filters] field
     *                      "query": {
     *                          "bool":{
     *                              "should": [
     *                                  {"match_phrase": [filters] field query text ...},
     *                                  {"match": [filters] field query text ...}
     *                                  {"prefix": ...}
     *                                  .
     *                                  . for every keyword in [filters] field query text
     *                                  .
     *                                  {"prefix": ...}
     *                              ]
     *                          }
     *                      }
     *                  }
     *                  .
     *                  . for every non-null [filters] field
     *                  .
     *                  {"nested": ...}
     *              ]
     *          }
     *      }
     *  }
     *
     *
     */
    fun findByFieldAndTextAndFilters(
        field: SuggestionType,
        queryText: String?,
        filters: BusinessPartnerSearchRequest,
        pageable: Pageable
    ): SearchHits {


        val lowerCaseQueryText = queryText?.lowercase()
        val lowerCaseFilters = bpdmQueryBuilder.toLowerCaseSearchRequest(filters)

        val boolQuery = QueryBuilders.boolQuery()

        boolQuery
            .must(bpdmQueryBuilder.buildNestedQuery(field.docName, lowerCaseQueryText, true))
            .filter().addAll(
                bpdmQueryBuilder.toFieldTextPairs(lowerCaseFilters)
                    .map { (fieldName, queryText) -> bpdmQueryBuilder.buildNestedQuery(fieldName, queryText, false) })

        val searchRequest = SearchRequest()
        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder
            .query(boolQuery)
            .from(pageable.pageNumber * pageable.pageSize)
            .size(pageable.pageSize)
            .highlighter(HighlightBuilder().field(HighlightBuilder.Field(field.docName)))
        searchRequest.source(searchSourceBuilder)

        val searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT)

        val arrayOfSearchHits = searchResponse.hits
            .flatMap { it.innerHits[field.docName] ?: emptyList() }
            .sortedByDescending { it.score }
            .take(pageable.pageSize)
            .toTypedArray()

        return SearchHits(
            arrayOfSearchHits,
            org.apache.lucene.search.TotalHits(arrayOfSearchHits.size.toLong(), searchResponse.hits.totalHits!!.relation),
            searchResponse.hits.maxScore
        )
    }
}