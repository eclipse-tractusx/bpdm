/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.util

import org.apache.lucene.search.join.ScoreMode
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SitePropertiesSearchRequest
import org.opensearch.common.unit.Fuzziness
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.InnerHitBuilder
import org.opensearch.index.query.NestedQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.springframework.stereotype.Component

/**
 * Offers utility methods for building OpenSearch queries
 */
@Component
class BpdmOpenSearchQueryBuilder {

    /**
     * Returns an OpenSearch nested query object model for searching a [queryText] in the [BusinessPartnerDoc] [fieldName].
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
     * Returns an OpenSearch boolean should query object model for searching a [queryText] in the [BusinessPartnerDoc] [fieldName].
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