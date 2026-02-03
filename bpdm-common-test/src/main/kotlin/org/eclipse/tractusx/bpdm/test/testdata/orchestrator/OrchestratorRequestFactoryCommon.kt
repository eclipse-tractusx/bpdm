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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class OrchestratorRequestFactoryCommon(
    val metadata: TestMetadataReferences? = null
) {
    fun buildUncategorizedProperties(seed: String, random: Random = createRandomFromSeed(seed)): UncategorizedProperties{
        return UncategorizedProperties(
            nameParts = 1.rangeTo(random.nextInt(2, 5)).map { index -> "$seed Uncategorized Name Part $index" },
            identifiers = 1.rangeTo(random.nextInt(2, 5)).map { index -> buildIdentifier(seed, index, null)  },
            states = buildStates(random),
            address = buildPostalAddress(seed, null)
        )
    }

    fun buildSite(seed: String, random: Random = createRandomFromSeed(seed)): Site {
        return Site(
            bpnReference = buildBpnSReference(seed),
            siteName = "Site Name $seed",
            states = buildStates(random),
            confidenceCriteria = buildConfidenceCriteria(random),
            hasChanged = true,
            siteMainAddress = buildPostalAddress(seed, AddressType.SiteMainAddress)
        )
    }

    fun buildPostalAddress(seed: String, addressType: AddressType?, random: Random = createRandomFromSeed(seed)): PostalAddress {
        return PostalAddress(
            bpnReference = buildBpnAReference(seed, addressType),
            hasChanged = true,
            addressName = "$seed Address Name",
            identifiers = buildAddressIdentifiers(seed, random, addressType),
            states = buildStates(random),
            confidenceCriteria = buildConfidenceCriteria(random),
            physicalAddress = PhysicalAddress(
                geographicCoordinates = GeoCoordinate(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.toTypedArray().random(random).alpha2,
                administrativeAreaLevel1 = getAdminAreaReference(seed, random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = Street(
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
            alternativeAddress = AlternativeAddress(
                geographicCoordinates = GeoCoordinate(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.toTypedArray().random(random).alpha2,
                administrativeAreaLevel1 =  getAdminAreaReference(seed, random),
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            )
        )
    }

    fun buildBpnLReference(seed: String): BpnReference{
        return buildBpnReference(seed, "BPNL")
    }

    fun buildBpnSReference(seed: String): BpnReference{
        return buildBpnReference(seed, "BPNS")
    }

    fun buildBpnAReference(seed: String, addressType: AddressType?): BpnReference{
        val addressTypeName= addressType?.name ?: "Uncategorized"

        return buildBpnReference(seed, "BPNA$addressTypeName")
    }

    fun buildBpnReference(seed: String, bpn: String): BpnReference {
        return BpnReference(
            referenceValue = "$bpn Reference Value $seed",
            desiredBpn = bpn,
            referenceType = BpnReferenceType.Bpn
        )
    }

    fun buildConfidenceCriteria(random: Random) : ConfidenceCriteria {
        val lastConfidenceCheck = random.nextInstant()
        return ConfidenceCriteria(
            sharedByOwner = random.nextBoolean(),
            checkedByExternalDataSource = random.nextBoolean(),
            numberOfSharingMembers = random.nextInt(0, 5),
            lastConfidenceCheckAt = lastConfidenceCheck,
            nextConfidenceCheckAt = lastConfidenceCheck.plus(10, ChronoUnit.DAYS),
            confidenceLevel = random.nextInt(0, 10)
        )
    }

    fun buildLegalIdentifiers(seed: String, random: Random): List<Identifier> {
        return buildIdentifiers(seed, random, getLegalIdentifierTypeReference(seed, random))
    }

    fun buildAddressIdentifiers(seed: String, random: Random, addressType: AddressType?): List<Identifier> {
        return buildIdentifiers("$seed $addressType", random, getAddressIdentifierTypeReference(seed, random))
    }

    fun buildIdentifiers(seed: String, random: Random, idType: String): List<Identifier>{
        return 1.rangeTo(random.nextInt(2, 5)).map { index -> buildIdentifier(seed, index, idType)  }
    }

    fun buildIdentifier(seed: String, index: Int, identifierType: String?): Identifier {
        return Identifier("$seed Id Value $index",  identifierType ?: "$seed Id Type $index", "$seed Issuing Body $index")
    }

    fun buildStates(random: Random): List<BusinessState>{
        val validFrom = random.nextInstant()
        val firstDuration = Duration.ofDays(random.nextLong(1L, 100L))
        val validTo = validFrom.plus(firstDuration)

        val firstStateType = BusinessStateType.entries.random(random)

        val secondDuration = Duration.ofDays(random.nextLong(1L, 100L))
        val secondStateType = BusinessStateType.entries.find { it != firstStateType }!!

        return listOf(BusinessState(validFrom, validTo, firstStateType), BusinessState(validTo, validTo.plus(secondDuration), secondStateType))
    }

    fun buildNameParts(seed: String): List<NamePart>{
        return listOf(
            NamePart("Legal Name $seed", NamePartType.LegalName),
            NamePart("Site Name $seed", NamePartType.SiteName),
            NamePart("Address Name $seed", NamePartType.AddressName),
            NamePart("Legal Short Name $seed", NamePartType.ShortName),
            NamePart("Legal Form $seed", NamePartType.LegalForm)
        )
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


}

data class TestMetadataReferences(
    val legalForms: List<String>,
    val legalEntityIdentifierTypes: List<String>,
    val addressIdentifierTypes: List<String>,
    val adminAreas: List<String>
)