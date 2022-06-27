package com.catenax.gpdm.component.elastic.impl.util

import com.catenax.gpdm.component.elastic.impl.doc.AddressDoc
import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.dto.request.AddressPropertiesSearchRequest
import com.catenax.gpdm.dto.request.BusinessPartnerPropertiesSearchRequest
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.SitePropertiesSearchRequest
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.index.query.NestedQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.stereotype.Component

/**
 * Offers utility methods for building Elasticsearch queries
 */
@Component
class BpdmQueryBuilder {

    /**
     * Returns an Elasticsearch nested query object model for searching a [queryText] in the [BusinessPartnerDoc] [fieldName].
     * In case [queryText] is not null it is matched by phrase, word or prefix with the [fieldName] values.
     * Otherwise, the query just returns possible values for [fieldName].
     * [withHitInfo] the query result not only contains the hit [BusinessPartnerDoc] but also the exact [fieldName] values hit.
     */
    fun buildNestedQuery(fieldName: String, queryText: String?, withHitInfo: Boolean): NestedQueryBuilder {
        val innerQuery = if (queryText == null) QueryBuilders.matchAllQuery() else buildInnerShouldQuery(
            "$fieldName.text",
            queryText
        )

        val nestedQuery = QueryBuilders.nestedQuery(fieldName, innerQuery, ScoreMode.Avg)

        if (withHitInfo)
            nestedQuery.innerHit(InnerHitBuilder())

        return nestedQuery
    }

    /**
     * Returns an Elasticsearch boolean should query object model for searching a [queryText] in the [BusinessPartnerDoc] [fieldName].
     * [queryText] is not null it is matched by phrase, word or prefix with the [fieldName] values.
     */
    fun buildInnerShouldQuery(fieldName: String, queryText: String): BoolQueryBuilder {
        val boolQuery = QueryBuilders.boolQuery()
        val shouldQuery = boolQuery.should()
        shouldQuery.add(QueryBuilders.matchPhraseQuery(fieldName, queryText).boost(5f))
        shouldQuery.add(
            QueryBuilders.matchQuery(fieldName, queryText).boost(3f).fuzziness(Fuzziness.ONE).prefixLength(3)
        )
        shouldQuery.addAll(queryText.split(" ").filter { it.isNotBlank() }.map { QueryBuilders.prefixQuery(fieldName, it) })
        return boolQuery
    }

    /**
     * Converts a [searchRequest] into pairs of [BusinessPartnerDoc] field name to query text for that field.
     * Fields with no query text are omitted.
     */
    fun toFieldTextPairs(searchRequest: BusinessPartnerSearchRequest): Collection<Pair<String, String>> {
        return toFieldTextPairs(searchRequest.partnerProperties) + toFieldTextPairs(searchRequest.addressProperties) + toFieldTextPairs(searchRequest.siteProperties)
    }

    /**
     * Converts a [bpSearch] into pairs of [BusinessPartnerDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(bpSearch: BusinessPartnerPropertiesSearchRequest): Collection<Pair<String, String>> {
        val bpParamPairs = mutableListOf(
            Pair(BusinessPartnerDoc::names.name, bpSearch.name),
            Pair(BusinessPartnerDoc::legalForm.name, bpSearch.legalForm),
            Pair(BusinessPartnerDoc::classifications.name, bpSearch.classification),
            Pair(BusinessPartnerDoc::status.name, bpSearch.status)
        )

        return bpParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Converts a [addressSearch] into pairs of [BusinessPartnerDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(addressSearch: AddressPropertiesSearchRequest): Collection<Pair<String, String>> {
        val addressParamPairs = listOf(
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::localities.name}", addressSearch.locality),
            Pair(
                "${BusinessPartnerDoc::addresses.name}.${AddressDoc::administrativeAreas.name}",
                addressSearch.administrativeArea
            ),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postCodes.name}", addressSearch.postCode),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::premises.name}", addressSearch.premise),
            Pair(
                "${BusinessPartnerDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}",
                addressSearch.postalDeliveryPoint
            ),
            Pair("${BusinessPartnerDoc::addresses.name}.${AddressDoc::thoroughfares.name}", addressSearch.thoroughfare),
        )

        return addressParamPairs.filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Converts a [siteSearch] into pairs of [BusinessPartnerDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(siteSearch: SitePropertiesSearchRequest): Collection<Pair<String, String>> {
        val siteParamPairs = listOf(
            Pair(BusinessPartnerDoc::sites.name, siteSearch.siteName)
        )

        return siteParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: BusinessPartnerSearchRequest): BusinessPartnerSearchRequest {
        return BusinessPartnerSearchRequest(
            toLowerCaseSearchRequest(searchRequest.partnerProperties),
            toLowerCaseSearchRequest(searchRequest.addressProperties),
            toLowerCaseSearchRequest(searchRequest.siteProperties)
        )
    }

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: BusinessPartnerPropertiesSearchRequest): BusinessPartnerPropertiesSearchRequest {
        return BusinessPartnerPropertiesSearchRequest(
            searchRequest.name?.lowercase(),
            searchRequest.legalForm?.lowercase(),
            searchRequest.status?.lowercase(),
            searchRequest.classification?.lowercase()
        )
    }

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: AddressPropertiesSearchRequest): AddressPropertiesSearchRequest {
        return AddressPropertiesSearchRequest(
            searchRequest.administrativeArea?.lowercase(),
            searchRequest.postCode?.lowercase(),
            searchRequest.locality?.lowercase(),
            searchRequest.thoroughfare?.lowercase(),
            searchRequest.premise?.lowercase(),
            searchRequest.postalDeliveryPoint?.lowercase()
        )
    }

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: SitePropertiesSearchRequest): SitePropertiesSearchRequest {
        return SitePropertiesSearchRequest(
            searchRequest.siteName?.lowercase()
        )
    }
}