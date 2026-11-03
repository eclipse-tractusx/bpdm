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
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountrySubdivisionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.CountryV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierTypeV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.LegalFormV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.RegionV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator
import java.time.Instant

/**
 * This class contains functionality for creating expected results for business partner data
 */
class ExpectedBusinessPartnerResultFactory(
    expectedMetadata: TestMetadataV7
) {
    private val expectedAdminAreasLevel1: Collection<CountrySubdivisionV7Dto> = expectedMetadata.adminAreas
    private val expectedLegalForms: Collection<LegalFormV7Dto> = expectedMetadata.legalForms
    private val expectedLegalEntityIdentifierTypes: Collection<IdentifierTypeV7Dto> = expectedMetadata.legalEntityIdentifierTypes
    private val expectedAddressIdentifierTypes: Collection<IdentifierTypeV7Dto> = expectedMetadata.addressIdentifierTypes
    private val expectedCountries: List<CountryV7Dto> = expectedMetadata.countries

    fun mapToExpectedLegalEntity(
        givenRequest: LegalEntityPartnerCreateV7Request,
        givenBpnL: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.EPOCH,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness,
        isMaintainConfidences: Boolean = false
    ): LegalEntityWithLegalAddressVerboseV7Dto {
        return LegalEntityWithLegalAddressVerboseV7Dto(
            header = with(givenRequest.legalEntity.header) {
                LegalEntityHeaderVerboseV7Dto(
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
                    updatedAt = legalEntityUpdatedAt,
                    ownershipUltimate = false,
                    ultimateOwnerBpnl = null
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
        givenRequest: SitePartnerCreateV7Request,
        isCatenaXMemberData: Boolean,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.EPOCH,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt,
        isMaintainConfidences: Boolean = false
    ): SiteWithMainAddressVerboseV7Dto {
        return SiteWithMainAddressVerboseV7Dto(
            site = with(givenRequest.site) {
                SiteVerboseV7Dto(
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
        givenRequest: AddressPartnerCreateV7Request,
        isCatenaXMemberData: Boolean,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        bpnLegalEntityOverwrite: String? = null,
        createdAt: Instant = Instant.EPOCH,
        updatedAt: Instant = createdAt,
        isMaintainConfidences: Boolean = false
    ): LogisticAddressInvariantVerboseV7Dto {
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
        givenRequest: LogisticAddressV7Dto,
        givenBpnA: String,
        bpnLegalEntity: String?,
        bpnSite: String?,
        addressType: AddressType,
        isCatenaXMemberData: Boolean,
        createdAt: Instant,
        updatedAt: Instant,
        isMaintainConfidences: Boolean = false
    ): LogisticAddressInvariantVerboseV7Dto {
        return with(givenRequest) {
            LogisticAddressInvariantVerboseV7Dto(
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

    private fun mapToExpectedResult(givenRequest: LegalEntityIdentifierV7Dto): LegalEntityIdentifierVerboseV7Dto {
        val identifierType = expectedLegalEntityIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${givenRequest.type}' is not expected")
        return LegalEntityIdentifierVerboseV7Dto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = givenRequest.issuingBody
        )
    }

    private fun mapToExpectedResult(givenRequest: AddressIdentifierV7Dto): AddressIdentifierVerboseV7Dto {
        val identifierType = expectedAddressIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${givenRequest.type}' is not expected")
        return AddressIdentifierVerboseV7Dto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    private fun mapToExpectedResult(givenRequest: LegalEntityStateV7Dto): LegalEntityStateVerboseV7Dto {
        return with(givenRequest) {
            LegalEntityStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: AddressStateV7Dto): AddressStateVerboseV7Dto {
        return with(givenRequest) {
            AddressStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: SiteStateV7Dto): SiteStateVerboseV7Dto {
        return with(givenRequest) {
            SiteStateVerboseV7Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    private fun mapToExpectedResult(givenRequest: PhysicalPostalAddressV7Dto): PhysicalPostalAddressVerboseV7Dto {
        return with(givenRequest) {
            PhysicalPostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = expectedCountries.toCountryVerbose(country),
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

    private fun mapToExpectedResult(givenRequest: AlternativePostalAddressV7Dto): AlternativePostalAddressVerboseV7Dto {
        return with(givenRequest) {
            AlternativePostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = expectedCountries.toCountryVerbose(country),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    private fun mapToExpectedResult(givenAdminAreaCode: String): RegionV7Dto {
        return with(expectedAdminAreasLevel1.find { it.code == givenAdminAreaCode }!!) {
            RegionV7Dto(countryCode, code, name)
        }
    }

    private fun mapToExpectedConfidence(confidenceCriteria: ConfidenceCriteriaV7Dto, numberOfSharingMembers: Int = 0): ConfidenceCriteriaV7Dto{
        return confidenceCriteria.copy(numberOfSharingMembers = numberOfSharingMembers)
    }
}