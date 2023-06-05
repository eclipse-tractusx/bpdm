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
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.AddressDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.LegalEntityDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.config.OpenSearchConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.pool.exception.BpdmOpenSearchUserException
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.*
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
    val logisticAddressRepository: LogisticAddressRepository,
    val addressService: AddressService,
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val objectMapper: ObjectMapper,
    val openSearchConfigProperties: OpenSearchConfigProperties
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

        addressService.fetchLogisticAddressDependencies(addressPage.content.map { (_, address) -> address }.toSet())

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

    private fun searchAndPreparePage(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, LegalEntity>> {
        return if (searchRequest == BusinessPartnerSearchRequest.EmptySearchRequest) {
            paginateLegalEntities(paginationRequest)
        } else {
            searchIndex(searchRequest, paginationRequest)
        }
    }

    private fun searchAndPreparePage(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, LogisticAddress>> {

        return if (searchRequest == AddressPartnerSearchRequest.EmptySearchRequest) {
            paginateAddressPartner(paginationRequest)
        } else {
            searchIndex(searchRequest, paginationRequest)
        }
    }

    private fun paginateLegalEntities(paginationRequest: PaginationRequest): PageResponse<Pair<Float, LegalEntity>> {
        logger.debug { "Paginate database for legal entities" }
        val legalEntityPage = legalEntityRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toDto(legalEntityPage.content.map { Pair(0f, it) }) // assign 0 score as no search has been conducted
    }

    private fun paginateAddressPartner(paginationRequest: PaginationRequest): PageResponse<Pair<Float, LogisticAddress>> {
        logger.debug { "Paginate database for address partners" }
        val addressPage = logisticAddressRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        return addressPage.toDto(addressPage.content.map { Pair(0f, it) }) // assign 0 score as no search has been conducted
    }

    private fun searchIndex(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, LegalEntity>> {
        logger.debug { "Search index for legal entities" }

        if (paginationRequest.page > openSearchConfigProperties.maxPage)
            throw BpdmOpenSearchUserException("When using search parameters page can't exceed ${openSearchConfigProperties.maxPage} but was ${paginationRequest.page} instead")

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

    private fun searchIndex(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<Pair<Float, LogisticAddress>> {
        logger.debug { "Search index for addresses" }

        if (paginationRequest.page > openSearchConfigProperties.maxPage)
            throw BpdmOpenSearchUserException("When using search parameters page can't exceed ${openSearchConfigProperties.maxPage} but was ${paginationRequest.page} instead")

        val searchResult = addressDocSearchRepository.findBySearchRequest(
            searchRequest,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.info { "Found ${searchResult.hits.size} addresses in OpenSearch. (${searchResult.totalHits} in total)" }

        val bpnHitMap = searchResult.associateBy { it.id }

        val addresses = logisticAddressRepository.findDistinctByBpnIn(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(addresses.map { it.bpn }.toSet())

        if (missingPartners.isNotEmpty())
            logger.warn { "Some BPNs could not be found in the database: ${missingPartners.joinToString()}" }

        val scoreAddressPairs = addresses.map { Pair(bpnHitMap[it.bpn]!!.score, it) }.sortedByDescending { it.first }

        val totalHits = searchResult.totalHits!!.value - missingPartners.size
        val totalPages = ceil(totalHits.toDouble() / paginationRequest.size).toInt()
        return PageResponse(totalHits, totalPages, paginationRequest.page, addresses.size, scoreAddressPairs)
    }

}