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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.NamePartTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class OrchestratorRequestFactoryCommon(
    val metadata: TestMetadataReferences? = null,
    private val countries: List<String> = metadata?.countries ?: emptyList()
) {
    fun buildUncategorizedProperties(seed: String, random: Random = createRandomFromSeed(seed)): UncategorizedPropertiesV7Dto{
        val address = buildPostalAddressWithScripVariants(seed, null)
        return UncategorizedPropertiesV7Dto(
            nameParts = 1.rangeTo(random.nextInt(2, 5)).map { index -> "$seed Uncategorized Name Part $index" },
            identifiers = 1.rangeTo(random.nextInt(2, 5)).map { index -> buildIdentifier(seed, index, null)  },
            states = buildStates(random),
            address = address,
            scriptVariants = address.scriptVariants.map { UncategorizedScriptVariantV7Dto(it.scriptCode, listOf("$seed Uncategorized Name Part Variant ${it.scriptCode}")) }
        )
    }

    fun buildSite(seed: String, random: Random = createRandomFromSeed(seed)): SiteV7Dto {
        return SiteV7Dto(
            bpnReference = buildBpnSReference(seed),
            siteName = "Site Name $seed",
            states = buildStates(random),
            confidenceCriteria = buildConfidenceCriteria(random),
            hasChanged = true,
            siteMainAddress = buildPostalAddress(seed, AddressType.SiteMainAddress),
            scriptVariants = listOf(buildSiteScriptVariant(seed, random)),
            updatedAt = buildUpdatedAt(random)
        )
    }

    fun buildAdditionalSites(seed: String, count: Int = 2): List<AdditionalSiteV7Dto> {
        return (1..count).map { index ->
            AdditionalSiteV7Dto(
                bpnReference = buildBpnSReference("$seed Additional $index"),
                siteName = "Additional Site Name $index $seed"
            )
        }
    }

    fun buildPostalAddressWithScripVariants(seed: String, addressType: AddressType?, random: Random = createRandomFromSeed(seed)): PostalAddressWithScriptVariantsV7Dto{
        return PostalAddressWithScriptVariantsV7Dto(buildPostalAddress(seed, addressType, random), listOf(buildAddressVariantWithScriptCode(seed, random)))
    }

    fun buildPostalAddress(seed: String, addressType: AddressType?, random: Random = createRandomFromSeed(seed)): PostalAddressV7Dto {
        return PostalAddressV7Dto(
            bpnReference = buildBpnAReference(seed, addressType),
            hasChanged = true,
            addressName = "$seed Address Name",
            identifiers = buildAddressIdentifiers(seed, random, addressType),
            states = buildStates(random),
            confidenceCriteria = buildConfidenceCriteria(random),
            physicalAddress = PhysicalAddressV7Dto(
                geographicCoordinates = GeoCoordinateV7Dto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = getCountryReference(seed, random),
                administrativeAreaLevel1 = getAdminAreaReference(seed, random),
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
                taxJurisdictionCode = "123"
            ),
            alternativeAddress = AlternativeAddressV7Dto(
                geographicCoordinates = GeoCoordinateV7Dto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = getCountryReference(seed, random),
                administrativeAreaLevel1 =  getAdminAreaReference(seed, random),
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            updatedAt = buildUpdatedAt(random)
        )
    }

    fun buildUpdatedAt(random: Random): Instant {
        return random.nextInstant()
    }

    fun buildBpnLReference(seed: String): BpnReferenceV7Dto{
        return buildBpnReference(seed, "BPNL")
    }

    fun buildBpnSReference(seed: String): BpnReferenceV7Dto{
        return buildBpnReference(seed, "BPNS")
    }

    fun buildBpnAReference(seed: String, addressType: AddressType?): BpnReferenceV7Dto{
        val addressTypeName= addressType?.name ?: "Uncategorized"

        return buildBpnReference(seed, "BPNA$addressTypeName")
    }

    fun buildBpnReference(seed: String, bpn: String): BpnReferenceV7Dto {
        return BpnReferenceV7Dto(
            referenceValue = "$bpn Reference Value $seed",
            desiredBpn = bpn,
            referenceType = BpnReferenceTypeV7.Bpn
        )
    }

    fun buildConfidenceCriteria(random: Random) : ConfidenceCriteriaV7Dto {
        val lastConfidenceCheck = random.nextInstant()
        return ConfidenceCriteriaV7Dto(
            sharedByOwner = random.nextBoolean(),
            checkedByExternalDataSource = random.nextBoolean(),
            numberOfSharingMembers = random.nextInt(0, 5),
            lastConfidenceCheckAt = lastConfidenceCheck,
            nextConfidenceCheckAt = lastConfidenceCheck.plus(10, ChronoUnit.DAYS),
            confidenceLevel = random.nextInt(0, 10)
        )
    }

    fun buildLegalIdentifiers(seed: String, random: Random): List<IdentifierV7Dto> {
        return buildIdentifiers(seed, random, getLegalIdentifierTypeReference(seed, random))
    }

    fun buildAddressIdentifiers(seed: String, random: Random, addressType: AddressType?): List<IdentifierV7Dto> {
        return buildIdentifiers("$seed $addressType", random, getAddressIdentifierTypeReference(seed, random))
    }

    fun buildIdentifiers(seed: String, random: Random, idType: String): List<IdentifierV7Dto>{
        return 1.rangeTo(random.nextInt(2, 5)).map { index -> buildIdentifier(seed, index, idType)  }
    }

    fun buildIdentifier(seed: String, index: Int, identifierType: String?): IdentifierV7Dto {
        return IdentifierV7Dto("$seed Id Value $index",  identifierType ?: "$seed Id Type $index", "$seed Issuing Body $index")
    }

    fun buildStates(random: Random): List<BusinessStateV7Dto>{
        val validFrom = random.nextInstant()
        val firstDuration = Duration.ofDays(random.nextLong(1L, 100L))
        val validTo = validFrom.plus(firstDuration)

        val firstStateType = BusinessStateType.entries.random(random)

        val secondDuration = Duration.ofDays(random.nextLong(1L, 100L))
        val secondStateType = BusinessStateType.entries.find { it != firstStateType }!!

        return listOf(BusinessStateV7Dto(validFrom, validTo, firstStateType), BusinessStateV7Dto(validTo, validTo.plus(secondDuration), secondStateType))
    }

    fun buildNameParts(seed: String): List<NamePartV7Dto>{
        return listOf(
            NamePartV7Dto("Legal Name $seed", NamePartTypeV7.LegalName),
            NamePartV7Dto("Site Name $seed", NamePartTypeV7.SiteName),
            NamePartV7Dto("Address Name $seed", NamePartTypeV7.AddressName),
            NamePartV7Dto("Legal Short Name $seed", NamePartTypeV7.ShortName),
            NamePartV7Dto("Legal Form $seed", NamePartTypeV7.LegalForm)
        )
    }

    fun buildLegalEntityScriptVariant(seed: String, random: Random = createRandomFromSeed(seed)): LegalEntityScriptVariantV7Dto {
        val scriptCode = metadata?.scriptCodes?.randomOrNull(random) ?: "Script Code $seed"

        return LegalEntityScriptVariantV7Dto(
            scriptCode = scriptCode,
            legalName = buildScriptVariantStringValue("Legal Name", seed, scriptCode),
            legalShortName = buildScriptVariantStringValue("Legal Short Name", seed, scriptCode),
            legalAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildSiteScriptVariant(seed: String, random: Random = createRandomFromSeed(seed)): SiteScriptVariantV7Dto {
        val scriptCode = metadata?.scriptCodes?.randomOrNull(random) ?: "Script Code $seed"

        return SiteScriptVariantV7Dto(
            scriptCode = scriptCode,
            siteName = buildScriptVariantStringValue("Site Name $seed", seed, scriptCode),
            mainAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildAddressVariantWithScriptCode(seed: String, random: Random = createRandomFromSeed(seed)): PostalAddressScriptVariantWithScriptCodeV7Dto{
        val scriptCode = metadata?.scriptCodes?.randomOrNull(random) ?: "Script Code $seed"

        return PostalAddressScriptVariantWithScriptCodeV7Dto(
            scriptCode = scriptCode,
            postalProperties = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildPostalAddressScriptVariant(scriptCode: String, seed: String): PostalAddressScriptVariantV7Dto{
        return PostalAddressScriptVariantV7Dto(
            addressName = buildScriptVariantStringValue("Address Name $seed", seed, scriptCode),
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
                namePrefix = buildScriptVariantStringValue("NamePrefix", seed, scriptCode),
                additionalNamePrefix = buildScriptVariantStringValue("AdditionalNamePrefix", seed, scriptCode),
                nameSuffix = buildScriptVariantStringValue("NameSuffix", seed, scriptCode),
                additionalNameSuffix = buildScriptVariantStringValue("AdditionalNameSuffix", seed, scriptCode),
            ),
            industrialZone = buildScriptVariantStringValue("IndustrialZone", seed, scriptCode),
            building = buildScriptVariantStringValue("Building", seed, scriptCode),
            floor = buildScriptVariantStringValue("Floor", seed, scriptCode),
            door = buildScriptVariantStringValue("Door", seed, scriptCode),
        )
    }

    fun buildAlternativeAddressScriptVariant(scriptCode: String, seed: String): AlternativeAddressScriptVariantV7Dto{
        return AlternativeAddressScriptVariantV7Dto(
            city = buildScriptVariantStringValue("City $seed", seed, scriptCode)
        )
    }

    fun getCountryReference(seed: String, random: Random = createRandomFromSeed(seed)): String{
        return countries.random(random)
    }

    fun getLegalIdentifierTypeReference(seed: String, random: Random = createRandomFromSeed(seed)): String{
        return metadata?.legalEntityIdentifierTypes?.random(random) ?: "Legal Identifier Type $seed"
    }

    fun getAddressIdentifierTypeReference(seed: String, random: Random = createRandomFromSeed(seed)): String{
        return metadata?.addressIdentifierTypes?.random(random) ?: "Address Type $seed"
    }

    fun getAdminAreaReference(seed: String, random: Random = createRandomFromSeed(seed)): String{
        return metadata?.adminAreas?.random(random) ?: "Admin Area $seed"
    }

    fun createRandomFromSeed(seed: String): Random{
        return Random(seed.hashCode())
    }

    private fun Random.nextInstant() = Instant.ofEpochSecond(nextLong(100000L, 200000L))

    private fun buildScriptVariantStringValue(name: String, seed: String, scriptCode: String): String {
        return "$name $seed Variant $scriptCode"
    }
}

data class TestMetadataReferences(
    val legalForms: List<String>,
    val legalEntityIdentifierTypes: List<String>,
    val addressIdentifierTypes: List<String>,
    val adminAreas: List<String>,
    val scriptCodes: List<String>,
    val reasonCodes: List<String>,
    val countries: List<String>
)