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

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.common.latestSeededStateSecond
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteHeaderScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7Request

class PoolRequestFactoryV7(
    private val testMetadata: TestMetadataV7
){
    private val availableAddressIdentifiers: Collection<String> = testMetadata.addressIdentifierTypes.map { it.technicalKey }
    private val availableAdminAreas: Collection<String> = testMetadata.adminAreas.map { it.code }
    private val availableScriptCodes: Collection<String> = testMetadata.scriptCodes.map { it.technicalKey }

    fun buildLegalEntityCreateRequest(seed: String, isParticipantData: Boolean = false): LegalEntityPartnerCreateV7Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateV7Request(
            legalEntity = buildLegalEntity(seed, random, isParticipantData),
            index = seed
        )
    }

    fun buildLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateV7Request {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateV7Request(
            bpnl = bpnl,
            legalEntity = buildLegalEntity(seed, random)
        )
    }

    fun buildLegalEntity(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityV7Dto {
        return LegalEntityV7Dto(
            header = buildLegalEntityHeaderDto(seed, random, isParticipantData),
            legalAddress = createAddressDto("Legal Address $seed", random),
            scriptVariants = listOf(buildLegalEntityScriptVariantDto(seed, random)),
        )
    }

    fun buildLegalEntityHeaderDto(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityHeaderV7Dto{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityHeaderV7Dto(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = testMetadata.legalForms.random(random).technicalKey,
            identifiers = listOf(buildLegalEntityIdentifier(seed, 0, random), buildLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateV7Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateV7Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaV7Dto(
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

    fun buildLegalEntityIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierV7Dto{
        val idKey = testMetadata.legalEntityIdentifierTypes.random(random).technicalKey
        return LegalEntityIdentifierV7Dto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }

    fun buildLegalEntityScriptVariantDto(seed: String, random: Random = createRandom(seed)): LegalEntityScriptVariantV7Dto {
        val scriptCode = availableScriptCodes.random(random)

        return LegalEntityScriptVariantV7Dto(
            scriptCode = scriptCode,
            legalName = buildScriptVariantStringValue("Legal Name", seed, scriptCode),
            shortName = buildScriptVariantStringValue("Legal Short Name", seed, scriptCode),
            legalAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto): SitePartnerCreateV7Request {
        return with(buildSiteCreateRequest(seed, legalEntityParent.header.bpnl)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto): SiteCreateWithLegalAddressAsMainV7Request {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.header.bpnl)
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseV7Dto): SitePartnerUpdateV7Request {
        return with(createSiteUpdateRequest(seed, siteToUpdate.site.bpns)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseV7Dto): AddressPartnerCreateV7Request =
        buildAdditionalAddressCreateRequest(seed, legalEntity.header.bpnl)

    fun buildAdditionalAddressCreateRequest(seed: String, site: SitePartnerCreateVerboseV7Dto): AddressPartnerCreateV7Request =
        buildAdditionalAddressCreateRequest(seed, site.site.bpns)

    fun buildAddressUpdateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseV7Dto): AddressPartnerUpdateV7Request =
        buildAddressUpdateRequest(seed, legalEntity.legalAddress.bpna)
            .copy(scriptVariants = buildAddressScriptVariants(legalEntity.scriptVariants.map { it.scriptCode }, seed))

    fun buildAddressUpdateRequest(seed: String, site: SitePartnerCreateVerboseV7Dto): AddressPartnerUpdateV7Request =
        buildAddressUpdateRequest(seed, site.mainAddress.bpna)
            .copy(scriptVariants = buildAddressScriptVariants(site.site.scriptVariants.map { it.scriptCode }, seed))

    /**
     * Builds one address script variant per given script code. A legal or site main address has to keep covering every
     * script code its legal entity or site is named in, so an update of such an address states them explicitly.
     */
    fun buildAddressScriptVariants(scriptCodes: List<String>, seed: String): List<LogisticAddressScriptVariantV7Dto> =
        scriptCodes.map { LogisticAddressScriptVariantV7Dto(it, buildPostalAddressScriptVariant(it, seed)) }

    fun buildAddressUpdateRequest(seed: String, createdAddress: AddressPartnerCreateVerboseV7Dto): AddressPartnerUpdateV7Request =
        buildAddressUpdateRequest(seed, createdAddress.address.bpna)

    fun buildAddressUpdateRequest(seed: String, bpna: String, random: Random = Random(seed.hashCode().toLong())): AddressPartnerUpdateV7Request {
        return AddressPartnerUpdateV7Request(bpna, createAddressDto(seed, random), listOfNotNull(buildLogisticAddressScriptVariant(seed, random)))
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityPartnerCreateVerboseV6Dto): SitePartnerCreateV7Request {
        return buildSiteCreateRequest(seed, legalEntityParent.legalEntity.bpnl)
    }

    fun buildLegalSiteCreateRequest(seed: String, bpnlParent: String, random: Random = Random(seed.hashCode().toLong())): SiteCreateWithLegalAddressAsMainV7Request{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, latestSeededStateSecond), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SiteCreateWithLegalAddressAsMainV7Request(
            name = "Site Name $seed",
            states = listOf(
                SiteStateV7Dto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                SiteStateV7Dto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            scriptVariants = availableScriptCodes.shuffled(random).take(2).map { scriptCode ->
                SiteHeaderScriptVariantV7Dto(
                    scriptCode = scriptCode,
                    name = buildScriptVariantStringValue("Site Name", seed, scriptCode)
                )
            },
            bpnLParent = bpnlParent,
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

    fun buildSiteCreateRequest(seed: String, bpnlParent: String): SitePartnerCreateV7Request {
        return SitePartnerCreateV7Request(
            bpnlParent = bpnlParent,
            index = seed,
            site = createSiteDto("Main Address $seed")
        )
    }

    fun createSiteUpdateRequest(seed: String, bpns: String): SitePartnerUpdateV7Request {
        return SitePartnerUpdateV7Request(
            bpns = bpns,
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

    /**
     * The script code this factory picks for [seed], so a caller can pin an entity to the code of a related seed - needed
     * when several entities share one address and therefore have to be named in the scripts that address covers.
     */
    fun scriptCodeFor(seed: String, random: Random = Random(seed.hashCode().toLong())): String? =
        availableScriptCodes.randomOrNull(random)

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

    private fun createRandom(seed: String) =  Random(seed.hashCode().toLong())

    private fun SiteV7Dto.withMainAddressIsShared() =
        copy(mainAddress = mainAddress.copy(confidenceCriteria = mainAddress.confidenceCriteria.withGivenConfidence(TestDataV7.SharedByOwner)))

}