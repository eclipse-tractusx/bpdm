package com.catenax.gpdm.component.elastic.impl.repository

import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.component.elastic.impl.doc.TextDoc
import com.catenax.gpdm.component.elastic.impl.util.BpdmQueryBuilder
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.exception.BpdmConversionException
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.SearchHitsImpl
import org.springframework.data.elasticsearch.core.document.SearchDocument
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Repository

/**
 * Creates and executes Elasticsearch queries for querying [BusinessPartnerDoc] properties of type [TextDoc]
 */
@Repository
class TextDocSearchRepository(
    val template: ElasticsearchOperations,
    val bpdmQueryBuilder: BpdmQueryBuilder
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
     * Elasticsearch always returns the whole [BusinessPartnerDoc] as a result. Therefore, we extract the inner
     * [TextDoc] hits and create a custom [SearchHits] collection based on the extracted hits.
     *
     * Elasticsearch query structure:
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
    fun findByFieldAndTextAndFilters(field: SuggestionType,
                                     queryText: String?,
                                     filters: BusinessPartnerSearchRequest,
                                     pageable: Pageable): SearchHits<TextDoc>{


        val lowerCaseQueryText = queryText?.lowercase()
        val lowerCaseFilters = bpdmQueryBuilder.toLowerCaseSearchRequest(filters)

        val boolQuery = QueryBuilders.boolQuery()

        boolQuery
            .must(bpdmQueryBuilder.buildNestedQuery(field.docName, lowerCaseQueryText, true))
            .filter().addAll(bpdmQueryBuilder.toFieldTextPairs(lowerCaseFilters).map {(fieldName, queryText) -> bpdmQueryBuilder.buildNestedQuery(fieldName, queryText, false) })

        val query = NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withPageable(pageable)
            .withHighlightFields(HighlightBuilder.Field(field.docName))
            .build()

        val result = template.search(query, BusinessPartnerDoc::class.java)

        val innerResult = result.searchHits
            .flatMap { it.innerHits[field.docName] ?: emptyList() }
            .sortedByDescending { it.score }
            .take(pageable.pageSize)
            .map{ hit ->
                val textDoc = convertToTextDoc(hit.content)

                SearchHit(hit.index, hit.id, hit.routing, hit.score, hit.sortValues.toTypedArray(), hit.highlightFields,
                    hit.innerHits, hit.nestedMetaData, hit.explanation, hit.matchedQueries, textDoc)
        }

        return SearchHitsImpl(innerResult.size.toLong(),
            result.totalHitsRelation,
            result.maxScore,
            null,
            innerResult,
            null,
            null)

    }

    /**
     * Inner hit contents are of an unknown type and need to inferred. SpringData Elasticsearch will try to provide the original
     * [TextDoc] type but may fail to do so and create the more generic [SearchDocument] type for such contents.
     * Here we check for the types and try to convert the inner hit content to a [TextDoc], if needed.
     *
     * @throws BpdmConversionException if inner hit content can't be converted
     */
    private fun convertToTextDoc(content: Any): TextDoc {
        if(content is TextDoc)
            return content

        if(content is SearchDocument)
            return TextDoc(content[TextDoc::text.name].toString())

        throw BpdmConversionException(content.toString(), content::class, TextDoc::class)
    }
}