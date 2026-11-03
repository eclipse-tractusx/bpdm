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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.*
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.AddressRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.LegalEntityRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.SiteRepresentationInputV7Dto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

class BusinessPartnerInputRequestV7Factory(
    val testMetadata: GateTestMetadataV7
) {
    fun fromSeed(seed: String): BusinessPartnerInputV7Request = SeededCreator(seed).create()

    private inner class SeededCreator(private val seed: String) {

        private val random = Random(seed.hashCode())

        fun create(): BusinessPartnerInputV7Request = BusinessPartnerInputV7Request(
            externalId = seed,
            nameParts = (1..2).map { "Name Part $it $seed" },
            identifiers = createIdentifiers(),
            states = createStates(),
            roles = BusinessPartnerRole.entries,
            isOwnCompanyData = random.nextBoolean(),
            legalEntity = createLegalEntity(),
            site = createSite(),
            address = createAddress(),
            externalSequenceTimestamp = randomInstant(2020, 2025),
            scriptVariants = createScriptVariants()
        )

        private fun createIdentifiers(): List<BusinessPartnerIdentifierV7Dto> =
            testMetadata.identifierTypes.shuffled(random).take(2).mapIndexed { i, type ->
                BusinessPartnerIdentifierV7Dto(
                    type = type,
                    value = "Identifier Value ${i + 1} $seed",
                    issuingBody = "Issuing Body ${i + 1} $seed"
                )
            }

        private fun createStates(): List<BusinessPartnerStateV7Dto> =
            (1..2).map {
                BusinessPartnerStateV7Dto(
                    validFrom = randomLocalDateTime(2000, 2015),
                    validTo = randomLocalDateTime(2016, 2030),
                    type = BusinessStateType.entries.random(random)
                )
            }

        private fun createLegalEntity(): LegalEntityRepresentationInputV7Dto =
            LegalEntityRepresentationInputV7Dto(
                legalEntityBpn = "BPNL $seed",
                legalName = "Legal Name $seed",
                shortName = "Short Name $seed",
                legalForm = testMetadata.legalForms.random(random),
                states = createStates()
            )

        private fun createSite(): SiteRepresentationInputV7Dto =
            SiteRepresentationInputV7Dto(
                siteBpn = "BPNS $seed",
                name = "Site Name $seed",
                states = createStates()
            )

        private fun createAddress(): AddressRepresentationInputV7Dto =
            AddressRepresentationInputV7Dto(
                addressBpn = "BPNA $seed",
                name = "Address Name $seed",
                addressType = AddressType.entries.random(random),
                physicalPostalAddress = createPhysicalAddress(),
                alternativePostalAddress = createAlternativeAddress(),
                states = createStates()
            )

        private fun createPhysicalAddress(): PhysicalPostalAddressV7Dto =
            PhysicalPostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(
                    longitude = random.nextDouble(-180.0, 180.0),
                    latitude = random.nextDouble(-90.0, 90.0),
                    altitude = random.nextDouble(0.0, 5000.0)
                ),
                country = getCountryReference(),
                administrativeAreaLevel1 = testMetadata.adminAreas.random(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetV7Dto(
                    namePrefix = "Name Prefix $seed",
                    additionalNamePrefix = "Additional Name Prefix $seed",
                    name = "Street Name $seed",
                    nameSuffix = "Name Suffix $seed",
                    additionalNameSuffix = "Additional Name Suffix $seed",
                    houseNumber = "House Number $seed",
                    houseNumberSupplement = "House Number Supplement $seed",
                    milestone = "Milestone $seed",
                    direction = "Direction $seed"
                ),
                companyPostalCode = "Company Postal Code $seed",
                industrialZone = "Industrial Zone $seed",
                building = "Building $seed",
                floor = "Floor $seed",
                door = "Door $seed",
                taxJurisdictionCode = random.nextInt(100, 1000).toString()
            )

        private fun createAlternativeAddress(): AlternativePostalAddressV7Dto =
            AlternativePostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(
                    longitude = random.nextDouble(-180.0, 180.0),
                    latitude = random.nextDouble(-90.0, 90.0),
                    altitude = random.nextDouble(0.0, 5000.0)
                ),
                country = getCountryReference(),
                administrativeAreaLevel1 = testMetadata.adminAreas.random(random),
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            )

        private fun getCountryReference(): String =
            testMetadata.countries.random(random)

        private fun createScriptVariants(): List<BusinessPartnerScriptVariantV7Dto> =
            testMetadata.scriptVariants.shuffled(random).take(2).mapIndexed { idx, scriptCode ->
                val i = idx + 1
                BusinessPartnerScriptVariantV7Dto(
                    scriptCode = scriptCode,
                    nameParts = (1..2).map { "Name Part Variant $it $i $seed" },
                    legalEntity = LegalEntityScriptVariantV7Dto(
                        legalName = "Legal Name Variant $i $seed",
                        shortName = "Short Name Variant $i $seed"
                    ),
                    site = SiteScriptVariantV7Dto(
                        name = "Site Name Variant $i $seed"
                    ),
                    address = AddressScriptVariantV7Dto(
                        name = "Address Name Variant $i $seed",
                        physicalAddress = PhysicalAddressScriptVariantV7Dto(
                            city = "City Variant $i $seed",
                            district = "District Variant $i $seed",
                            street = StreetScriptVariantV7Dto(
                                name = "Street Name Variant $i $seed",
                                direction = "Direction Variant $i $seed"
                            ),
                            industrialZone = "Industrial Zone Variant $i $seed",
                            building = "Building Variant $i $seed",
                            floor = "Floor Variant $i $seed",
                            door = "Door Variant $i $seed"
                        ),
                        alternativeAddress = AlternativeAddressScriptVariantV7Dto(
                            city = "Alt City Variant $i $seed"
                        )
                    )
                )
            }

        private fun randomLocalDateTime(fromYear: Int, toYear: Int): LocalDateTime {
            val year = random.nextInt(fromYear, toYear + 1)
            val month = random.nextInt(1, 13)
            val day = random.nextInt(1, 29)
            return LocalDateTime.of(year, month, day, 0, 0)
        }

        private fun randomInstant(fromYear: Int, toYear: Int): Instant {
            val from = LocalDateTime.of(fromYear, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC)
            val to = LocalDateTime.of(toYear, 12, 31, 0, 0).toEpochSecond(ZoneOffset.UTC)
            return Instant.ofEpochSecond(random.nextLong(from, to))
        }
    }
}
