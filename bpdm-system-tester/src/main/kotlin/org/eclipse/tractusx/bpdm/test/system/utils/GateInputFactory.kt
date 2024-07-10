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

package org.eclipse.tractusx.bpdm.test.system.utils

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.test.system.config.TestRunData
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

class GateInputFactory(
    private val testMetadata: TestMetadata,
    private val testRunData: TestRunData
) {
    val genericFullValidWithSiteWithoutAnyBpn = createAllFieldsFilled("genericFullValidWithSiteWithoutAnyBpn")
    { it.withoutAnyBpn().withAddressType(null) }

    fun createAllFieldsFilled(seed: String, transform: (BusinessPartnerInputRequest) -> BusinessPartnerInputRequest = {it}): InputTestData {
        return InputTestData(seed, transform(SeededTestDataCreator(seed).createAllFieldsFilled()))
    }

    fun createFullValid(seed: String, externalId: String = seed): BusinessPartnerInputRequest {
        return SeededTestDataCreator(seed).createAllFieldsFilled().copy(externalId = testRunData.toExternalId(externalId))
    }

    inner class SeededTestDataCreator(
        private val seed: String,
    ){
        private val longSeed = seed.hashCode().toLong()
        private val random = Random(longSeed)
        private val listRange = 1 .. 3


        fun createAllFieldsFilled(): BusinessPartnerInputRequest{

            return BusinessPartnerInputRequest(
                externalId = "${seed}_${testRunData.testTime}",
                nameParts = listRange.map { "Name Part $seed $it" },
                identifiers = emptyList(),
                roles = BusinessPartnerRole.entries,
                isOwnCompanyData = random.nextBoolean(),
                states = emptyList(),
                legalEntity = LegalEntityRepresentationInputDto(
                    legalEntityBpn = "BPNL $seed",
                    legalName = "Legal Name $seed",
                    shortName = "Short Name $seed",
                    legalForm = testMetadata.legalForms.random(random)
                ),
                site = SiteRepresentationInputDto(
                    siteBpn = "BPNS $seed",
                    name = "Site Name $seed"
                ),
                address = AddressRepresentationInputDto(
                    addressBpn = "BPNA $seed",
                    name = "Address Name $seed",
                    addressType = AddressType.AdditionalAddress,
                    physicalPostalAddress = createPhysicalAddress(),
                    alternativePostalAddress = createAlternativeAddress()
                )
            )
        }


        private fun createIdentifiers(): List<BusinessPartnerIdentifierDto>{
            return listRange.map { testMetadata.identifierTypes.random(random) }
                .mapIndexed{ index, type -> BusinessPartnerIdentifierDto(type = type, value = "Identifier Value $seed $index", issuingBody = "Issuing Body $seed $index") }
        }

        private fun createStates(): List<BusinessPartnerStateDto>{
            return random.nextTime().let {
                listRange.runningFold(Pair(it, it.plus(random.nextDuration()))){ current, _ -> Pair(current.second, current.second.plus(random.nextDuration())) }
            }.map { (validFrom, validTo) -> BusinessPartnerStateDto(validFrom = validFrom, validTo = validTo, BusinessStateType.entries.random(random)) }
        }

        private fun createPhysicalAddress(): PhysicalPostalAddressDto{
            return PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = testMetadata.adminAreas.random(random),
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
                taxJurisdictionCode = "123"
            )
        }

        private fun createAlternativeAddress(): AlternativePostalAddressDto{
            return AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 =  testMetadata.adminAreas.random(random),
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            )
        }

        private fun Random.nextInstant() = Instant.ofEpochSecond(nextLong(0, 365241780471))
        private fun Random.nextTime() = nextInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()
        private fun Random.nextDuration() = Duration.ofHours(nextLong(0, 10000))

    }
}

data class InputTestData(
    val seed: String,
    val request: BusinessPartnerInputRequest
)

data class TestMetadata(
    val identifierTypes: List<String>,
    val legalForms: List<String>,
    val adminAreas: List<String>
)

fun BusinessPartnerInputRequest.withoutAnyBpn() = withoutLegalEntityBpn().withoutSiteBpn().withoutAddressBpn()
fun BusinessPartnerInputRequest.withAddressType(addressType: AddressType?) = copy(address = address.copy(addressType = addressType))
fun BusinessPartnerInputRequest.withoutLegalEntityBpn() = copy(legalEntity = legalEntity.copy(legalEntityBpn = null))
fun BusinessPartnerInputRequest.withoutSiteBpn() = copy(site = site.copy(siteBpn = null))
fun BusinessPartnerInputRequest.withoutAddressBpn() = copy(address = address.copy(addressBpn = null))