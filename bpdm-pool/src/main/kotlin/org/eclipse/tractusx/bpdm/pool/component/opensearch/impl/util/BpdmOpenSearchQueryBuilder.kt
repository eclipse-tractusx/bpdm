/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LegalEntityDoc
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
     * Returns an OpenSearch nested query object model for searching a [queryText] in the [LegalEntityDoc] [fieldName].
     * In case [queryText] is not null it is matched by phrase, word or prefix with the [fieldName] values.
     * Otherwise, the query just returns possible values for [fieldName].
     * [withHitInfo] the query result not only contains the hit [LegalEntityDoc] but also the exact [fieldName] values hit.
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
     * Returns an OpenSearch boolean should query object model for searching a [queryText] in the [LegalEntityDoc] [fieldName].
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
     * Converts a [searchRequest] into pairs of [LegalEntityDoc] field name to query text for that field.
     * Fields with no query text are omitted.
     */
    fun toFieldTextPairs(searchRequest: BusinessPartnerSearchRequest): Collection<Pair<String, String>> {
        return toFieldTextPairs(searchRequest.partnerProperties)
    }

    /**
     * Converts a [bpSearch] into pairs of [LegalEntityDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(bpSearch: LegalEntityPropertiesSearchRequest): Collection<Pair<String, String>> {
        val bpParamPairs = mutableListOf(
            Pair(LegalEntityDoc::legalName.name, bpSearch.legalName),
        )

        return bpParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Converts a [addressSearch] into pairs of [LegalEntityDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(addressSearch: AddressPropertiesSearchRequest): Collection<Pair<String, String>> {
        val addressParamPairs = listOf(
            Pair("${LegalEntityDoc::addresses.name}.${AddressDoc::localities.name}", addressSearch.locality),
            Pair(
                "${LegalEntityDoc::addresses.name}.${AddressDoc::administrativeAreas.name}",
                addressSearch.administrativeArea
            ),
            Pair("${LegalEntityDoc::addresses.name}.${AddressDoc::postCodes.name}", addressSearch.postCode),
            Pair("${LegalEntityDoc::addresses.name}.${AddressDoc::premises.name}", addressSearch.premise),
            Pair(
                "${LegalEntityDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}",
                addressSearch.postalDeliveryPoint
            ),
            Pair("${LegalEntityDoc::addresses.name}.${AddressDoc::thoroughfares.name}", addressSearch.thoroughfare),
        )

        return addressParamPairs.filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Converts a [siteSearch] into pairs of [LegalEntityDoc] field name to query text for that field.
     * @see toFieldTextPairs
     */
    fun toFieldTextPairs(siteSearch: SitePropertiesSearchRequest): Collection<Pair<String, String>> {
        val siteParamPairs = listOf(
            Pair(LegalEntityDoc::sites.name, siteSearch.siteName)
        )

        return siteParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }

    /**
     * Converts a [addressSearch] into pairs of [AddressPartnerDoc] field name to query text for that field.
     */
    fun toFieldTextPairs(addressSearch: AddressPartnerSearchRequest): Collection<Pair<String, String>> {
        val addressParamPairs = listOf(
            Pair(AddressPartnerDoc::name.name, addressSearch.name)

        )

        return addressParamPairs
            .filter { (_, query) -> query != null }
            .map { (fieldName, query) -> Pair(fieldName, query!!) }
    }



    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: BusinessPartnerSearchRequest): BusinessPartnerSearchRequest {
        return BusinessPartnerSearchRequest(
            toLowerCaseSearchRequest(searchRequest.partnerProperties)
        )
    }

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: LegalEntityPropertiesSearchRequest): LegalEntityPropertiesSearchRequest {
        return LegalEntityPropertiesSearchRequest(
            searchRequest.legalName?.lowercase()
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

    /**
     * Returns a lowercase representation of [searchRequest]
     */
    fun toLowerCaseSearchRequest(searchRequest: AddressPartnerSearchRequest): AddressPartnerSearchRequest {
        return AddressPartnerSearchRequest(
            searchRequest.name?.lowercase()
        )
    }
}