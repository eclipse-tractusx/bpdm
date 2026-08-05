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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDtoV6
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

class BusinessPartnerV6RequestFactory(
    testMetadata: TestMetadataV6
){
    private val availableLegalForms = testMetadata.legalForms.map { it.technicalKey }
    private val availableLegalEntityIdentifiers = testMetadata.legalEntityIdentifierTypes.map { it.technicalKey }
    private val availableAddressIdentifiers: Collection<String> =  testMetadata.addressIdentifierTypes.map { it.technicalKey }
    private val availableAdminAreas: Collection<String> = testMetadata.adminAreas.map { it.code }

    fun buildLegalEntityCreateRequest(seed: String): LegalEntityPartnerCreateRequestV6 {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateRequestV6(
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random),
            index = seed
        )
    }

    fun createLegalEntityUpdateRequest(seed: String, legalEntityToUpdate: LegalEntityPartnerCreateVerboseDtoV6): LegalEntityPartnerUpdateRequestV6 {
        return createLegalEntityUpdateRequest(seed, legalEntityToUpdate.legalEntity.bpnl)
    }

    fun createLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateRequestV6 {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateRequestV6(
            bpnl = bpnl,
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random)
        )
    }

    fun createLegalEntityDto(seed: String, random: Random =  Random(seed.hashCode().toLong())): LegalEntityDtoV6 {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityDtoV6(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = availableLegalForms.randomOrNull(random),
            identifiers = listOf(createLegalEntityIdentifier(seed, 0, random), createLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateDtoV6(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateDtoV6(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDtoV6(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 1,
                lastConfidenceCheckAt = timeStamp,
                nextConfidenceCheckAt = timeStamp.plusDays(7),
                confidenceLevel = 5
            ),
            isCatenaXMemberData = random.nextBoolean()
        )
    }

    fun createLegalEntityIdentifier(seed: String, index: Int, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierDtoV6{
        val idKey = availableLegalEntityIdentifiers.random(random)
        return LegalEntityIdentifierDtoV6("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6): SitePartnerCreateRequestV6 {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateRequestV6 {
        return SitePartnerCreateRequestV6(
            bpnlParent = bpnlParent,
            index = seed,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseDtoV6): SitePartnerUpdateRequestV6 {
        return SitePartnerUpdateRequestV6(
            bpns = siteToUpdate.site.bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, bpns: String): SitePartnerUpdateRequestV6 {
        return SitePartnerUpdateRequestV6(
            bpns = bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteDto(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteDtoV6 {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteDtoV6(
            name = "Site Name $seed",
            states = listOf(
                SiteStateDtoV6(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateDtoV6(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            mainAddress = createAddressDto(seed, random).withSharedByOwner(true),
            confidenceCriteria = ConfidenceCriteriaDtoV6(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6, random: Random = Random(seed.hashCode().toLong())):SiteCreateRequestWithLegalAddressAsMainV6 {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl, random)
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, bpnL: String, random: Random = Random(seed.hashCode().toLong())):SiteCreateRequestWithLegalAddressAsMainV6{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateRequestWithLegalAddressAsMainV6(
            name = "Site Name $seed",
            bpnLParent = bpnL,
            states = listOf(
                SiteStateDtoV6(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateDtoV6(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDtoV6(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6): AddressPartnerCreateRequestV6 {
        return buildAdditionalAddressCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildAdditionalAddressCreateRequest(seed: String, bpnParent: String): AddressPartnerCreateRequestV6 {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return AddressPartnerCreateRequestV6(
            bpnParent = bpnParent,
            index = seed,
            address = createAddressDto(seed, random)
        )
    }

    fun buildAddressUpdateRequest(seed: String, legalAddressToUpdate: LegalEntityPartnerCreateVerboseDtoV6): AddressPartnerUpdateRequestV6 {
        return buildAddressUpdateRequest(seed, legalAddressToUpdate.legalAddress.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, addressToUpdate: AddressPartnerCreateVerboseDtoV6): AddressPartnerUpdateRequestV6 {
        return buildAddressUpdateRequest(seed, addressToUpdate.address.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateRequestV6 {
        return AddressPartnerUpdateRequestV6(bpna, createAddressDto(seed, random))
    }

    fun createAddressDto(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressDtoV6 {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressDtoV6(
            name = "Address Name $seed",
            states = listOf(
                AddressStateDtoV6(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateDtoV6(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = (1 ..2.coerceAtMost(availableAddressIdentifiers.size)).map { buildAddressIdentifier(seed, it, random) },
            physicalPostalAddress = PhysicalPostalAddressDtoV6(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetDtoV6(
                    name = "Street Name $seed",
                    houseNumber = "House Number $seed",
                    houseNumberSupplement = "House Number Supplement $seed",
                    milestone = "Milestone $seed",
                    direction = "Direction $seed",
                    namePrefix = "Name Prefix $seed",
                    nameSuffix = "Name Suffix $seed",
                    additionalNamePrefix = "Additional Name Prefix $seed",
                    additionalNameSuffix = "Additional Name Suffix $seed"
                ),
                companyPostalCode = "Company Postal Code $seed",
                industrialZone = "Industrial Zone $seed",
                building = "Building $seed",
                floor = "Floor $seed",
                door = "Door $seed",
                taxJurisdictionCode = "Tax Jurisdiction Code $seed"
            ),
            alternativePostalAddress = AlternativePostalAddressDtoV6(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                postalCode = "Postal Code $seed",
                city = "City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            confidenceCriteria = ConfidenceCriteriaDtoV6(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 0
            )
        )
    }

    fun buildAddressIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): AddressIdentifierDtoV6{
        val idKey = availableAddressIdentifiers.random(random)
        return AddressIdentifierDtoV6("$idKey Value $seed $index", idKey)
    }

    private fun LogisticAddressDtoV6.withSharedByOwner(isSharedByOwner: Boolean) =
        copy(confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isSharedByOwner))
}