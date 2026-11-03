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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchAddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchAlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchFilterTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchLegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchPhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchResultV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchSiteV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPropertiesSearchV7Request
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.exception.BusinessPartnerSearchException
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Provides search functionality on the Catena-x database for the BPDM system
 */
@Service
@Primary
class BusinessPartnerSearchService(
    private val logisticAddressRepository: LogisticAddressRepository
): SearchService {
    /**
     * @see searchBusinessPartner
     *
     */
    @Transactional(readOnly = true)
    override fun searchBusinessPartner(
        searchRequest: LegalEntityPropertiesSearchV7Request,
        searchResultFilter: Set<BusinessPartnerSearchFilterTypeV7>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultV7Dto> {

        fun String?.startsWithWhitespace(): Boolean = this?.firstOrNull()?.isWhitespace() == true

        val isAllSearchParamsEmpty = with(searchRequest) {
            listOf(bpn, legalName, city, streetName, postalCode).all { it.isNullOrBlank() } && country == null
        }

        val isBpnAndLegalNameBothBlank = searchRequest.bpn.isNullOrBlank() && searchRequest.legalName.isNullOrBlank()
        if (isAllSearchParamsEmpty || isBpnAndLegalNameBothBlank) {
            throw BusinessPartnerSearchException("At least one of 'bpn' or 'legalName' must be provided.")
        }

        val isFilterBlank = searchResultFilter.isNullOrEmpty()
        if (isFilterBlank) {
            throw BusinessPartnerSearchException("At least one filter value must be provided in 'searchResultFilter'.")
        }

        if (searchRequest.bpn.startsWithWhitespace() || searchRequest.legalName.startsWithWhitespace()) {
            throw BusinessPartnerSearchException("'bpn' and 'legalName' must not start with a whitespace character.")
        }

        searchRequest.legalName?.takeIf { it.length < 3 }?.let {
            throw BusinessPartnerSearchException("'legalName' must contain at least 3 characters.")
        }

        val pageable = PageRequest.of(paginationRequest.page, paginationRequest.size)
        val includeLegalEntities = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterTypeV7.IncludeLegalEntities)
        val includeSites = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterTypeV7.IncludeSites)
        val includeAdditionalAddresses = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterTypeV7.IncludeAdditionalAddresses)

        val results = mutableListOf<BusinessPartnerSearchResultV7Dto>()

        val matchedAddress = logisticAddressRepository.searchBusinessPartner(
            searchRequest,
            includeLegalEntities,
            includeSites,
            includeAdditionalAddresses,
            pageable
        )

        if (matchedAddress.hasContent()) {
            logisticAddressRepository.joinCountries(matchedAddress.content.toSet())
        }
        results.addAll(matchedAddress.map{ searchAddressResultMapping(it) })

        return PageDto(
            totalElements = matchedAddress.totalElements,
            totalPages = matchedAddress.totalPages,
            page = paginationRequest.page,
            contentSize = results.size,
            content = results
        )
    }

    private fun shouldInclude(
        searchResultFilter: Set<BusinessPartnerSearchFilterTypeV7>?,
        filterType: BusinessPartnerSearchFilterTypeV7
    ): Boolean {
        return searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(filterType)
    }

    private fun searchAddressResultMapping(result: LogisticAddressDb): BusinessPartnerSearchResultV7Dto {

        val legalEntity = requireNotNull(result.legalEntity) {
            "searchAddressResultMapping requires LogisticAddressDb.legalEntity to be non-null"
        }

        val legalAddressId = result.legalEntity?.legalAddress?.id
        val isSiteMainAddress = result.sites.any { it.mainAddress == result }

        val addressType = when {
            result.id == legalAddressId && isSiteMainAddress -> AddressType.LegalAndSiteMainAddress
            result.id == legalAddressId -> AddressType.LegalAddress
            isSiteMainAddress -> AddressType.SiteMainAddress
            else -> AddressType.AdditionalAddress
        }

        val identifiers: List<BusinessPartnerSearchIdentifierV7Dto> =
            legalEntity.identifiers.map { identifier ->
                BusinessPartnerSearchIdentifierV7Dto(
                    type = identifier.type.technicalKey,
                    value = identifier.value,
                    issuingBody = identifier.issuingBody
                )
            }

        val states: List<BusinessPartnerSearchStateV7Dto> =
            legalEntity.states.map { state ->
                BusinessPartnerSearchStateV7Dto(
                    validTo = BusinessStateDates.reportValidTo(state.validTo),
                    validFrom = BusinessStateDates.reportValidFrom(state.validFrom),
                    type = state.type
                )
            }

        val addressIdentifiers: Collection<BusinessPartnerSearchAddressIdentifierV7Dto> =
            result.identifiers.map { identifier ->
                BusinessPartnerSearchAddressIdentifierV7Dto(
                    type = identifier.type.technicalKey,
                    value = identifier.value
                )
            }

        val legalEntityDto = BusinessPartnerSearchLegalEntityV7Dto(
            legalEntityBpn = legalEntity.bpn,
            legalName = legalEntity.legalName.value,
            shortName = legalEntity.legalName.shortName,
            legalForm = legalEntity.legalForm?.technicalKey,
            confidenceCriteria = with(legalEntity.confidenceCriteria) {
                BusinessPartnerSearchConfidenceCriteriaV7Dto(
                    sharedByOwner = sharedByOwner,
                    checkedByExternalDataSource = checkedByExternalDataSource,
                    numberOfSharingMembers = numberOfSharingMembers,
                    lastConfidenceCheckAt = lastConfidenceCheckAt,
                    nextConfidenceCheckAt = nextConfidenceCheckAt,
                    confidenceLevel = confidenceLevel
                )
            },
            states = legalEntity.states.map {
                BusinessPartnerSearchStateV7Dto(
                    validTo = BusinessStateDates.reportValidTo(it.validTo),
                    validFrom = BusinessStateDates.reportValidFrom(it.validFrom),
                    type = it.type
                )
            }
        )

        val siteDto = result.mainSite?.let { site ->
            BusinessPartnerSearchSiteV7Dto(
                siteBpn = site.bpn,
                name = site.name,
                confidenceCriteria = with(site.confidenceCriteria) {
                    BusinessPartnerSearchConfidenceCriteriaV7Dto(
                        sharedByOwner = sharedByOwner,
                        checkedByExternalDataSource = checkedByExternalDataSource,
                        numberOfSharingMembers = numberOfSharingMembers,
                        lastConfidenceCheckAt = lastConfidenceCheckAt,
                        nextConfidenceCheckAt = nextConfidenceCheckAt,
                        confidenceLevel = confidenceLevel
                    )
                },
                states = site.states.map {
                    BusinessPartnerSearchStateV7Dto(
                        validTo = BusinessStateDates.reportValidTo(it.validTo),
                        validFrom = BusinessStateDates.reportValidFrom(it.validFrom),
                        type = it.type
                    )
                }
            )
        }

        val physical = result.physicalPostalAddress
        val alternative = result.alternativePostalAddress
        val streetDto = physical.street?.let { street ->
            StreetV7Dto(
                name = street.name,
                houseNumber = street.houseNumber,
                houseNumberSupplement = street.houseNumberSupplement,
                milestone = street.milestone,
                direction = street.direction,
                namePrefix = street.namePrefix,
                additionalNamePrefix = street.additionalNamePrefix,
                nameSuffix = street.nameSuffix,
                additionalNameSuffix = street.additionalNameSuffix
            )
        }

        val addressDto = BusinessPartnerSearchPostalAddressV7Dto(
            name = result.name,
            addressType = addressType,
            identifiers = addressIdentifiers,
            addressBpn = result.bpn,
            states = result.states.map {
                BusinessPartnerSearchStateV7Dto(
                    validTo = BusinessStateDates.reportValidTo(it.validTo),
                    validFrom = BusinessStateDates.reportValidFrom(it.validFrom),
                    type = it.type
                )
            },
            physicalPostalAddress = BusinessPartnerSearchPhysicalAddressV7Dto(
                geographicCoordinates = physical.geographicCoordinates?.let {
                    GeoCoordinateDto(
                    longitude = it.longitude,
                    latitude = it.latitude,
                    altitude = it.altitude
                )},
                administrativeAreaLevel1 = physical.administrativeAreaLevel1?.regionCode,
                administrativeAreaLevel2 = physical.administrativeAreaLevel2,
                administrativeAreaLevel3 = physical.administrativeAreaLevel3,
                street = streetDto,
                postalCode = physical.postCode,
                city = physical.city,
                country = physical.country.countryCode,
                district = physical.districtLevel1,
                companyPostalCode = physical.companyPostCode,
                industrialZone = physical.industrialZone,
                building = physical.building,
                floor = physical.floor,
                door = physical.door,
                taxJurisdictionCode = physical.taxJurisdictionCode
            ),
            alternativePostalAddress = BusinessPartnerSearchAlternativeAddressV7Dto(
                geographicCoordinates = alternative?.geographicCoordinates?.let {
                    GeoCoordinateDto(
                        longitude = it.longitude,
                        latitude = it.latitude,
                        altitude = it.altitude
                    )},
                country = alternative?.country?.countryCode,
                administrativeAreaLevel1 = alternative?.administrativeAreaLevel1?.regionCode,
                postalCode = alternative?.postCode,
                city = alternative?.city,
                deliveryServiceType =  alternative?.deliveryServiceType,
                deliveryServiceQualifier = alternative?.deliveryServiceQualifier,
                deliveryServiceNumber = alternative?.deliveryServiceNumber

            ),
            confidenceCriteria = with(result.confidenceCriteria) {
                BusinessPartnerSearchConfidenceCriteriaV7Dto(
                    sharedByOwner = sharedByOwner,
                    checkedByExternalDataSource = checkedByExternalDataSource,
                    numberOfSharingMembers = numberOfSharingMembers,
                    lastConfidenceCheckAt = lastConfidenceCheckAt,
                    nextConfidenceCheckAt = nextConfidenceCheckAt,
                    confidenceLevel = confidenceLevel
                )
            }
        )

        return BusinessPartnerSearchResultV7Dto(
            identifiers = identifiers,
            states = states,
            legalEntity = legalEntityDto,
            site = siteDto,
            address = addressDto,
            isParticipantData = legalEntity.isDataSpaceParticipant
        )
    }
}