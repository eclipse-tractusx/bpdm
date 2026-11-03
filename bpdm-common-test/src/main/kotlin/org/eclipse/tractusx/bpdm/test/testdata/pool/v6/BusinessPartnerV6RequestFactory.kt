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

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.common.latestSeededStateSecond
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.StreetV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.businesspartner.ConfidenceCriteriaV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityIdentifierV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request

class BusinessPartnerV6RequestFactory(
    testMetadata: TestMetadataV6
){
    private val availableLegalForms = testMetadata.legalForms.map { it.technicalKey }
    private val availableLegalEntityIdentifiers = testMetadata.legalEntityIdentifierTypes.map { it.technicalKey }
    private val availableAddressIdentifiers: Collection<String> =  testMetadata.addressIdentifierTypes.map { it.technicalKey }
    private val availableAdminAreas: Collection<String> = testMetadata.adminAreas.map { it.code }

    fun buildLegalEntityCreateRequest(seed: String): LegalEntityPartnerCreateV6Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateV6Request(
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random),
            index = seed
        )
    }

    fun createLegalEntityUpdateRequest(seed: String, legalEntityToUpdate: LegalEntityPartnerCreateVerboseV6Dto): LegalEntityPartnerUpdateV6Request {
        return createLegalEntityUpdateRequest(seed, legalEntityToUpdate.legalEntity.bpnl)
    }

    fun createLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateV6Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateV6Request(
            bpnl = bpnl,
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random)
        )
    }

    fun createLegalEntityDto(seed: String, random: Random =  Random(seed.hashCode().toLong())): LegalEntityV6Dto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityV6Dto(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = availableLegalForms.randomOrNull(random),
            identifiers = listOf(createLegalEntityIdentifier(seed, 0, random), createLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateV6Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateV6Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaV6Dto(
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

    fun createLegalEntityIdentifier(seed: String, index: Int, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierV6Dto{
        val idKey = availableLegalEntityIdentifiers.random(random)
        return LegalEntityIdentifierV6Dto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto): SitePartnerCreateV6Request {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateV6Request {
        return SitePartnerCreateV6Request(
            bpnlParent = bpnlParent,
            index = seed,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseV6Dto): SitePartnerUpdateV6Request {
        return SitePartnerUpdateV6Request(
            bpns = siteToUpdate.site.bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, bpns: String): SitePartnerUpdateV6Request {
        return SitePartnerUpdateV6Request(
            bpns = bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteDto(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteV6Dto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteV6Dto(
            name = "Site Name $seed",
            states = listOf(
                SiteStateV6Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateV6Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            mainAddress = createAddressDto(seed, random).withSharedByOwner(true),
            confidenceCriteria = ConfidenceCriteriaV6Dto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto, random: Random = Random(seed.hashCode().toLong())):SiteCreateWithLegalAddressAsMainV6Request {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl, random)
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, bpnL: String, random: Random = Random(seed.hashCode().toLong())):SiteCreateWithLegalAddressAsMainV6Request{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateWithLegalAddressAsMainV6Request(
            name = "Site Name $seed",
            bpnLParent = bpnL,
            states = listOf(
                SiteStateV6Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateV6Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaV6Dto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto): AddressPartnerCreateV6Request {
        return buildAdditionalAddressCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildAdditionalAddressCreateRequest(seed: String, bpnParent: String): AddressPartnerCreateV6Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return AddressPartnerCreateV6Request(
            bpnParent = bpnParent,
            index = seed,
            address = createAddressDto(seed, random)
        )
    }

    fun buildAddressUpdateRequest(seed: String, legalAddressToUpdate: LegalEntityPartnerCreateVerboseV6Dto): AddressPartnerUpdateV6Request {
        return buildAddressUpdateRequest(seed, legalAddressToUpdate.legalAddress.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, addressToUpdate: AddressPartnerCreateVerboseV6Dto): AddressPartnerUpdateV6Request {
        return buildAddressUpdateRequest(seed, addressToUpdate.address.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateV6Request {
        return AddressPartnerUpdateV6Request(bpna, createAddressDto(seed, random))
    }

    fun createAddressDto(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressV6Dto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressV6Dto(
            name = "Address Name $seed",
            states = listOf(
                AddressStateV6Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateV6Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = (1 ..2.coerceAtMost(availableAddressIdentifiers.size)).map { buildAddressIdentifier(seed, it, random) },
            physicalPostalAddress = PhysicalPostalAddressV6Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = listOf("DE", "US", "FR", "CN", "PL").random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetV6Dto(
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
            alternativePostalAddress = AlternativePostalAddressV6Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = listOf("DE", "US", "FR", "CN", "PL").random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                postalCode = "Postal Code $seed",
                city = "City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            confidenceCriteria = ConfidenceCriteriaV6Dto(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 0
            )
        )
    }

    fun buildAddressIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): AddressIdentifierV6Dto{
        val idKey = availableAddressIdentifiers.random(random)
        return AddressIdentifierV6Dto("$idKey Value $seed $index", idKey)
    }

    private fun LogisticAddressV6Dto.withSharedByOwner(isSharedByOwner: Boolean) =
        copy(confidenceCriteria = confidenceCriteria.copy(sharedByOwner = isSharedByOwner))
}