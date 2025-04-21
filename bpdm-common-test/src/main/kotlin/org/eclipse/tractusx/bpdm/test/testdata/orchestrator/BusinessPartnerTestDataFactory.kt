/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random




class BusinessPartnerTestDataFactory(
    val metadata: TestMetadataReferences? = null
){

    //Business Partner with maximal fields filled
    fun createFullBusinessPartner(seed: String = "Test"): BusinessPartner {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return BusinessPartner(
            nameParts = listOf(
                NamePart("Legal Name $seed", NamePartType.LegalName),
                NamePart("Site Name $seed", NamePartType.SiteName),
                NamePart("Address Name $seed", NamePartType.AddressName),
                NamePart("Legal Short Name $seed", NamePartType.ShortName),
                NamePart("Legal Form $seed", NamePartType.LegalForm)
            ),
            owningCompany = "Owner Company BPNL $seed",
            uncategorized = UncategorizedProperties(
                nameParts = 1.rangeTo(random.nextInt(2, 5)).map { index -> "$seed Uncategorized Name Part $index" },
                identifiers = 1.rangeTo(random.nextInt(2, 5)).map { index -> createIdentifier(seed, index, null)  },
                states = 1.rangeTo(random.nextInt(2, 5)).map { _ -> createState(random) },
                address = createAddress("Uncategorized Address $seed")
            ),
            legalEntity = createLegalEntity(seed),
            site = createSite(seed),
            additionalAddress = createAddress("Additional Address $seed")
        )
    }

    fun createLegalEntityBusinessPartner(seed: String = "Test"): BusinessPartner {
        return BusinessPartner.empty.copy(legalEntity = createLegalEntity(seed))
    }

    fun createSiteBusinessPartner(seed: String = "Test"): BusinessPartner {
        return BusinessPartner.empty.copy(
            legalEntity = createLegalEntity(seed),
            site = createSite(seed)
        )
    }

    fun createLegalEntity(seed: String = "Test"): LegalEntity {
        val random = seed.toRandom()
        return LegalEntity(
            bpnReference = createBpnReference(seed, "BPNL"),
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = metadata?.legalForms?.random(random) ?: "Legal Form $seed",
            identifiers = createIdentifiers(seed, random, metadata?.legalEntityIdentifierTypes),
            states = createStates(seed, random.nextInt(2, 5), random),
            confidenceCriteria = createConfidenceCriteria(random),
            isParticipantData = random.nextBoolean(),
            hasChanged = true,
            legalAddress = createAddress("Legal Address $seed")
        )
    }

    fun createSite(seed: String = "Test"): Site {
        val random = seed.toRandom()
        return Site(
            bpnReference = createBpnReference(seed, "BPNS"),
            siteName = "Site Name $seed",
            states = createStates(seed, 5, random),
            confidenceCriteria = createConfidenceCriteria(random),
            hasChanged = true,
            siteMainAddress = createAddress("Site Main Address $seed")
        )
    }

    fun createAddress(seed: String = "Test"): PostalAddress {
        val random = seed.toRandom()
        return PostalAddress(
            bpnReference = createBpnReference(seed, "$seed BPNA"),
            hasChanged = true,
            addressName = "$seed Address Name",
            identifiers = createIdentifiers(seed, random, metadata?.addressIdentifierTypes),
            states = createStates(seed, 5, random),
            confidenceCriteria = createConfidenceCriteria(random),
            physicalAddress = PhysicalAddress(
                geographicCoordinates = GeoCoordinate(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.toTypedArray().random(random).alpha2,
                administrativeAreaLevel1 = metadata?.adminAreas?.random(random) ?: "Admin Level 1 $seed",
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
                administrativeAreaLevel1 =  metadata?.adminAreas?.random(random) ?: "Alt Admin Level 1 $seed",
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            )
        )
    }

    private fun createBpnReference(seed: String, bpn: String): BpnReference {
        return BpnReference(
            referenceValue = "$bpn Reference Value $seed",
            desiredBpn = "Desired $bpn $seed",
            referenceType = BpnReferenceType.Bpn
        )
    }

    private fun createConfidenceCriteria(random: Random) : ConfidenceCriteria {
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

    private fun createIdentifiers(seed: String, random: Random, availableIdentifiers: List<String>?): List<Identifier> {
        return 1.rangeTo(random.nextInt(2, 5)).map { index -> createIdentifier(seed, index, availableIdentifiers?.random(random))  }
    }

    private fun createIdentifier(seed: String, index: Int, identifierType: String?): Identifier {
        return Identifier("$seed Id Value $index",  identifierType ?: "$seed Id Type $index", "$seed Issuing Body $index")
    }

    private fun createStates(seed: String, count: Int, random: Random): List<BusinessState> {
        return 1.rangeTo(count).map { _ -> createState(random) }
    }

    private fun createState(random: Random): BusinessState {
        val validFrom = random.nextInstant()
        return BusinessState(validFrom = validFrom, validTo = validFrom.plus(10, ChronoUnit.DAYS), BusinessStateType.entries.random())
    }

    private fun String.toRandom() =  Random(hashCode().toLong())



    private fun Random.nextInstant() = Instant.ofEpochSecond(nextLong(0, 365241780471))


    data class TestMetadataReferences(
        val legalForms: List<String>,
        val legalEntityIdentifierTypes: List<String>,
        val addressIdentifierTypes: List<String>,
        val adminAreas: List<String>
    )
}
