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
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.test.util.StringIgnoreComparator
import java.time.Instant

class ExpectedBusinessPartnerV6ResultFactory(
    private val testMetadata: TestMetadataV6
){

    fun buildExpectedLegalEntityCreateResponse(
        givenRequest: LegalEntityPartnerCreateRequest,
        givenBpnL: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness,
    ): LegalEntityPartnerCreateVerboseDto{
        return LegalEntityPartnerCreateVerboseDto(
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
        givenRequest: LegalEntityPartnerUpdateRequest,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        currentness: Instant = Instant.MIN,
        legalEntityCreatedAt: Instant = currentness,
        legalEntityUpdatedAt: Instant = currentness,
        addressCreatedAt: Instant = currentness,
        addressUpdatedAt: Instant = currentness
    ): LegalEntityPartnerCreateVerboseDto{
        return LegalEntityPartnerCreateVerboseDto(
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
        givenCreateResponse: LegalEntityPartnerCreateVerboseDto
    ): LegalEntityWithLegalAddressVerboseDto{
        return LegalEntityWithLegalAddressVerboseDto(legalEntity = givenCreateResponse.legalEntity, legalAddress = givenCreateResponse.legalAddress)
    }

    fun buildExpectedSiteCreateResponse(
        givenRequest: SitePartnerCreateRequest,
        legalEntityParent: LegalEntityPartnerCreateVerboseDto,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        addressCreatedAt: Instant = siteCreatedAt,
        addressUpdatedAt: Instant = siteCreatedAt
    ): SitePartnerCreateVerboseDto{
        return SitePartnerCreateVerboseDto(
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
        givenSiteRequest: SiteCreateRequestWithLegalAddressAsMain,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDto,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt,
        index: Int = 0
    ): SitePartnerCreateVerboseDto{
        return SitePartnerCreateVerboseDto(
            site = with(givenSiteRequest){
                SiteVerboseDto(
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
        givenRequest: SitePartnerUpdateRequest,
        givenSite: SitePartnerCreateVerboseDto,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDto,
        siteUpdatedAt: Instant = Instant.MIN,
        mainAddressUpdatedAt: Instant = siteUpdatedAt
    ): SitePartnerCreateVerboseDto{
        return SitePartnerCreateVerboseDto(
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
        givenSiteCreateResponse: SitePartnerCreateVerboseDto
    ): SiteWithMainAddressVerboseDto{
        return SiteWithMainAddressVerboseDto(
            site = givenSiteCreateResponse.site,
            mainAddress = givenSiteCreateResponse.mainAddress
        )
    }

    fun buildExpectedAdditionalAddressCreateResponse(
        givenRequest: AddressPartnerCreateRequest,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDto,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseDto{
        return AddressPartnerCreateVerboseDto(
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
        givenRequest: AddressPartnerCreateRequest,
        givenSite: SitePartnerCreateVerboseDto,
        givenBpnA: String = StringIgnoreComparator.IGNORE_STRING,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): AddressPartnerCreateVerboseDto{
        return AddressPartnerCreateVerboseDto(
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
        givenRequest: AddressPartnerUpdateRequest,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto{
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
        givenRequest: AddressPartnerUpdateRequest,
        givenLegalEntity: LegalEntityPartnerCreateVerboseDto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto{
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
        givenRequest: AddressPartnerUpdateRequest,
        givenSite: SitePartnerCreateVerboseDto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto{
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
        givenRequest: AddressPartnerUpdateRequest,
        givenSite: SitePartnerCreateVerboseDto,
        createdAt: Instant = Instant.MIN,
        updatedAt: Instant = createdAt
    ): LogisticAddressVerboseDto{
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
                confidenceCriteria = mapToExpectedConfidence(confidenceCriteria),
                addressType = addressType
            )
        }
    }

    fun mapToExpectedResult(givenRequest: LegalEntityIdentifierDto): LegalEntityIdentifierVerboseDto {
        val identifierType = testMetadata.legalEntityIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Legal Entity identifier with Key '${givenRequest.type}' is not expected")
        return LegalEntityIdentifierVerboseDto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name),
            issuingBody = givenRequest.issuingBody
        )
    }

    fun mapToExpectedResult(givenRequest: AddressIdentifierDto): AddressIdentifierVerboseDto {
        val identifierType = testMetadata.addressIdentifierTypes.find { givenRequest.type == it.technicalKey }
            ?: throw IllegalArgumentException("Address identifier with Key '${givenRequest.type}' is not expected")
        return AddressIdentifierVerboseDto(
            value = givenRequest.value,
            typeVerbose = TypeKeyNameVerboseDto(identifierType.technicalKey, identifierType.name)
        )
    }

    fun mapToExpectedResult(givenRequest: LegalEntityStateDto): LegalEntityStateVerboseDto {
        return with(givenRequest) {
            LegalEntityStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: AddressStateDto): AddressStateVerboseDto {
        return with(givenRequest) {
            AddressStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: SiteStateDto): SiteStateVerboseDto {
        return with(givenRequest) {
            SiteStateVerboseDto(validFrom = validFrom, validTo = validTo, typeVerbose = TypeKeyNameVerboseDto(type, type.getTypeName()))
        }
    }

    fun mapToExpectedResult(givenRequest: PhysicalPostalAddressDto): PhysicalPostalAddressVerboseDto {
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

    fun mapToExpectedResult(givenRequest: AlternativePostalAddressDto): AlternativePostalAddressVerboseDto {
        return with(givenRequest) {
            AlternativePostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates,
                countryVerbose = TypeKeyNameVerboseDto(country, country.getName()),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { mapToExpectedResult(it) },
                postalCode, city, deliveryServiceType, deliveryServiceQualifier, deliveryServiceNumber
            )
        }
    }

    fun mapToExpectedResult(givenAdminAreaCode: String): RegionDto {
        return with(testMetadata.adminAreas.find { it.code == givenAdminAreaCode }!!) {
            RegionDto(countryCode, code, name)
        }
    }

    private fun LegalEntityDto.mapToExpectedVerbose(
        givenBpnL: String,
        currentness: Instant,
        legalEntityCreatedAt: Instant,
        legalEntityUpdatedAt: Instant
    ): LegalEntityVerboseDto{
        return LegalEntityVerboseDto(
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

    private fun SiteDto.mapToExpectedVerbose(
        isCatenaXMemberData: Boolean,
        bpnLParent: String,
        givenBpnS: String = StringIgnoreComparator.IGNORE_STRING,
        siteCreatedAt: Instant = Instant.MIN,
        siteUpdatedAt: Instant = siteCreatedAt
    ): SiteVerboseDto{
        return SiteVerboseDto(
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

    private fun mapToExpectedConfidence(confidenceCriteria: ConfidenceCriteriaDto, numberOfSharingMembers: Int = 0): ConfidenceCriteriaDto{
        return confidenceCriteria.copy(numberOfSharingMembers = numberOfSharingMembers)
    }
}