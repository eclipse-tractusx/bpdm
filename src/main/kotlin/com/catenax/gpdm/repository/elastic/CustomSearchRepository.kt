package com.catenax.gpdm.repository.elastic

import com.catenax.gpdm.dto.elastic.AddressDoc
import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.dto.request.AddressSearchRequest
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Repository

@Repository
class CustomSearchRepository(
    val template: ElasticsearchOperations
) {

    fun findBySearchRequest(searchRequest: BusinessPartnerSearchRequest, pageable: Pageable): Page<BusinessPartnerDoc>{
        val boolQuery = QueryBuilders.boolQuery()
        val mustQuery = boolQuery.must()

        toFieldTextPairs(searchRequest)
            .filter { it.second != null }
            .forEach{ (name, queryText) ->
                val innerBoolQuery = QueryBuilders.boolQuery().minimumShouldMatch(1)
                val innerShouldQuery = innerBoolQuery.should()

                innerShouldQuery.add(QueryBuilders.wildcardQuery(name, "*$queryText*").caseInsensitive(true))
                innerShouldQuery.add(QueryBuilders.matchQuery(name, queryText).fuzziness(Fuzziness.ONE))

                mustQuery.add( innerBoolQuery )
            }

        val query = NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withPageable(pageable)
            .build()

        val result = template.search(query, BusinessPartnerDoc::class.java)
        return PageImpl(result.searchHits.map { it.content }, pageable, result.totalHits)
    }

    private fun toFieldTextPairs(bpSearch: BusinessPartnerSearchRequest): Collection<Pair<String, String?>>{
        val bpParamPairs = listOf(
            Pair(BusinessPartnerDoc::names.name, bpSearch.name),
            Pair(BusinessPartnerDoc::legalForm.name, bpSearch.legalForm),
            Pair(BusinessPartnerDoc::classifications.name, bpSearch.classification),
            Pair(BusinessPartnerDoc::status.name, bpSearch.status)
        )

        return if(bpSearch.address != null) bpParamPairs.plus(toFieldTextPairs(bpSearch.address!!))
                else bpParamPairs
    }

    private fun toFieldTextPairs(addressSearch: AddressSearchRequest): Collection<Pair<String, String?>>{
        return listOf(
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::localities.name}", addressSearch.locality),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::administrativeAreas.name}", addressSearch.administrativeArea),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postCodes.name}", addressSearch.postCode),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::premises.name}", addressSearch.premise),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}", addressSearch.postalDeliveryPoint),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::thoroughfares.name}", addressSearch.thoroughfare),
        )
    }

}