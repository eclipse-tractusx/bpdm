/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Provides search functionality on the Catena-x database for the BPDM system
 */
@Service
@Primary
class BusinessPartnerSearchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val addressService: AddressService,
    private val siteService: SiteService,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val siteRepository: SiteRepository,
): SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Uses the [searchRequest] and [paginationRequest] to perform query on database for business partner.
     * The BPNs of found partners are used to query the whole business partner records from the database.
     * The records are supplied with relevancy scores of the search hits and returned as a paginated result.
     * In case BPNs, can not be found in the database, the [PageDto] properties are
     * adapted accordingly from the page information
     *
     */
    override fun searchLegalEntities(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityMatchVerboseDto> {
        val legalEntityPage = searchAndPrepareLegalEntityPage(searchRequest, paginationRequest)
        businessPartnerFetchService.fetchLegalEntityDependencies(legalEntityPage.content.map { (_, legalEntity) -> legalEntity }.toSet())

        return with(legalEntityPage) {
            PageDto(totalElements, totalPages, page, contentSize,
                content.map { (score, legalEntity) -> legalEntity.toMatchDto(score) })
        }
    }

    private fun searchAndPrepareLegalEntityPage(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<Pair<Float, LegalEntityDb>> {
        return if (searchRequest == BusinessPartnerSearchRequest.EmptySearchRequest) {
            paginateLegalEntities(paginationRequest)
        } else {
            searchLegalEntity(searchRequest, paginationRequest)
        }
    }

    private fun paginateLegalEntities(paginationRequest: PaginationRequest): PageDto<Pair<Float, LegalEntityDb>> {
        logger.debug { "Paginate database for legal entities" }
        val legalEntityPage = legalEntityRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toDto(legalEntityPage.content.map { Pair(0f, it) }) //assigned 0 score as no ordering performed
    }

    private fun searchLegalEntity(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<Pair<Float, LegalEntityDb>> {
        logger.debug { "Search for legal entities" }

        // Concatenate and convert to lowercase.
        val value = "%${searchRequest.partnerProperties.legalName}%".lowercase()
        val legalEntityPage = legalEntityRepository.findByLegalNameValue(value, PageRequest.of(paginationRequest.page, paginationRequest.size))
        return PageDto(
            totalElements = legalEntityPage.totalElements,
            totalPages = legalEntityPage.totalPages,
            page = paginationRequest.page,
            content = legalEntityPage.content.mapIndexed { index, legalEntity ->
                val score = legalEntityPage.totalElements - paginationRequest.page.toLong() * paginationRequest.size.toLong() - index
                Pair(score.toFloat(), legalEntity)
            },
            contentSize = legalEntityPage.content.size
        )

    }

    /**
     * @see searchLegalEntities
     *
     */
    override fun searchAddresses(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<AddressMatchVerboseDto> {
        val addressPage = searchAndPrepareAddressPage(searchRequest, paginationRequest)
        addressService.fetchLogisticAddressDependencies(addressPage.content.map { (_, address) -> address }.toSet())
        return with(addressPage) {
            PageDto(totalElements, totalPages, page, contentSize,
                content.map { (score, address) -> address.toMatchDto(score) })
        }
    }

    private fun searchAndPrepareAddressPage(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<Pair<Float, LogisticAddressDb>> {

        return if (searchRequest == AddressPartnerSearchRequest.EmptySearchRequest) {
            paginateAddressPartner(paginationRequest)
        } else {
            searchAddress(searchRequest, paginationRequest)
        }
    }

    private fun paginateAddressPartner(paginationRequest: PaginationRequest): PageDto<Pair<Float, LogisticAddressDb>> {
        logger.debug { "Paginate database for address partners" }
        val addressPage = logisticAddressRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        return addressPage.toDto(addressPage.content.map { Pair(0f, it) }) //assigned 0 score as no ordering performed
    }

    private fun searchAddress(
        searchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<Pair<Float, LogisticAddressDb>> {
        logger.debug { "Search for addresses" }

        // Concatenate and convert to lowercase.
        val addressName = "%${searchRequest.name}%".lowercase()
        val addressPage = logisticAddressRepository.findByName(addressName, PageRequest.of(paginationRequest.page, paginationRequest.size))
        return PageDto(
            totalElements = addressPage.totalElements,
            totalPages = addressPage.totalPages,
            page = paginationRequest.page,
            content = addressPage.content.mapIndexed { index, address ->
                val score = addressPage.totalElements - paginationRequest.page.toLong() * paginationRequest.size.toLong() - index
                Pair(score.toFloat(), address)
            },
            contentSize = addressPage.content.size
        )

    }

    /**
     * @see searchLegalEntities
     *
     */
    override fun searchSites(
        paginationRequest: PaginationRequest
    ): PageDto<SiteMatchVerboseDto> {
        val sitePage = searchAndPreparePageSite(paginationRequest)
        siteService.fetchSiteDependenciesPage(sitePage.content.map { site -> site }.toSet())

        return with(sitePage) {
            PageDto(totalElements, totalPages, page, contentSize,
                content.map { site -> site.toMatchDto() })
        }
    }

    private fun searchAndPreparePageSite(
        paginationRequest: PaginationRequest
    ): PageDto<SiteDb> {
        return paginateSite(paginationRequest)
    }

    private fun paginateSite(paginationRequest: PaginationRequest): PageDto<SiteDb> {
        logger.debug { "Paginate database for sites" }
        val sitePage = siteRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        return sitePage.toDto(sitePage.content.map { it })
    }
}