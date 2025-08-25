/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSearchResultDto
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
    private val siteRepository: SiteRepository
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
            PageDto(
                totalElements, totalPages, page, contentSize,
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
            PageDto(
                totalElements, totalPages, page, contentSize,
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
            PageDto(
                totalElements, totalPages, page, contentSize,
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

    /**
     * @see searchBusinessPartner
     *
     */
    override fun searchBusinessPartner(
        searchRequest: LegalEntityPropertiesSearchRequest,
        searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultDto> {

        val pageRequest = PageRequest.of(paginationRequest.page, paginationRequest.size)

        val results = mutableListOf<BusinessPartnerSearchResultDto>()

        if (searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities)) {
            val legalEntities = legalEntityRepository.findAll(LegalEntityRepository.buildLegalEntitySpecification(searchRequest),pageRequest )
            results.addAll(legalEntities.content.map { searchLegalEntityResultToMapping(it) })

            if(searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlySites)) results.addAll(legalEntities.content.flatMap{it.sites.map{ site->searchSiteResultMapping (site)}})

            if(searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses)) {
                results.addAll(legalEntities.content.flatMap { it.addresses.filterNot { it.physicalPostalAddress.street?.name == results.firstOrNull()?.street?.name }.map { address -> searchAddressResultMapping(address) }})
            }
        }

        if (searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlySites)) {
            val sites = siteRepository.findAll(SiteRepository.buildSiteSpecification(searchRequest), pageRequest)
            results.addAll(sites.content.map { searchSiteResultMapping(it) })

            if(searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities)) {
                val parentLegalEntity = legalEntityRepository.findAllById(sites.content.map {it.legalEntity.id});
                if(parentLegalEntity.isNotEmpty()) results.add(searchLegalEntityResultToMapping(parentLegalEntity.first()) )
            }

            if(searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses)) {
                results.addAll(sites.content.flatMap {
                    it.addresses.filterNot { it.physicalPostalAddress.street?.name == results.firstOrNull()?.street?.name }
                        .map { address -> searchAddressResultMapping(address) }
                })
            }
        }

        if (!searchResultFilter.isNullOrEmpty() && !searchRequest.id.isNullOrEmpty() && searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses )) {

            val addresses = logisticAddressRepository.findAll(
                LogisticAddressRepository.buildAddressSpecification(searchRequest),
                pageRequest
            )

            results.addAll(addresses.content.map{ searchAddressResultMapping(it) })

            val existingStreetNames = results.map { it.street }.toSet()
            if (existingStreetNames.isNotEmpty()) {
                results.addAll(addresses.filterNot { address ->
                    val streetName = address.physicalPostalAddress.street?.name
                    streetName != null && existingStreetNames.firstOrNull()?.name!!.contains(streetName)
                }.map { searchAddressResultMapping(it) })
            }

            if (searchResultFilter.isEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities)) {
                val parentLegalEntity = legalEntityRepository.findAllById(addresses.content.map { it.legalEntity!!.id });
                if(parentLegalEntity.isNotEmpty()) results.add(searchLegalEntityResultToMapping(parentLegalEntity.first()))
            }
        }

        val start = paginationRequest.page * paginationRequest.size
        val end = minOf(start + paginationRequest.size, results.size)
        val pagedContent = if (start < results.size) results.subList(start, end) else emptyList()

        return PageDto(
            totalElements = results.size.toLong(),
            totalPages = (results.size / paginationRequest.size) + if (results.size % paginationRequest.size == 0) 0 else 1,
            page = paginationRequest.page,
            contentSize = paginationRequest.size,
            content = pagedContent
        )
    }

    private fun searchLegalEntityResultToMapping(result: LegalEntityDb): BusinessPartnerSearchResultDto {

        return BusinessPartnerSearchResultDto(
            id = result.bpn,
            name = result.legalName.value,
            identifiers = result.identifiers.map { it ->
                LegalEntityIdentifierDto(
                    type = it.type.technicalKey,
                    value = it.value,
                    issuingBody = it.issuingBody
                )
            },
            legalForm = result.legalForm?.let {
                LegalFormDto(
                    technicalKey = result.legalForm!!.technicalKey,
                    name = result.legalForm!!.name,
                    isActive = result.legalForm!!.isActive,
                    transliteratedName = result.legalForm?.transliteratedName,
                    country = result.legalForm?.countryCode,
                    language = result.legalForm?.languageCode,
                    administrativeAreaLevel1 = result.legalForm?.administrativeArea?.countryCode?.name,
                    transliteratedAbbreviations = result.legalForm?.transliteratedAbbreviations
                )
            },
            street = StreetDto(
                name = result.legalAddress.physicalPostalAddress.street?.name,
                houseNumber = result.legalAddress.physicalPostalAddress.street?.houseNumber,
                houseNumberSupplement = result.legalAddress.physicalPostalAddress.street?.houseNumberSupplement,
                milestone = result.legalAddress.physicalPostalAddress.street?.milestone,
                direction = result.legalAddress.physicalPostalAddress.street?.direction,
                namePrefix = result.legalAddress.physicalPostalAddress.street?.namePrefix,
                additionalNamePrefix = result.legalAddress.physicalPostalAddress.street?.additionalNamePrefix,
                nameSuffix = result.legalAddress.physicalPostalAddress.street?.nameSuffix,
                additionalNameSuffix = result.legalAddress.physicalPostalAddress.street?.additionalNameSuffix
            ),
            city = result.legalAddress.physicalPostalAddress.city,
            postalCode = result.legalAddress.physicalPostalAddress.postCode.toString(),
            country = result.legalAddress.physicalPostalAddress.country.toString(),
        )
    }
    private fun searchSiteResultMapping(result: SiteDb): BusinessPartnerSearchResultDto {

        return BusinessPartnerSearchResultDto(
            id = result.bpn,
            name = result.name,
            identifiers = emptyList(),
            legalForm = null,
            street = StreetDto(
                name = result.mainAddress.physicalPostalAddress.street?.name,
                houseNumber = result.mainAddress.physicalPostalAddress.street?.houseNumber,
                houseNumberSupplement = result.mainAddress.physicalPostalAddress.street?.houseNumberSupplement,
                milestone = result.mainAddress.physicalPostalAddress.street?.milestone,
                direction = result.mainAddress.physicalPostalAddress.street?.direction,
                namePrefix = result.mainAddress.physicalPostalAddress.street?.namePrefix,
                additionalNamePrefix = result.mainAddress.physicalPostalAddress.street?.additionalNamePrefix,
                nameSuffix = result.mainAddress.physicalPostalAddress.street?.nameSuffix,
                additionalNameSuffix = result.mainAddress.physicalPostalAddress.street?.additionalNameSuffix
            ),
            city = result.mainAddress.physicalPostalAddress.city,
            postalCode = result.mainAddress.physicalPostalAddress.postCode.toString(),
            country = result.mainAddress.physicalPostalAddress.country.toString(),
        )
    }
    private fun searchAddressResultMapping(result: LogisticAddressDb): BusinessPartnerSearchResultDto {

        return BusinessPartnerSearchResultDto(
            id = result.bpn,
            name = result.name,
            identifiers = emptyList(),
            legalForm = null,
            street = StreetDto(
                name = result.physicalPostalAddress.street?.name,
                houseNumber = result.physicalPostalAddress.street?.houseNumber,
                houseNumberSupplement = result.physicalPostalAddress.street?.houseNumberSupplement,
                milestone = result.physicalPostalAddress.street?.milestone,
                direction = result.physicalPostalAddress.street?.direction,
                namePrefix = result.physicalPostalAddress.street?.namePrefix,
                additionalNamePrefix = result.physicalPostalAddress.street?.additionalNamePrefix,
                nameSuffix = result.physicalPostalAddress.street?.nameSuffix,
                additionalNameSuffix = result.physicalPostalAddress.street?.additionalNameSuffix
            ),
            city = result.physicalPostalAddress.city,
            postalCode = result.physicalPostalAddress.postCode.toString(),
            country = result.physicalPostalAddress.country.toString(),
        )
    }
}