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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto as AddressPartnerCreateVerboseDtoV7

class PoolRequestFactoryV7(
    private val testMetadata: TestMetadataV7
){
    private val availableAddressIdentifiers: Collection<String> = testMetadata.addressIdentifierTypes.map { it.technicalKey }
    private val availableAdminAreas: Collection<String> = testMetadata.adminAreas.map { it.code }
    private val availableScriptCodes: Collection<String> = testMetadata.scriptCodes.map { it.technicalKey }

    fun buildLegalEntityCreateRequest(seed: String, isParticipantData: Boolean = false): LegalEntityPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateRequest(
            legalEntity = buildLegalEntity(seed, random, isParticipantData),
            index = seed
        )
    }

    fun buildLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateRequest(
            bpnl = bpnl,
            legalEntity = buildLegalEntity(seed, random)
        )
    }

    fun buildLegalEntity(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityDto {
        return LegalEntityDto(
            header = buildLegalEntityHeaderDto(seed, random, isParticipantData),
            legalAddress = createAddressDto("Legal Address $seed", random),
            scriptVariants = listOf(buildLegalEntityScriptVariantDto(seed, random)),
        )
    }

    fun buildLegalEntityHeaderDto(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityHeaderDto{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityHeaderDto(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = testMetadata.legalForms.random(random).technicalKey,
            identifiers = listOf(buildLegalEntityIdentifier(seed, 0, random), buildLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp,
                nextConfidenceCheckAt = timeStamp.plusDays(7),
                confidenceLevel = 0
            ),
            isParticipantData = isParticipantData
        )
    }

    fun buildLegalEntityIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierDto{
        val idKey = testMetadata.legalEntityIdentifierTypes.random(random).technicalKey
        return LegalEntityIdentifierDto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }

    fun buildLegalEntityScriptVariantDto(seed: String, random: Random = createRandom(seed)): LegalEntityScriptVariantDto {
        val scriptCode = availableScriptCodes.random(random)

        return LegalEntityScriptVariantDto(
            scriptCode = scriptCode,
            legalName = buildScriptVariantStringValue("Legal Name", seed, scriptCode),
            shortName = buildScriptVariantStringValue("Legal Short Name", seed, scriptCode),
            legalAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseDto): SitePartnerCreateRequest {
        return with(buildSiteCreateRequest(seed, legalEntityParent.header.bpnl)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseDto): SiteCreateRequestWithLegalAddressAsMain {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.header.bpnl)
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseDto): SitePartnerUpdateRequest {
        return with(createSiteUpdateRequest(seed, siteToUpdate.site.bpns)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseDto): AddressPartnerCreateRequest =
        buildAdditionalAddressCreateRequest(seed, legalEntity.header.bpnl)

    fun buildAdditionalAddressCreateRequest(seed: String, site: SitePartnerCreateVerboseDto): AddressPartnerCreateRequest =
        buildAdditionalAddressCreateRequest(seed, site.site.bpns)

    fun buildAddressUpdateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseDto): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, legalEntity.legalAddress.bpna)

    fun buildAddressUpdateRequest(seed: String, site: SitePartnerCreateVerboseDto): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, site.mainAddress.bpna)

    fun buildAddressUpdateRequest(seed: String, createdAddress: AddressPartnerCreateVerboseDtoV7): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, createdAddress.address.bpna)

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateRequest {
        return AddressPartnerUpdateRequest(bpna, createAddressDto(seed, random), listOfNotNull(buildLogisticAddressScriptVariant(seed, random)))
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6): SitePartnerCreateRequest {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildLegalSiteCreateRequest(seed: String, bpnlParent: String, random: Random = Random(seed.hashCode().toLong())): SiteCreateRequestWithLegalAddressAsMain{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateRequestWithLegalAddressAsMain(
            name = "Site Name $seed",
            states = listOf(
                SiteStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            scriptVariants = availableScriptCodes.shuffled(random).take(2).map { scriptCode ->
                SiteHeaderScriptVariantDto(
                    scriptCode = scriptCode,
                    name = buildScriptVariantStringValue("Site Name", seed, scriptCode)
                )
            },
            bpnLParent = bpnlParent,
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

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateRequest {
        return SitePartnerCreateRequest(
            bpnlParent = bpnlParent,
            index = seed,
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
            mainAddress = createAddressDto(seed, random).withSharedByOwner(true),
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

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseDtoV6): AddressPartnerCreateRequest {
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

    fun createAddressDto(seed: String, random: Random = Random(seed.hashCode().toLong())): LogisticAddressDto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressDto(
            name = "Address Name $seed",
            states = listOf(
                AddressStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = (1 ..2.coerceAtMost(availableAddressIdentifiers.size)).map { buildAddressIdentifier(seed, it, random) },
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
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 0
            )
        )
    }

    fun buildAddressIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): AddressIdentifierDto{
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

    protected fun buildScriptVariantStringValue(name: String, seed: String, scriptCode: String): String{
        return "$name $seed Variant $scriptCode"
    }

    private fun createRandom(seed: String) =  Random(seed.hashCode().toLong())

    private fun SiteDto.withMainAddressIsShared() =
        copy(mainAddress = mainAddress.copy(confidenceCriteria = mainAddress.confidenceCriteria.withGivenConfidence(TestDataV7.SharedByOwner)))

}