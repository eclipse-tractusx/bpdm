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

package org.eclipse.tractusx.bpdm.test.testdata.pool.common

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withSharedByOwner
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * Bounds seeded state starts so that a state and its ten-day successor both lie before the latest date.
 */
val latestSeededStateSecond: Long = BusinessStateDates.LATEST.minusDays(11).toEpochSecond(ZoneOffset.UTC)

abstract class BusinessPartnerCommonRequestFactory(
    protected val availableAddressIdentifiers: Collection<String>,
    protected val availableAdminAreas: Collection<String>,
    protected val availableScriptCodes: Collection<String>
) {

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto): SitePartnerCreateV7Request {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateV7Request {
        return SitePartnerCreateV7Request(
            bpnlParent = bpnlParent,
            index = seed,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteDto(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteV7Dto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteV7Dto(
            name = "Site Name $seed",
            states = listOf(
                SiteStateV7Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateV7Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            mainAddress = createAddressDto(seed, random).withSharedByOwner(true),
            scriptVariants = listOfNotNull(buildSiteScriptVariant(seed, random)),
            confidenceCriteria = ConfidenceCriteriaV7Dto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto, random: Random = Random(seed.hashCode().toLong())):SiteCreateWithLegalAddressAsMainV7Request {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl, random)
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, bpnL: String, random: Random = Random(seed.hashCode().toLong())):SiteCreateWithLegalAddressAsMainV7Request{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateWithLegalAddressAsMainV7Request(
            name = "Site Name $seed",
            bpnLParent = bpnL,
            states = listOf(
                SiteStateV7Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateV7Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaV7Dto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto): AddressPartnerCreateV7Request {
        return buildAdditionalAddressCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildAdditionalAddressCreateRequest(seed: String, bpnParent: String): AddressPartnerCreateV7Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return AddressPartnerCreateV7Request(
            bpnParent = bpnParent,
            index = seed,
            address = createAddressDto(seed, random),
            scriptVariants = listOfNotNull(buildLogisticAddressScriptVariant(seed, random))
        )
    }

    fun buildAddressUpdateRequest(seed: String, legalAddressToUpdate: LegalEntityPartnerCreateVerboseV6Dto): AddressPartnerUpdateV7Request {
        return buildAddressUpdateRequest(seed, legalAddressToUpdate.legalAddress.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, addressToUpdate: AddressPartnerCreateVerboseV6Dto): AddressPartnerUpdateV7Request {
        return buildAddressUpdateRequest(seed, addressToUpdate.address.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateV7Request {
        return AddressPartnerUpdateV7Request(bpna, createAddressDto(seed, random), listOfNotNull(buildLogisticAddressScriptVariant(seed, random)))
    }

    fun createAddressDto(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressV7Dto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressV7Dto(
            name = "Address Name $seed",
            states = listOf(
                AddressStateV7Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateV7Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = (1 ..2.coerceAtMost(availableAddressIdentifiers.size)).map { buildAddressIdentifier(seed, it, random) },
            physicalPostalAddress = PhysicalPostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = listOf("DE", "US", "FR", "CN", "PL").random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetV7Dto(
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
            alternativePostalAddress = AlternativePostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = listOf("DE", "US", "FR", "CN", "PL").random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                postalCode = "Postal Code $seed",
                city = "City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            confidenceCriteria = ConfidenceCriteriaV7Dto(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 0
            )
        )
    }

    fun buildAddressIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): AddressIdentifierV7Dto{
        val idKey = availableAddressIdentifiers.random(random)
        return AddressIdentifierV7Dto("$idKey Value $seed $index", idKey)
    }

    fun buildSiteScriptVariant(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteScriptVariantV7Dto?{
        val scriptCode = availableScriptCodes.randomOrNull(random) ?: return null
        return SiteScriptVariantV7Dto(
            scriptCode = scriptCode,
            name = buildScriptVariantStringValue("Site Name", seed, scriptCode),
            mainAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildLogisticAddressScriptVariant(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressScriptVariantV7Dto? {
        val scriptCode = availableScriptCodes.randomOrNull(random) ?: return null
        return LogisticAddressScriptVariantV7Dto(
            scriptCode = scriptCode,
            address = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildPostalAddressScriptVariant(scriptCode: String, seed: String): PostalAddressScriptVariantV7Dto{
        return PostalAddressScriptVariantV7Dto(
            addressName = buildScriptVariantStringValue("Address Name", seed, scriptCode),
            physicalAddress = buildPhysicalAddressScriptVariant(scriptCode, seed),
            alternativeAddress = buildAlternativeAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildPhysicalAddressScriptVariant(scriptCode: String, seed: String): PhysicalAddressScriptVariantV7Dto{
        return PhysicalAddressScriptVariantV7Dto(
            city = buildScriptVariantStringValue("City", seed, scriptCode),
            district = buildScriptVariantStringValue("District", seed, scriptCode),
            street = StreetScriptVariantV7Dto(
                name = buildScriptVariantStringValue("Street Name", seed, scriptCode),
                direction = buildScriptVariantStringValue("Direction", seed, scriptCode),
                namePrefix = buildScriptVariantStringValue("Name Prefix", seed, scriptCode),
                nameSuffix = buildScriptVariantStringValue("Name Suffix", seed, scriptCode),
                additionalNamePrefix = buildScriptVariantStringValue("Additional Name Prefix", seed, scriptCode),
                additionalNameSuffix = buildScriptVariantStringValue("Additional Name Suffix", seed, scriptCode)
            ),
            industrialZone = buildScriptVariantStringValue("Industrial Zone", seed, scriptCode),
            building = buildScriptVariantStringValue("Building", seed, scriptCode),
            floor = buildScriptVariantStringValue("Floor", seed, scriptCode),
            door = buildScriptVariantStringValue("Door", seed, scriptCode)
        )
    }

    fun buildAlternativeAddressScriptVariant(scriptCode: String, seed: String): AlternativeAddressScriptVariantV7Dto{
        return AlternativeAddressScriptVariantV7Dto(
            city = buildScriptVariantStringValue("City", seed, scriptCode)
        )
    }

    protected fun buildScriptVariantStringValue(name: String, seed: String, scriptCode: String): String{
        return "$name $seed Variant $scriptCode"
    }
}