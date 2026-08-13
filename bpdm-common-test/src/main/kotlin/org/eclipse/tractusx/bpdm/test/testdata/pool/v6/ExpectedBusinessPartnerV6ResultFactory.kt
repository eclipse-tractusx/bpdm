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
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator
import java.time.Instant

class ExpectedBusinessPartnerV6ResultFactory(
    private val testMetadata: TestMetadataV6
){

    fun buildExpectedLegalEntityCreateResponse(
        givenRequest: LegalEntityPartnerCreateRequestV6,
        givenBpnL: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness,
    ): LegalEntityPartnerCreateVerboseDtoV6{
        return LegalEntityPartnerCreateVerboseDtoV6(
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
        givenRequest: LegalEntityPartnerUpdateRequestV6,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness
    ): LegalEntityPartnerCreateVerboseDtoV6{
        return LegalEntityPartnerCreateVerboseDtoV6(
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
        givenCreateResponse: LegalEntityPartnerCreateVerboseDtoV6
    ): LegalEntityWithLegalAddressVerboseDtoV6{
        return LegalEntityWithLegalAddressVerboseDtoV6(legalEntity = givenCreateResponse.legalEntity, legalAddress = givenCreateResponse.legalAddress)
    }

    fun buildExpectedSiteCreateResponse(
        givenRequest: SitePartnerCreateRequestV6,
        legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt
    ): SitePartnerCreateVerboseDtoV6{
        return SitePartnerCreateVerboseDtoV6(
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
        givenSiteRequest: SiteCreateRequestWithLegalAddressAsMainV6,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDtoV6,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        index: Int = 0
    ): SitePartnerCreateVerboseDtoV6{
        return SitePartnerCreateVerboseDtoV6(
            site = with(givenSiteRequest){
                SiteVerboseDtoV6(
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
        givenRequest: SitePartnerUpdateRequestV6,
        givenSite: SitePartnerCreateVerboseDtoV6,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDtoV6,
        siteUpdatedAt: Instant = Instant.MIN,
        mainAddressUpdatedAt: Instant = siteUpdatedAt
    ): SitePartnerCreateVerboseDtoV6{
        return SitePartnerCreateVerboseDtoV6(
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
        givenSiteCreateResponse: SitePartnerCreateVerboseDtoV6
    ): SiteWithMainAddressVerboseDtoV6{
        return SiteWithMainAddressVerboseDtoV6(
            site = givenSiteCreateResponse.site,
            mainAddress = givenSiteCreateResponse.mainAddress
        )
    }

    fun buildExpectedAdditionalAddressCreateResponse(
        givenRequest: AddressPartnerCreateRequestV6,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDtoV6,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseDtoV6{
        return AddressPartnerCreateVerboseDtoV6(
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
        givenRequest: AddressPartnerCreateRequestV6,
        givenSite: SitePartnerCreateVerboseDtoV6,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseDtoV6{
        return AddressPartnerCreateVerboseDtoV6(
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
        givenRequest: AddressPartnerUpdateRequestV6,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDtoV6,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDtoV6{
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
        givenRequest: AddressPartnerUpdateRequestV6,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDtoV6,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDtoV6{
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
        givenRequest: AddressPartnerUpdateRequestV6,
        givenSite: SitePartnerCreateVerboseDtoV6,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDtoV6{
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
        givenRequest: AddressPartnerUpdateRequestV6,
        givenSite: SitePartnerCreateVerboseDtoV6,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDtoV6{
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
        givenRequest: LogisticAddressDtoV6,
        givenBpnA: String,
        bpnLegalEntity: String?,
        bpnSite: String?,
        addressType: AddressType,
        isCatenaXMemberData: Boolean,
        createdAt: Instant,
        updatedAt: Instant
    ): LogisticAddressVerboseDtoV6 {
        return with(givenRequest) {
            LogisticAddressVerboseDtoV6(
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

    fun mapToExpectedResult(givenRequest: LegalEntityIdentifierDtoV6): LegalEntityIdentifierVerboseDtoV6 {
        val identifierType = testMetadata.legalEntityIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${givenRequest.type}' is not expected")
        return LegalEntityIdentifierVerboseDtoV6(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = givenRequest.issuingBody
        )
    }

    fun mapToExpectedResult(givenRequest: AddressIdentifierDtoV6): AddressIdentifierVerboseDtoV6 {
        val identifierType = testMetadata.addressIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${givenRequest.type}' is not expected")
        return AddressIdentifierVerboseDtoV6(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    fun mapToExpectedResult(givenRequest: LegalEntityStateDtoV6): LegalEntityStateVerboseDtoV6 {
        return with(givenRequest) {
            LegalEntityStateVerboseDtoV6(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: AddressStateDtoV6): AddressStateVerboseDtoV6 {
        return with(givenRequest) {
            AddressStateVerboseDtoV6(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: SiteStateDtoV6): SiteStateVerboseDtoV6 {
        return with(givenRequest) {
            SiteStateVerboseDtoV6(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: PhysicalPostalAddressDtoV6): PhysicalPostalAddressVerboseDtoV6 {
        return with(givenRequest) {
            PhysicalPostalAddressVerboseDtoV6(
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

    fun mapToExpectedResult(givenRequest: AlternativePostalAddressDtoV6): AlternativePostalAddressVerboseDtoV6 {
        return with(givenRequest) {
            AlternativePostalAddressVerboseDtoV6(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    fun mapToExpectedResult(givenAdminAreaCode: String): RegionDtoV6 {
        return with(testMetadata.adminAreas.find { it.code == givenAdminAreaCode }!!) {
            RegionDtoV6(countryCode, code, name)
        }
    }

    private fun LegalEntityDtoV6.mapToExpectedVerbose(
        givenBpnL: String,
        currentness: Instant,
        legalEntityCreatedAt: Instant,
        legalEntityUpdatedAt: Instant
    ): LegalEntityVerboseDtoV6{
        return LegalEntityVerboseDtoV6(
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

    private fun SiteDtoV6.mapToExpectedVerbose(
        isCatenaXMemberData: Boolean,
        bpnLParent: String,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt
    ): SiteVerboseDtoV6{
        return SiteVerboseDtoV6(
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

    private fun mapToExpectedConfidence(confidenceCriteria: ConfidenceCriteriaDtoV6, numberOfSharingMembers: Int = 0): ConfidenceCriteriaDtoV6{
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
    private fun deriveConfidenceLevel(confidenceCriteria: ConfidenceCriteriaDtoV6, numberOfSharingMembers: Int): Int {
        val sharedByOwnerLevel = if (confidenceCriteria.sharedByOwner) 5 else 0
        val checkedByExternalDataSourceLevel = if (confidenceCriteria.checkedByExternalDataSource) 3 else 0
        val numberOfSharingMembersLevel = if (numberOfSharingMembers >= 3) 1 else 0
        return sharedByOwnerLevel + checkedByExternalDataSourceLevel + numberOfSharingMembersLevel
    }
}