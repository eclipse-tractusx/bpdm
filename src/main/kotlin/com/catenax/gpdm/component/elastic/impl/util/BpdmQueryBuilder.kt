package com.catenax.gpdm.component.elastic.impl.util

import com.catenax.gpdm.component.elastic.impl.doc.AddressDoc
import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.dto.request.AddressSearchRequest
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.index.query.NestedQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.stereotype.Component

@Component
class BpdmQueryBuilder {

    fun buildNestedQuery(fieldName: String, queryText: String?, withHitInfo: Boolean): NestedQueryBuilder {
        val innerQuery = if(queryText == null) QueryBuilders.matchAllQuery() else buildInnerShouldQuery("$fieldName.text", queryText)

        val nestedQuery =  QueryBuilders.nestedQuery(fieldName, innerQuery, ScoreMode.Avg)

        if(withHitInfo)
            nestedQuery.innerHit(InnerHitBuilder())

        return nestedQuery
    }

    fun buildInnerShouldQuery(fieldName: String, queryText: String): BoolQueryBuilder {
        val boolQuery = QueryBuilders.boolQuery()
        val shouldQuery = boolQuery.should()
        shouldQuery.add(QueryBuilders.matchPhraseQuery(fieldName, queryText).boost(5f))
        shouldQuery.add(QueryBuilders.matchQuery(fieldName, queryText).boost(3f).fuzziness(Fuzziness.ONE))
        shouldQuery.addAll(queryText.split(" ").map { QueryBuilders.prefixQuery(fieldName, it) })
        return boolQuery
    }

    fun toFieldTextPairs(bpSearch: BusinessPartnerSearchRequest): Collection<Pair<String, String>>{
        val bpParamPairs = mutableListOf(
            Pair(BusinessPartnerDoc::names.name, bpSearch.name),
            Pair(BusinessPartnerDoc::legalForm.name, bpSearch.legalForm),
            Pair(BusinessPartnerDoc::classifications.name, bpSearch.classification),
            Pair(BusinessPartnerDoc::status.name, bpSearch.status)
        )

        if(bpSearch.address != null)
            bpParamPairs += toFieldTextPairs(bpSearch.address!!)

        return bpParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    fun toFieldTextPairs(addressSearch: AddressSearchRequest): Collection<Pair<String, String?>>{
        return listOf(
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::localities.name}", addressSearch.locality),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::administrativeAreas.name}", addressSearch.administrativeArea),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postCodes.name}", addressSearch.postCode),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::premises.name}", addressSearch.premise),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}", addressSearch.postalDeliveryPoint),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::thoroughfares.name}", addressSearch.thoroughfare),
        )
    }

    fun toLowerCaseSearchRequest(searchRequest: BusinessPartnerSearchRequest): BusinessPartnerSearchRequest{
        return BusinessPartnerSearchRequest(
            searchRequest.name?.lowercase(),
            searchRequest.legalForm?.lowercase(),
            searchRequest.status?.lowercase(),
            if(searchRequest.address != null) toLowerCaseSearchRequest(searchRequest.address!!) else null,
            searchRequest.classification?.lowercase()
        )
    }

    fun toLowerCaseSearchRequest(searchRequest: AddressSearchRequest): AddressSearchRequest{
        return AddressSearchRequest(
            searchRequest.administrativeArea?.lowercase(),
            searchRequest.postCode?.lowercase(),
            searchRequest.locality?.lowercase(),
            searchRequest.thoroughfare?.lowercase(),
            searchRequest.premise?.lowercase(),
            searchRequest.postalDeliveryPoint?.lowercase()
        )
    }

}