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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator
import java.time.Instant

/**
 * This class contains functionality for creating expected results for business partner data
 */
class ExpectedBusinessPartnerResultFactory(
    expectedMetadata: TestMetadata
) {
    private val expectedAdminAreasLevel1: Collection<CountrySubdivisionDto> = expectedMetadata.adminAreas
    private val expectedLegalForms: Collection<LegalFormDto> = expectedMetadata.legalForms
    private val expectedLegalEntityIdentifierTypes: Collection<IdentifierTypeDto> = expectedMetadata.legalEntityIdentifierTypes
    private val expectedAddressIdentifierTypes: Collection<IdentifierTypeDto> = expectedMetadata.addressIdentifierTypes

    fun mapToExpectedLegalEntity(
        givenRequest: LegalEntityPartnerCreateRequest,
        givenBpnL: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness,
    ): LegalEntityWithLegalAddressVerboseDto {
        return LegalEntityWithLegalAddressVerboseDto(
            legalEntity = with(givenRequest.legalEntity) {
                LegalEntityVerboseDto(
                    bpnl = givenBpnL,
                    legalName = legalName,
                    legalShortName = legalShortName,
                    legalFormVerbose = legalForm?.let { lf ->
                        expectedLegalForms.find { lf == it.technicalKey }
                            ?: throw IllegalArgumentException("Legal Form with Key '$lf' is not expected")
                    },
                    identifiers = identifiers.map { mapToExpectedResult(it) },
                    states = states.map { mapToExpectedResult(it) },
                    relations = emptyList(),
                    currentness = currentness,
                    confidenceCriteria = confidenceCriteria,
                    isCatenaXMemberData = isCatenaXMemberData,
                    createdAt = legalEntityCreatedAt,
                    updatedAt = legalEntityUpdatedAt
                )
            },
            legalAddress = mapToExpectedResult(
                givenRequest.legalAddress,
                givenBpnA,
                givenBpnL,
                null,
                AddressType.LegalAddress,
                givenRequest.legalEntity.isCatenaXMemberData,
                addressCreatedAt,
                addressUpdatedAt
            )
        )
    }

    fun mapToExpectedSites(
        hierarchy: LegalEntityHierarchy
    ): List<SiteWithMainAddressVerboseDto> {
        return hierarchy.getAllSites().map { mapToExpectedSite(it, hierarchy.legalEntity.legalEntity.isCatenaXMemberData) }
    }

    fun mapToExpectedSite(
        givenRequest: SitePartnerCreateRequest,
        isCatenaXMemberData: Boolean,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt
    ): SiteWithMainAddressVerboseDto {
        return SiteWithMainAddressVerboseDto(
            site = with(givenRequest.site) {
                SiteVerboseDto(
                    bpns = givenBpnS,
                    name = name,
                    states = states.map { mapToExpectedResult(it) },
                    isCatenaXMemberData = isCatenaXMemberData,
                    bpnLegalEntity = givenRequest.bpnlParent,
                    createdAt = siteCreatedAt,
                    updatedAt = siteUpdatedAt,
                    confidenceCriteria = confidenceCriteria
                )
            },
            mainAddress = mapToExpectedResult(
                givenRequest.site.mainAddress,
                givenBpnA,
                null,
                givenBpnS,
                AddressType.SiteMainAddress,
                isCatenaXMemberData,
                addressCreatedAt,
                addressUpdatedAt
            )
        )
    }

    fun mapToExpectedAddresses(
        hierarchy: LegalEntityHierarchy
    ): List<LogisticAddressVerboseDto> {
        return listOf(mapToExpectedAddress(hierarchy.legalEntity))
            .plus(hierarchy.getAllSites().map { mapToExpectedAddress(it, hierarchy.legalEntity.legalEntity.isCatenaXMemberData) })
            .plus(hierarchy.getAllAddresses().map { mapToExpectedAddress(it, hierarchy.legalEntity.legalEntity.isCatenaXMemberData) })
    }

    fun mapToExpectedAddress(
        givenRequest: LegalEntityPartnerCreateRequest,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        bpnLegalEntity: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto {
        return mapToExpectedResult(
            givenRequest = givenRequest.legalAddress,
            givenBpnA = givenBpnA,
            bpnLegalEntity = bpnLegalEntity,
            bpnSite = null,
            addressType = AddressType.LegalAddress,
            isCatenaXMemberData = givenRequest.legalEntity.isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun mapToExpectedAddress(
        givenRequest: SitePartnerCreateRequest,
        isCatenaXMemberData: Boolean,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        bpnSite: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto {
        return mapToExpectedResult(
            givenRequest = givenRequest.site.mainAddress,
            givenBpnA = givenBpnA,
            bpnLegalEntity = null,
            bpnSite = bpnSite,
            addressType = AddressType.SiteMainAddress,
            isCatenaXMemberData = isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun mapToExpectedAddress(
        givenRequest: AddressPartnerCreateRequest,
        isCatenaXMemberData: Boolean,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto {
        return mapToExpectedResult(
            givenRequest = givenRequest.address,
            givenBpnA = givenBpnA,
            bpnLegalEntity = givenRequest.bpnParent.takeIf { it.startsWith("BPNL") },
            bpnSite = givenRequest.bpnParent.takeIf { it.startsWith("BPNS") },
            addressType = AddressType.AdditionalAddress,
            isCatenaXMemberData = isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun mapToExpectedResult(
        givenRequest: LogisticAddressDto,
        givenBpnA: String,
        bpnLegalEntity: String?,
        bpnSite: String?,
        addressType: AddressType,
        isCatenaXMemberData: Boolean,
        createdAt: Instant,
        updatedAt: Instant
    ): LogisticAddressVerboseDto {
        return with(givenRequest) {
            LogisticAddressVerboseDto(
                bpna = givenBpnA,
                name = name,
                states = states.map { mapToExpectedResult(it) },
                identifiers = identifiers.map { mapToExpectedResult(it) },
                physicalPostalAddress = mapToExpectedResult(physicalPostalAddress),
                alternativePostalAddress = alternativePostalAddress?.let { mapToExpectedResult(it) },
                bpnLegalEntity = bpnLegalEntity,
                bpnSite = bpnSite,
                isCatenaXMemberData = isCatenaXMemberData,
                createdAt = createdAt,
                updatedAt = updatedAt,
                confidenceCriteria = confidenceCriteria,
                addressType = addressType
            )
        }
    }

    private fun mapToExpectedResult(givenRequest: LegalEntityIdentifierDto): LegalEntityIdentifierVerboseDto {
        val identifierType = expectedLegalEntityIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${givenRequest.type}' is not expected")
        return LegalEntityIdentifierVerboseDto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = givenRequest.issuingBody
        )
    }

    private fun mapToExpectedResult(givenRequest: AddressIdentifierDto): AddressIdentifierVerboseDto {
        val identifierType = expectedAddressIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${givenRequest.type}' is not expected")
        return AddressIdentifierVerboseDto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    private fun mapToExpectedResult(givenRequest: LegalEntityStateDto): LegalEntityStateVerboseDto {
        return with(givenRequest) {
            LegalEntityStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: AddressStateDto): AddressStateVerboseDto {
        return with(givenRequest) {
            AddressStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: SiteStateDto): SiteStateVerboseDto {
        return with(givenRequest) {
            SiteStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: PhysicalPostalAddressDto): PhysicalPostalAddressVerboseDto {
        return with(givenRequest) {
            PhysicalPostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                administrativeAreaLevel2,
                administrativeAreaLevel3,
                postalCode,
                city,
                district,
                street,
                companyPostalCode,
                industrialZone,
                building,
                floor,
                door,
                taxJurisdictionCode
            )
        }
    }

    private fun mapToExpectedResult(givenRequest: AlternativePostalAddressDto): AlternativePostalAddressVerboseDto {
        return with(givenRequest) {
            AlternativePostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    private fun mapToExpectedResult(givenAdminAreaCode: String): RegionDto {
        return with(expectedAdminAreasLevel1.find { it.code == givenAdminAreaCode }!!) {
            RegionDto(countryCode, code, name)
        }
    }
}