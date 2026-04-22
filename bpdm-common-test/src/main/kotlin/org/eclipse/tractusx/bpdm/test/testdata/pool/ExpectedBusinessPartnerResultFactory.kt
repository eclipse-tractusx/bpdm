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
    expectedMetadata: TestMetadataV7
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
        isMaintainConfidences: Boolean = false
    ): LegalEntityWithLegalAddressVerboseDto {
        return LegalEntityWithLegalAddressVerboseDto(
            header = with(givenRequest.legalEntity.header) {
                LegalEntityHeaderVerboseDto(
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
                    confidenceCriteria = if(isMaintainConfidences) confidenceCriteria else mapToExpectedConfidence(confidenceCriteria),
                    isParticipantData = isParticipantData,
                    createdAt = legalEntityCreatedAt,
                    updatedAt = legalEntityUpdatedAt
                )
            },
            legalAddress = mapToExpectedResult(
                givenRequest.legalEntity.legalAddress,
                givenBpnA,
                givenBpnL,
                null,
                AddressType.LegalAddress,
                givenRequest.legalEntity.header.isParticipantData,
                addressCreatedAt,
                addressUpdatedAt
            ),
            scriptVariants = givenRequest.legalEntity.scriptVariants
        )
    }

    fun mapToExpectedSite(
        givenRequest: SitePartnerCreateRequest,
        isCatenaXMemberData: Boolean,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt,
        isMaintainConfidences: Boolean = false
    ): SiteWithMainAddressVerboseDto {
        return SiteWithMainAddressVerboseDto(
            site = with(givenRequest.site) {
                SiteVerboseDto(
                    bpns = givenBpnS,
                    name = name,
                    states = states.map { mapToExpectedResult(it) },
                    isParticipantData = isCatenaXMemberData,
                    bpnLegalEntity = givenRequest.bpnlParent,
                    scriptVariants = scriptVariants,
                    createdAt = siteCreatedAt,
                    updatedAt = siteUpdatedAt,
                    confidenceCriteria = if(isMaintainConfidences) confidenceCriteria else mapToExpectedConfidence(confidenceCriteria, 1)
                )
            },
            mainAddress = mapToExpectedResult(
                givenRequest.site.mainAddress,
                givenBpnA,
                givenRequest.bpnlParent,
                givenBpnS,
                AddressType.SiteMainAddress,
                isCatenaXMemberData,
                addressCreatedAt,
                addressUpdatedAt
            )
        )
    }

    fun mapToExpectedAdditionalAddress(
        givenRequest: AddressPartnerCreateRequest,
        isCatenaXMemberData: Boolean,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        bpnLegalEntityOverwrite: String? = null,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt,
        isMaintainConfidences: Boolean = false
    ): LogisticAddressInvariantVerboseDto {
        return mapToExpectedResult(
            givenRequest = givenRequest.address,
            givenBpnA = givenBpnA,
            bpnLegalEntity = bpnLegalEntityOverwrite ?: givenRequest.bpnParent.takeIf { it.startsWith("BPNL") },
            bpnSite = givenRequest.bpnParent.takeIf { it.startsWith("BPNS") },
            addressType = AddressType.AdditionalAddress,
            isCatenaXMemberData = isCatenaXMemberData,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isMaintainConfidences = isMaintainConfidences
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
        updatedAt: Instant,
        isMaintainConfidences: Boolean = false
    ): LogisticAddressInvariantVerboseDto {
        return with(givenRequest) {
            LogisticAddressInvariantVerboseDto(
                bpna = givenBpnA,
                name = name,
                states = states.map { mapToExpectedResult(it) },
                identifiers = identifiers.map { mapToExpectedResult(it) },
                physicalPostalAddress = mapToExpectedResult(physicalPostalAddress),
                alternativePostalAddress = alternativePostalAddress?.let { mapToExpectedResult(it) },
                bpnLegalEntity = bpnLegalEntity,
                bpnSite = bpnSite,
                isParticipantData = isCatenaXMemberData,
                createdAt = createdAt,
                updatedAt = updatedAt,
                confidenceCriteria = if(isMaintainConfidences) confidenceCriteria else mapToExpectedConfidence(confidenceCriteria),
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

    private fun mapToExpectedConfidence(confidenceCriteria: ConfidenceCriteriaDto, numberOfSharingMembers: Int = 0): ConfidenceCriteriaDto{
        return confidenceCriteria.copy(numberOfSharingMembers = numberOfSharingMembers)
    }
}