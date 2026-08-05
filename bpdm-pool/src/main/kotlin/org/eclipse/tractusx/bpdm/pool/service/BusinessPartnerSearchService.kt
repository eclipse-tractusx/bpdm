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
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.exception.BusinessPartnerSearchException
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service


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
    override fun searchBusinessPartner(
        searchRequest: LegalEntityPropertiesSearchRequest,
        searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultDto> {

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
        val includeLegalEntities = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.IncludeLegalEntities)
        val includeSites = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.IncludeSites)
        val includeAdditionalAddresses = shouldInclude(searchResultFilter, BusinessPartnerSearchFilterType.IncludeAdditionalAddresses)

        val results = mutableListOf<BusinessPartnerSearchResultDto>()

        val matchedAddress = logisticAddressRepository.searchBusinessPartner(
            searchRequest,
            includeLegalEntities,
            includeSites,
            includeAdditionalAddresses,
            pageable
        )

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
        searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        filterType: BusinessPartnerSearchFilterType
    ): Boolean {
        return searchResultFilter.isNullOrEmpty() || searchResultFilter.contains(filterType)
    }

    private fun searchAddressResultMapping(result: LogisticAddressDb): BusinessPartnerSearchResultDto {

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

        val identifiers: List<BusinessPartnerIdentifierDto> =
            legalEntity.identifiers.map { identifier ->
                BusinessPartnerIdentifierDto(
                    type = identifier.type.technicalKey,
                    value = identifier.value,
                    issuingBody = identifier.issuingBody
                )
            }

        val states: List<BusinessPartnerStateDto> =
            legalEntity.states.map { state ->
                BusinessPartnerStateDto(
                    validTo = state.validTo,
                    validFrom = state.validFrom,
                    type = state.type
                )
            }

        val addressIdentifiers: Collection<AddressIdentifierDto> =
            result.identifiers.map { identifier ->
                AddressIdentifierDto(
                    type = identifier.type.technicalKey,
                    value = identifier.value
                )
            }

        val legalEntityDto = BusinessPartnerLegalEntity(
            legalEntityBpn = legalEntity.bpn,
            legalName = legalEntity.legalName.value,
            shortName = legalEntity.legalName.shortName,
            legalForm = legalEntity.legalForm?.technicalKey,
            confidenceCriteria = with(legalEntity.confidenceCriteria) {
                BusinessPartnerConfidenceCriteriaDto(
                    sharedByOwner = sharedByOwner,
                    checkedByExternalDataSource = checkedByExternalDataSource,
                    numberOfSharingMembers = numberOfSharingMembers,
                    lastConfidenceCheckAt = lastConfidenceCheckAt,
                    nextConfidenceCheckAt = nextConfidenceCheckAt,
                    confidenceLevel = confidenceLevel
                )
            },
            states = legalEntity.states.map {
                BusinessPartnerStateDto(
                    validTo = it.validTo,
                    validFrom = it.validFrom,
                    type = it.type
                )
            }
        )

        val siteDto = result.mainSite?.let { site ->
            BusinessPartnerSite(
                siteBpn = site.bpn,
                name = site.name,
                confidenceCriteria = with(site.confidenceCriteria) {
                    BusinessPartnerConfidenceCriteriaDto(
                        sharedByOwner = sharedByOwner,
                        checkedByExternalDataSource = checkedByExternalDataSource,
                        numberOfSharingMembers = numberOfSharingMembers,
                        lastConfidenceCheckAt = lastConfidenceCheckAt,
                        nextConfidenceCheckAt = nextConfidenceCheckAt,
                        confidenceLevel = confidenceLevel
                    )
                },
                states = site.states.map {
                    BusinessPartnerStateDto(
                        validTo = it.validTo,
                        validFrom = it.validFrom,
                        type = it.type
                    )
                }
            )
        }

        val physical = result.physicalPostalAddress
        val alternative = result.alternativePostalAddress
        val streetDto = physical.street?.let { street ->
            StreetDto(
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

        val addressDto = BusinessPartnerPostalAddress(
            name = result.name,
            addressType = addressType,
            identifiers = addressIdentifiers,
            addressBpn = result.bpn,
            states = result.states.map {
                BusinessPartnerStateDto(
                    validTo = it.validTo,
                    validFrom = it.validFrom,
                    type = it.type
                )
            },
            physicalPostalAddress = PhysicalPostalAddressDto(
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
                country = physical.country,
                district = physical.districtLevel1,
                companyPostalCode = physical.companyPostCode,
                industrialZone = physical.industrialZone,
                building = physical.building,
                floor = physical.floor,
                door = physical.door,
                taxJurisdictionCode = physical.taxJurisdictionCode
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = alternative?.geographicCoordinates?.let {
                    GeoCoordinateDto(
                        longitude = it.longitude,
                        latitude = it.latitude,
                        altitude = it.altitude
                    )},
                country = alternative?.country,
                administrativeAreaLevel1 = alternative?.administrativeAreaLevel1?.countryCode?.name,
                postalCode = alternative?.postCode,
                city = alternative?.city,
                deliveryServiceType =  alternative?.deliveryServiceType,
                deliveryServiceQualifier = alternative?.deliveryServiceQualifier,
                deliveryServiceNumber = alternative?.deliveryServiceNumber

            ),
            confidenceCriteria = with(result.confidenceCriteria) {
                BusinessPartnerConfidenceCriteriaDto(
                    sharedByOwner = sharedByOwner,
                    checkedByExternalDataSource = checkedByExternalDataSource,
                    numberOfSharingMembers = numberOfSharingMembers,
                    lastConfidenceCheckAt = lastConfidenceCheckAt,
                    nextConfidenceCheckAt = nextConfidenceCheckAt,
                    confidenceLevel = confidenceLevel
                )
            }
        )

        return BusinessPartnerSearchResultDto(
            identifiers = identifiers,
            states = states,
            legalEntity = legalEntityDto,
            site = siteDto,
            address = addressDto,
            isParticipantData = legalEntity.isCatenaXMemberData
        )
    }
}