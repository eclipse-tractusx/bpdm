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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v6

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityIdentifierV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.RegionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator
import java.time.Instant

class ExpectedBusinessPartnerV6ResultFactory(
    private val testMetadata: TestMetadataV6
){

    fun buildExpectedLegalEntityCreateResponse(
        givenRequest: LegalEntityPartnerCreateV6Request,
        givenBpnL: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness,
    ): LegalEntityPartnerCreateVerboseV6Dto{
        return LegalEntityPartnerCreateVerboseV6Dto(
            givenRequest.legalEntity.mapToExpectedVerbose(givenBpnL, currentness, legalEntityCreatedAt, legalEntityUpdatedAt),
            buildExpectedAddressResponse(
                givenRequest.legalAddress,
                givenBpnA,
                givenBpnL,
                null,
                AddressType.LegalAddress,
                givenRequest.legalEntity.isCatenaXMemberData,
                addressCreatedAt,
                addressUpdatedAt
            ),
            givenRequest.index
        )
    }

    fun buildExpectedLegalEntityUpdateResponse(
        givenRequest: LegalEntityPartnerUpdateV6Request,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness
    ): LegalEntityPartnerCreateVerboseV6Dto{
        return LegalEntityPartnerCreateVerboseV6Dto(
            givenRequest.legalEntity.mapToExpectedVerbose(givenRequest.bpnl, currentness, legalEntityCreatedAt, legalEntityUpdatedAt),
            buildExpectedAddressResponse(
                givenRequest.legalAddress,
                givenBpnA,
                givenRequest.bpnl,
                null,
                AddressType.LegalAddress,
                givenRequest.legalEntity.isCatenaXMemberData,
                addressCreatedAt,
                addressUpdatedAt
            ),
            givenRequest.bpnl
        )
    }

    fun buildExpectedLegalEntitySearchResponse(
        givenCreateResponse: LegalEntityPartnerCreateVerboseV6Dto
    ): LegalEntityWithLegalAddressVerboseV6Dto{
        return LegalEntityWithLegalAddressVerboseV6Dto(legalEntity = givenCreateResponse.legalEntity, legalAddress = givenCreateResponse.legalAddress)
    }

    fun buildExpectedSiteCreateResponse(
        givenRequest: SitePartnerCreateV6Request,
        legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt
    ): SitePartnerCreateVerboseV6Dto{
        return SitePartnerCreateVerboseV6Dto(
            site = givenRequest.site.mapToExpectedVerbose(
                isCatenaXMemberData = legalEntityParent.legalEntity.isCatenaXMemberData,
                bpnLParent = givenRequest.bpnlParent,
                givenBpnS = givenBpnS,
                siteCreatedAt = siteCreatedAt,
                siteUpdatedAt = siteUpdatedAt
            ),
            mainAddress =  buildExpectedAddressResponse(
                givenRequest = givenRequest.site.mainAddress,
                givenBpnA = givenBpnA,
                bpnLegalEntity = givenRequest.bpnlParent,
                bpnSite = givenBpnS,
                addressType = AddressType.SiteMainAddress,
                isCatenaXMemberData = legalEntityParent.legalEntity.isCatenaXMemberData,
                createdAt = addressCreatedAt,
                updatedAt = addressUpdatedAt
            ),
            index = givenRequest.index
        )
    }

    fun buildExpectedLegalAddressSiteCreateResponse(
        givenSiteRequest: SiteCreateWithLegalAddressAsMainV6Request,
        givenLegalEntity: LegalEntityPartnerCreateVerboseV6Dto,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        index: Int = 0
    ): SitePartnerCreateVerboseV6Dto{
        return SitePartnerCreateVerboseV6Dto(
            site = with(givenSiteRequest){
                SiteVerboseV6Dto(
                    givenBpnS,
                    name,
                    states.map { mapToExpectedResult(it) },
                    givenLegalEntity.legalEntity.isCatenaXMemberData,
                    givenLegalEntity.legalEntity.bpnl,
                    siteCreatedAt,
                    siteUpdatedAt,
                    mapToExpectedConfidence(confidenceCriteria, 1)
                )
            },
            mainAddress =  givenLegalEntity.legalAddress.copy(addressType = AddressType.LegalAndSiteMainAddress),
            index = index.toString()
        )
    }

    fun buildExpectedSiteUpdateResponse(
        givenRequest: SitePartnerUpdateV6Request,
        givenSite: SitePartnerCreateVerboseV6Dto,
        givenLegalEntity: LegalEntityPartnerCreateVerboseV6Dto,
        siteUpdatedAt: Instant = Instant.MIN,
        mainAddressUpdatedAt: Instant = siteUpdatedAt
    ): SitePartnerCreateVerboseV6Dto{
        return SitePartnerCreateVerboseV6Dto(
            site = givenRequest.site.mapToExpectedVerbose(
                isCatenaXMemberData = givenLegalEntity.legalEntity.isCatenaXMemberData,
                bpnLParent = givenLegalEntity.legalEntity.bpnl,
                givenBpnS = givenRequest.bpns,
                siteCreatedAt = givenSite.site.createdAt,
                siteUpdatedAt = siteUpdatedAt
            ),
            mainAddress =  buildExpectedAddressResponse(
                givenRequest = givenRequest.site.mainAddress,
                givenBpnA = givenSite.mainAddress.bpna,
                bpnLegalEntity = givenLegalEntity.legalEntity.bpnl,
                bpnSite = givenRequest.bpns,
                addressType = AddressType.SiteMainAddress,
                isCatenaXMemberData = givenLegalEntity.legalEntity.isCatenaXMemberData,
                createdAt = givenSite.mainAddress.createdAt,
                updatedAt = mainAddressUpdatedAt
            ),
            index = givenRequest.bpns
        )
    }

    fun buildExpectedSiteSearchResponse(
        givenSiteCreateResponse: SitePartnerCreateVerboseV6Dto
    ): SiteWithMainAddressVerboseV6Dto{
        return SiteWithMainAddressVerboseV6Dto(
            site = givenSiteCreateResponse.site,
            mainAddress = givenSiteCreateResponse.mainAddress
        )
    }

    fun buildExpectedAdditionalAddressCreateResponse(
        givenRequest: AddressPartnerCreateV6Request,
        givenLegalEntity: LegalEntityPartnerCreateVerboseV6Dto,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseV6Dto{
        return AddressPartnerCreateVerboseV6Dto(
            address = buildExpectedAddressResponse(
                givenRequest.address,
                givenBpnA,
                givenLegalEntity.legalEntity.bpnl,
                null,
                AddressType.AdditionalAddress,
                givenLegalEntity.legalEntity.isCatenaXMemberData,
                createdAt,
                updatedAt
            ),
            index = givenRequest.index
        )
    }

    fun buildExpectedAdditionalAddressCreateResponse(
        givenRequest: AddressPartnerCreateV6Request,
        givenSite: SitePartnerCreateVerboseV6Dto,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseV6Dto{
        return AddressPartnerCreateVerboseV6Dto(
            address = buildExpectedAddressResponse(
                givenRequest.address,
                givenBpnA,
                givenSite.site.bpnLegalEntity,
                givenSite.site.bpns,
                AddressType.AdditionalAddress,
                givenSite.site.isCatenaXMemberData,
                createdAt,
                updatedAt
            ),
            index = givenRequest.index
        )
    }

    fun buildExpectedLegalAddressUpdateResponse(
        givenRequest: AddressPartnerUpdateV6Request,
        givenLegalEntity: LegalEntityPartnerCreateVerboseV6Dto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseV6Dto{
        return buildExpectedAddressResponse(
                givenRequest.address,
                givenRequest.bpna,
                givenLegalEntity.legalEntity.bpnl,
                null,
                givenLegalEntity.legalAddress.addressType!!,
                givenLegalEntity.legalEntity.isCatenaXMemberData,
                createdAt,
                updatedAt
            )
    }

    fun buildExpectedAdditionalAddressUpdateResponse(
        givenRequest: AddressPartnerUpdateV6Request,
        givenLegalEntity: LegalEntityPartnerCreateVerboseV6Dto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseV6Dto{
        return buildExpectedAddressResponse(
            givenRequest.address,
            givenRequest.bpna,
            givenLegalEntity.legalEntity.bpnl,
            null,
            AddressType.AdditionalAddress,
            givenLegalEntity.legalEntity.isCatenaXMemberData,
            createdAt,
            updatedAt
        )
    }

    fun buildExpectedMainAddressUpdateResponse(
        givenRequest: AddressPartnerUpdateV6Request,
        givenSite: SitePartnerCreateVerboseV6Dto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseV6Dto{
        return buildExpectedAddressResponse(
            givenRequest.address,
            givenRequest.bpna,
            givenSite.site.bpnLegalEntity,
            givenSite.site.bpns,
            givenSite.mainAddress.addressType!!,
            givenSite.site.isCatenaXMemberData,
            createdAt,
            updatedAt
        )
    }

    fun buildExpectedAdditionalAddressUpdateResponse(
        givenRequest: AddressPartnerUpdateV6Request,
        givenSite: SitePartnerCreateVerboseV6Dto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseV6Dto{
        return buildExpectedAddressResponse(
            givenRequest.address,
            givenRequest.bpna,
            givenSite.site.bpnLegalEntity,
            givenSite.site.bpns,
            AddressType.AdditionalAddress,
            givenSite.site.isCatenaXMemberData,
            createdAt,
            updatedAt
        )
    }

    private fun buildExpectedAddressResponse(
        givenRequest: LogisticAddressV6Dto,
        givenBpnA: String,
        bpnLegalEntity: String?,
        bpnSite: String?,
        addressType: AddressType,
        isCatenaXMemberData: Boolean,
        createdAt: Instant,
        updatedAt: Instant
    ): LogisticAddressVerboseV6Dto {
        return with(givenRequest) {
            LogisticAddressVerboseV6Dto(
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
                confidenceCriteria = mapToExpectedConfidence(confidenceCriteria),
                addressType = addressType
            )
        }
    }

    fun mapToExpectedResult(givenRequest: LegalEntityIdentifierV6Dto): LegalEntityIdentifierVerboseV6Dto {
        val identifierType = testMetadata.legalEntityIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${givenRequest.type}' is not expected")
        return LegalEntityIdentifierVerboseV6Dto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = givenRequest.issuingBody
        )
    }

    fun mapToExpectedResult(givenRequest: AddressIdentifierV6Dto): AddressIdentifierVerboseV6Dto {
        val identifierType = testMetadata.addressIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${givenRequest.type}' is not expected")
        return AddressIdentifierVerboseV6Dto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    fun mapToExpectedResult(givenRequest: LegalEntityStateV6Dto): LegalEntityStateVerboseV6Dto {
        return with(givenRequest) {
            LegalEntityStateVerboseV6Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: AddressStateV6Dto): AddressStateVerboseV6Dto {
        return with(givenRequest) {
            AddressStateVerboseV6Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: SiteStateV6Dto): SiteStateVerboseV6Dto {
        return with(givenRequest) {
            SiteStateVerboseV6Dto(validFrom = validFrom ?: BusinessStateDates.EARLIEST, validTo = validTo ?: BusinessStateDates.LATEST, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: PhysicalPostalAddressV6Dto): PhysicalPostalAddressVerboseV6Dto {
        return with(givenRequest) {
            PhysicalPostalAddressVerboseV6Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = testMetadata.countries.toCountryVerbose(country),
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

    fun mapToExpectedResult(givenRequest: AlternativePostalAddressV6Dto): AlternativePostalAddressVerboseV6Dto {
        return with(givenRequest) {
            AlternativePostalAddressVerboseV6Dto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = testMetadata.countries.toCountryVerbose(country),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    fun mapToExpectedResult(givenAdminAreaCode: String): RegionV6Dto {
        return with(testMetadata.adminAreas.find { it.code == givenAdminAreaCode }!!) {
            RegionV6Dto(countryCode, code, name)
        }
    }

    private fun LegalEntityV6Dto.mapToExpectedVerbose(
        givenBpnL: String,
        currentness: Instant,
        legalEntityCreatedAt: Instant,
        legalEntityUpdatedAt: Instant
    ): LegalEntityVerboseV6Dto{
        return LegalEntityVerboseV6Dto(
            bpnl = givenBpnL,
            legalName = legalName,
            legalShortName = legalShortName,
            legalFormVerbose = legalForm?.let { lf ->
                testMetadata.legalForms.find { lf == it.technicalKey }
                    ?: throw IllegalArgumentException("Legal Form with Key '$lf' is not expected")
            },
            identifiers = identifiers.map { mapToExpectedResult(it) },
            states = states.map { mapToExpectedResult(it) },
            relations = emptyList(),
            currentness = currentness,
            confidenceCriteria = mapToExpectedConfidence(confidenceCriteria),
            isCatenaXMemberData = isCatenaXMemberData,
            createdAt = legalEntityCreatedAt,
            updatedAt = legalEntityUpdatedAt
        )
    }

    private fun SiteV6Dto.mapToExpectedVerbose(
        isCatenaXMemberData: Boolean,
        bpnLParent: String,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt
    ): SiteVerboseV6Dto{
        return SiteVerboseV6Dto(
            bpns = givenBpnS,
            name = name,
            states = states.map { mapToExpectedResult(it) },
            isCatenaXMemberData = isCatenaXMemberData,
            bpnLegalEntity = bpnLParent,
            createdAt = siteCreatedAt,
            updatedAt = siteUpdatedAt,
            confidenceCriteria = mapToExpectedConfidence(confidenceCriteria, 1)
        )
    }

    private fun mapToExpectedConfidence(confidenceCriteria: ConfidenceCriteriaV6Dto, numberOfSharingMembers: Int = 0): ConfidenceCriteriaV6Dto{
        return confidenceCriteria.copy(
            numberOfSharingMembers = numberOfSharingMembers,
            // The Pool derives confidenceLevel from the flags and ignores the value sent in the request
            // (see ConfidenceCriteriaDb.confidenceLevel), so the expectation must derive it the same way
            // rather than echo the request's value.
            confidenceLevel = deriveConfidenceLevel(confidenceCriteria, numberOfSharingMembers)
        )
    }

    // Mirrors ConfidenceCriteriaDb.confidenceLevel in bpdm-pool (not reachable from this module, which
    // only depends on bpdm-pool-api).
    private fun deriveConfidenceLevel(confidenceCriteria: ConfidenceCriteriaV6Dto, numberOfSharingMembers: Int): Int {
        val sharedByOwnerLevel = if (confidenceCriteria.sharedByOwner) 5 else 0
        val checkedByExternalDataSourceLevel = if (confidenceCriteria.checkedByExternalDataSource) 3 else 0
        val numberOfSharingMembersLevel = if (numberOfSharingMembers >= 3) 1 else 0
        return sharedByOwnerLevel + checkedByExternalDataSourceLevel + numberOfSharingMembersLevel
    }
}