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

import com.nimbusds.openid.connect.sdk.claims.Address
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
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

        val includeLegalEntities = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.ShowOnlyLegaEntities)
        val includeSites = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.ShowOnlySites)
        val includeAddresses = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses)

        val results = mutableListOf<BusinessPartnerSearchResultDto>()

        if (searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyLegaEntities)) {
            val legalEntities = legalEntityRepository.findAll(LegalEntityRepository.buildLegalEntitySpecification(searchRequest),pageRequest )

            if (includeLegalEntities) {
                val legalEntities = legalEntityRepository.findAll(
                    LegalEntityRepository.buildLegalEntitySpecification(searchRequest),
                    pageRequest
                )
                results.addAll(legalEntities.content.map { searchLegalEntityResultToMapping(it) })
            }

            if (includeSites) {
                results.addAll(legalEntities.content.flatMap { it.sites.map { site -> searchSiteResultMapping(site) } })
            }

            if (includeAddresses) {
                appendAddressesWithoutDuplicateStreet(
                    legalEntities.content.flatMap { it.addresses },
                    results
                )
            }
        }

        if (includeSites) {
            val sites = siteRepository.findAll(SiteRepository.buildSiteSpecification(searchRequest), pageRequest)
            results.addAll(sites.content.map { searchSiteResultMapping(it) })

            if (includeLegalEntities) {
                val parentLegalEntity = legalEntityRepository.findAllById(sites.content.map { it.legalEntity.id })
                if (parentLegalEntity.isNotEmpty()) {
                    results.add(searchLegalEntityResultToMapping(parentLegalEntity.first()))
                }
            }

            if (includeAddresses) {
                appendAddressesWithoutDuplicateStreet(
                    sites.content.flatMap { it.addresses },
                    results
                )
            }
        }

        val shouldSearchAddressesDirectly = !searchResultFilter.isNullOrEmpty() &&
                                            !searchRequest.id.isNullOrEmpty() &&
                                            searchResultFilter.contains(BusinessPartnerSearchFilterType.ShowOnlyAdditionalAddresses)

        if (shouldSearchAddressesDirectly) {

            val addresses = logisticAddressRepository.findAll(
                LogisticAddressRepository.buildAddressSpecification(searchRequest),
                pageRequest
            )

            appendAddressesWithoutDuplicateStreet(addresses.content, results)

            if (includeLegalEntities) {
                val parentLegalEntity = legalEntityRepository.findAllById(addresses.content.map { it.legalEntity!!.id })
                if (parentLegalEntity.isNotEmpty()) {
                    results.add(searchLegalEntityResultToMapping(parentLegalEntity.first()))
                }
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

    private fun shouldInclude(
        searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        filterType: BusinessPartnerSearchFilterType
    ): Boolean {
        return searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(filterType)
    }

    private fun appendAddressesWithoutDuplicateStreet(
        addresses: Iterable<LogisticAddressDb>,
        results: MutableList<BusinessPartnerSearchResultDto>
    ) {
        val existingStreetNames = results
            .mapNotNull { it.address.physicalPostalAddress.street?.name }
            .toMutableSet()

        addresses.forEach { address ->
            val streetName = address.physicalPostalAddress.street?.name
            if (streetName == null || !existingStreetNames.contains(streetName)) {
                if (address.site == null) {
                    results += searchAddressResultMapping(address)
                }
                if (streetName != null) {
                    existingStreetNames += streetName
                }
            }
        }
    }


    private fun searchLegalEntityResultToMapping(result: LegalEntityDb): BusinessPartnerSearchResultDto {
        return BusinessPartnerSearchResultDto (
            identifiers = result.identifiers.map { it ->
                BusinessPartnerIdentifierDto(
                    type = it.type.technicalKey,
                    value = it.value,
                    issuingBody = it.issuingBody
                )
            },
            legalEntity = LegalEntityRepresentationOutputDto(
                legalEntityBpn = result.bpn,
                legalName = result.legalName.value,
                legalForm = result.legalForm?.name,
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.confidenceCriteria.confidenceLevel
                )
            ),
            site = null,
            address = AddressComponentOutputDto(
                name = null,
                addressType = AddressType.LegalAddress,
                addressBpn = result.legalAddress.bpn,
                physicalPostalAddress = PhysicalPostalAddressDto (
                    geographicCoordinates = null,
                    administrativeAreaLevel1 = result.legalAddress.physicalPostalAddress.administrativeAreaLevel1?.countryCode?.name,
                    street = org.eclipse.tractusx.bpdm.gate.api.model.StreetDto(
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
                    postalCode = result.legalAddress.physicalPostalAddress.postCode,
                    city = result.legalAddress.physicalPostalAddress.city,
                    country = result.legalAddress.physicalPostalAddress.country
                ),
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.legalAddress.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.legalAddress.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.legalAddress.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.legalAddress.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.legalAddress.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.legalAddress.confidenceCriteria.confidenceLevel
                )
            ),
            isParticipantData = result.isCatenaXMemberData,
        )
    }
    private fun searchSiteResultMapping(result: SiteDb): BusinessPartnerSearchResultDto {

        return BusinessPartnerSearchResultDto(
            identifiers = result.legalEntity.identifiers.map { it ->
                BusinessPartnerIdentifierDto(
                    type = it.type.technicalKey,
                    value = it.value,
                    issuingBody = it.issuingBody
                )
            },
            legalEntity = LegalEntityRepresentationOutputDto(
                legalEntityBpn = result.legalEntity.bpn,
                legalName = result.legalEntity.legalName.value,
                legalForm = result.legalEntity.legalForm?.name,
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.legalEntity.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.legalEntity.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.legalEntity.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.legalEntity.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.legalEntity.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.legalEntity.confidenceCriteria.confidenceLevel
                )
            ),
            site = SiteRepresentationOutputDto (
                siteBpn = result.bpn,
                name = result.name,
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.confidenceCriteria.confidenceLevel
                )
            ),
            address = AddressComponentOutputDto(
                name = null,
                addressType = AddressType.SiteMainAddress,
                addressBpn = result.mainAddress.bpn,
                physicalPostalAddress = PhysicalPostalAddressDto (
                    geographicCoordinates = null,
                    administrativeAreaLevel1 = result.mainAddress.physicalPostalAddress.administrativeAreaLevel1?.countryCode?.name,
                    street = org.eclipse.tractusx.bpdm.gate.api.model.StreetDto(
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
                    postalCode = result.mainAddress.physicalPostalAddress.postCode,
                    city = result.mainAddress.physicalPostalAddress.city,
                    country = result.mainAddress.physicalPostalAddress.country
                ),
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.mainAddress.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.mainAddress.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.mainAddress.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.mainAddress.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.mainAddress.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.mainAddress.confidenceCriteria.confidenceLevel
                )
            ),
            isParticipantData = result.legalEntity.isCatenaXMemberData
        )
    }
    private fun searchAddressResultMapping(result: LogisticAddressDb): BusinessPartnerSearchResultDto {

        val addressType = when (result.id) {
            result.legalEntity?.legalAddress?.id -> AddressType.LegalAddress
            result.site?.mainAddress?.id        -> AddressType.SiteMainAddress
            else                                -> AddressType.AdditionalAddress
        }

        return BusinessPartnerSearchResultDto(
            identifiers = result.legalEntity?.identifiers?.map { it ->
                BusinessPartnerIdentifierDto(
                    type = it.type.technicalKey,
                    value = it.value,
                    issuingBody = it.issuingBody
                )
            } as Collection<BusinessPartnerIdentifierDto>,
            legalEntity = LegalEntityRepresentationOutputDto(
                legalEntityBpn = result.legalEntity!!.bpn,
                legalName = result.legalEntity?.legalName?.value,
                legalForm = result.legalEntity?.legalForm?.name,
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.legalEntity!!.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.legalEntity!!.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.legalEntity!!.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.legalEntity!!.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.legalEntity!!.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.legalEntity!!.confidenceCriteria.confidenceLevel
                )
            ),
            site = null,
            address = AddressComponentOutputDto(
                name = null,
                addressType = addressType,
                addressBpn = result.bpn,
                physicalPostalAddress = PhysicalPostalAddressDto (
                    geographicCoordinates = null,
                    administrativeAreaLevel1 = result.physicalPostalAddress.administrativeAreaLevel1?.countryCode?.name,
                    street = org.eclipse.tractusx.bpdm.gate.api.model.StreetDto(
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
                    postalCode = result.physicalPostalAddress.postCode,
                    city = result.physicalPostalAddress.city,
                    country = result.physicalPostalAddress.country
                ),
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = result.confidenceCriteria.sharedByOwner,
                    checkedByExternalDataSource = result.confidenceCriteria.checkedByExternalDataSource,
                    numberOfSharingMembers = result.confidenceCriteria.numberOfBusinessPartners,
                    lastConfidenceCheckAt = result.confidenceCriteria.lastConfidenceCheckAt,
                    nextConfidenceCheckAt = result.confidenceCriteria.nextConfidenceCheckAt,
                    confidenceLevel = result.confidenceCriteria.confidenceLevel
                )
            ),
            isParticipantData = result.legalEntity!!.isCatenaXMemberData
        )
    }
}