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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDto
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

abstract class BusinessPartnerCommonRequestFactory(
    private val availableAddressIdentifiers: Collection<String>,
    private val availableAdminAreas: Collection<String>,
    private val availableScriptCodes: Collection<String>
) {

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDto): SitePartnerCreateRequest {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateRequest {
        return SitePartnerCreateRequest(
            bpnlParent = bpnlParent,
            index = seed,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseDto): SitePartnerUpdateRequest {
        return SitePartnerUpdateRequest(
            bpns = siteToUpdate.site.bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, bpns: String): SitePartnerUpdateRequest {
        return SitePartnerUpdateRequest(
            bpns = bpns,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteDto(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteDto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteDto(
            name = "Site Name $seed",
            states = listOf(
                SiteStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            mainAddress = createAddressDto(seed, random),
            scriptVariants = listOfNotNull(buildSiteScriptVariant(seed, random)),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDto, random: Random = Random(seed.hashCode().toLong())):SiteCreateRequestWithLegalAddressAsMain {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl, random)
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, bpnL: String, random: Random = Random(seed.hashCode().toLong())):SiteCreateRequestWithLegalAddressAsMain{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateRequestWithLegalAddressAsMain(
            name = "Site Name $seed",
            bpnLParent = bpnL,
            states = listOf(
                SiteStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDto): AddressPartnerCreateRequest {
        return buildAdditionalAddressCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildAdditionalAddressCreateRequest(seed: String, bpnParent: String): AddressPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return AddressPartnerCreateRequest(
            bpnParent = bpnParent,
            index = seed,
            address = createAddressDto(seed, random),
            scriptVariants = listOfNotNull(buildLogisticAddressScriptVariant(seed, random))
        )
    }

    fun buildAddressUpdateRequest(seed: String, legalAddressToUpdate: LegalEntityPartnerCreateVerboseDto): AddressPartnerUpdateRequest {
        return buildAddressUpdateRequest(seed, legalAddressToUpdate.legalAddress.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, addressToUpdate: AddressPartnerCreateVerboseDto): AddressPartnerUpdateRequest {
        return buildAddressUpdateRequest(seed, addressToUpdate.address.bpna)
    }

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateRequest {
        return AddressPartnerUpdateRequest(bpna, createAddressDto(seed, random), listOfNotNull(buildLogisticAddressScriptVariant(seed, random)))
    }

    fun createAddressDto(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressDto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressDto(
            name = "Address Name $seed",
            states = listOf(
                AddressStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = (1 ..2.coerceAtMost(availableAddressIdentifiers.size)).map { createAddressIdentifier(seed, it, random) },
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetDto(
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
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.randomOrNull(random),
                postalCode = "Postal Code $seed",
                city = "City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 5
            )
        )
    }

    fun createAddressIdentifier(seed: String, index: Int, random: Random = Random("$seed $index".hashCode().toLong())): AddressIdentifierDto{
        val idKey = availableAddressIdentifiers.random(random)
        return AddressIdentifierDto("$idKey Value $seed $index", idKey)
    }

    fun buildSiteScriptVariant(seed: String, random: Random = Random(seed.hashCode().toLong())): SiteScriptVariantDto?{
        val scriptCode = availableScriptCodes.randomOrNull(random) ?: return null
        return SiteScriptVariantDto(
            scriptCode = scriptCode,
            name = buildScriptVariantStringValue("Site Name", seed, scriptCode),
            mainAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildLogisticAddressScriptVariant(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressScriptVariantDto? {
        val scriptCode = availableScriptCodes.randomOrNull(random) ?: return null
        return LogisticAddressScriptVariantDto(
            scriptCode = scriptCode,
            address = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildPostalAddressScriptVariant(scriptCode: String, seed: String): PostalAddressScriptVariantDto{
        return PostalAddressScriptVariantDto(
            addressName = buildScriptVariantStringValue("Address Name", seed, scriptCode),
            physicalAddress = buildPhysicalAddressScriptVariant(scriptCode, seed),
            alternativeAddress = buildAlternativeAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildPhysicalAddressScriptVariant(scriptCode: String, seed: String): PhysicalAddressScriptVariantDto{
        return PhysicalAddressScriptVariantDto(
            postalCode = buildScriptVariantStringValue("Postal Code", seed, scriptCode),
            city = buildScriptVariantStringValue("City", seed, scriptCode),
            district = buildScriptVariantStringValue("District", seed, scriptCode),
            street = StreetDto(
                name = buildScriptVariantStringValue("Street Name", seed, scriptCode),
                houseNumber = buildScriptVariantStringValue("House Number", seed, scriptCode),
                houseNumberSupplement = buildScriptVariantStringValue("House Number Supplement", seed, scriptCode),
                milestone = buildScriptVariantStringValue("Milestone", seed, scriptCode),
                direction = buildScriptVariantStringValue("Direction", seed, scriptCode),
                namePrefix = buildScriptVariantStringValue("Name Prefix", seed, scriptCode),
                nameSuffix = buildScriptVariantStringValue("Name Suffix", seed, scriptCode),
                additionalNamePrefix = buildScriptVariantStringValue("Additional Name Prefix", seed, scriptCode),
                additionalNameSuffix = buildScriptVariantStringValue("Additional Name Suffix", seed, scriptCode)
            ),
            companyPostalCode = buildScriptVariantStringValue("Company Postal Code", seed, scriptCode),
            industrialZone = buildScriptVariantStringValue("Industrial Zone", seed, scriptCode),
            building = buildScriptVariantStringValue("Building", seed, scriptCode),
            floor = buildScriptVariantStringValue("Floor", seed, scriptCode),
            door = buildScriptVariantStringValue("Door", seed, scriptCode),
            taxJurisdictionCode = buildScriptVariantStringValue("Tax Jurisdiction Code", seed, scriptCode)
        )
    }

    fun buildAlternativeAddressScriptVariant(scriptCode: String, seed: String): AlternativeAddressScriptVariantDto{
        return AlternativeAddressScriptVariantDto(
            postalCode = buildScriptVariantStringValue("Postal Code", seed, scriptCode),
            city = buildScriptVariantStringValue("City", seed, scriptCode),
            deliveryServiceNumber = buildScriptVariantStringValue("Delivery Service Number ", seed, scriptCode),
            deliveryServiceQualifier = buildScriptVariantStringValue("Delivery Service Qualifier ", seed, scriptCode)
        )
    }

    private fun buildScriptVariantStringValue(name: String, seed: String, scriptCode: String): String{
        return "$name $seed Variant $scriptCode"
    }
}