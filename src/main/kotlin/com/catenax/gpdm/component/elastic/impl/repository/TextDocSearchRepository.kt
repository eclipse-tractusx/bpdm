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

@Repository
class TextDocSearchRepository(
    val template: ElasticsearchOperations,
    val bpdmQueryBuilder: BpdmQueryBuilder
) {
    fun findByFieldAndTextAndFilters(field: SuggestionType,
                                     queryText: String?,
                                     filters: BusinessPartnerSearchRequest,
                                     pageable: Pageable): SearchHits<TextDoc>{

        val boolQuery = QueryBuilders.boolQuery()

        boolQuery
            .must(bpdmQueryBuilder.buildNestedQuery(field.docName, queryText, true))
            .filter().addAll(bpdmQueryBuilder.toFieldTextPairs(filters).map {(fieldName, queryText) -> bpdmQueryBuilder.buildNestedQuery(fieldName, queryText, false) })

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

    private fun convertToTextDoc(content: Any): TextDoc {
        if(content is TextDoc)
            return content

        if(content is SearchDocument)
            return TextDoc(content[TextDoc::text.name].toString())

        throw BpdmConversionException(content.toString(), content::class, TextDoc::class)
    }
}