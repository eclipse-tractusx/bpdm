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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.AddressDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.LegalEntityDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.TextDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerMatchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.entity.AddressPartner
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.eclipse.tractusx.bpdm.pool.service.toBusinessPartnerMatchDto
import org.eclipse.tractusx.bpdm.pool.service.toMatchDto
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import kotlin.math.ceil

/**
 * Implements search functionality by using OpenSearch
 */
@Service
@Primary
class SearchServiceImpl(
    val legalEntityDocSearchRepository: LegalEntityDocSearchRepository,
    val addressDocSearchRepository: AddressDocSearchRepository,
    val legalEntityRepository: LegalEntityRepository,
    val addressPartnerRepository: AddressPartnerRepository,
    val addressService: AddressService,
    val textDocSearchRepository: TextDocSearchRepository,
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val objectMapper: ObjectMapper
) : SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Uses the [searchRequest] to perform an OpenSearch query for business partners.
     * The BPNs of found partners are used to query the whole business partner records from the database.
     * The records are supplied with relevancy scores of the search hits and returned as a paginated result.
     * In case BPNs found by OpenSearch can not be found in the database, the [PageResponse] properties are
     * adapted accordingly from the OpenSearch page information
     *
     */
    override fun searchLegalEntities(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {

        val legalEntityPage = searchAndPreparePage(searchRequest, paginationRequest)
        businessPartnerFetchService.fetchLegalEntityDependencies(legalEntityPage.content.map { (_, legalEntity) -> legalEntity }.toSet())

        return with(legalEntityPage) {
            PageResponse(totalElements, totalPages, page, contentSize,
                content.map { (score, legalEntity) -> legalEntity.toMatchDto(score) })
        }
    }

    /**
     * @see SearchServiceImpl.searchLegalEntities
     */
    override fun searchAddresses(searchRequest: AddressPartnerSearchRequest, paginationRequest: PaginationRequest): PageResponse<AddressMatchResponse> {
        val addressPage = searchAndPreparePage(searchRequest, paginationRequest)

        addressService.fetchPartnerAddressDependencies(addressPage.content.map { (_, address) -> address }.toSet())

        return with(addressPage) {
            PageResponse(totalElements, totalPages, page, contentSize,
                content.map { (score, address) -> address.toMatchDto(score) })
        }
    }

    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerMatchResponse> {

        val legalEntityPage = searchAndPreparePage(searchRequest, paginationRequest)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntityPage.content.map { (_, legalEntity) -> legalEntity }.toSet())

        return with(legalEntityPage) {
            PageResponse(totalElements, totalPages, page, contentSize,
                content.map { (score, legalEntity) -> legalEntity.toBusinessPartnerMatchDto(score) })
        }
    }

    /**
     * Query OpenSearch for [field] values by [text] and [filters]
     *
     * The found values and their hit scores are converted to [SuggestionResponse] and returned as a paginated result.
     */
    override fun getSuggestion(
        field: SuggestionType,
        text: String?,
        filters: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {

        logger.debug { "Search index for suggestion type $field" }

        val hits = textDocSearchRepository.findByFieldAndTextAndFilters(
            field,
            text,
            filters,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Returning ${hits.hits.size} suggestions for $field. (${hits.totalHits} found in total)" }

        return PageResponse(
            hits.totalHits!!.value,
            ceil(hits.totalHits!!.value.toDouble() / paginationRequest.size).toInt(),
            paginationRequest.page,
            hits.hits.size,
            hits.map { hit ->
                SuggestionResponse(extractTextDocText(hit.sourceAsString), hit.score)
            }.toList()
        )
    }

    private fun extractTextDocText(textDocJson: String): String {
        val textDoc: TextDoc = objectMapper.readValue(textDocJson)
        return textDoc.text
    }

    private fun searchAndPreparePage(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, LegalEntity>> {
        logger.debug { "Search index for legal entities" }

        val searchResult = legalEntityDocSearchRepository.findBySearchRequest(
            searchRequest,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Found ${searchResult.hits.size} business partners in OpenSearch. (${searchResult.totalHits} in total)" }

        val bpnHitMap = searchResult.associateBy { it.id }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(legalEntities.map { it.bpn }.toSet())

        if (missingPartners.isNotEmpty())
            logger.warn { "Some BPNs could not be found in the database: ${missingPartners.joinToString()}" }

        val scoreLegalEntityPairs = legalEntities.map { Pair(bpnHitMap[it.bpn]!!.score, it) }.sortedByDescending { it.first }

        val totalHits = searchResult.totalHits!!.value - missingPartners.size
        val totalPages = ceil(totalHits.toDouble() / paginationRequest.size).toInt()
        return PageResponse(totalHits, totalPages, paginationRequest.page, legalEntities.size, scoreLegalEntityPairs)
    }

    private fun searchAndPreparePage(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, AddressPartner>> {
        logger.debug { "Search index for addresses" }

        val searchResult = addressDocSearchRepository.findBySearchRequest(
            searchRequest,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Found ${searchResult.hits.size} addresses in OpenSearch. (${searchResult.totalHits} in total)" }

        val bpnHitMap = searchResult.associateBy { it.id }

        val addresses = addressPartnerRepository.findDistinctByBpnIn(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(addresses.map { it.bpn }.toSet())

        if (missingPartners.isNotEmpty())
            logger.warn { "Some BPNs could not be found in the database: ${missingPartners.joinToString()}" }

        val scoreAddressPairs = addresses.map { Pair(bpnHitMap[it.bpn]!!.score, it) }.sortedByDescending { it.first }

        val totalHits = searchResult.totalHits!!.value - missingPartners.size
        val totalPages = ceil(totalHits.toDouble() / paginationRequest.size).toInt()
        return PageResponse(totalHits, totalPages, paginationRequest.page, addresses.size, scoreAddressPairs)
    }
}