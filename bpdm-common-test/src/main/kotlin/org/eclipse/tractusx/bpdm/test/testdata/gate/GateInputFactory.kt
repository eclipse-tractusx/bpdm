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

package org.eclipse.tractusx.bpdm.test.testdata.gate

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.StreetV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.AddressRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.LegalEntityRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.SiteRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import java.time.LocalDate
import kotlin.random.Random

class GateInputFactory(
    private val testMetadata: TestMetadata,
    private val testRunData: TestRunData?
) {
    fun createAllFieldsFilled(seed: String, transform: (BusinessPartnerInputV7Request) -> BusinessPartnerInputV7Request = {it}): InputTestData {
        return InputTestData(seed, transform(SeededTestDataCreator(seed).createAllFieldsFilled()))
    }

    fun buildRelation(
        externalId: String,
        relationType: RelationTypeV7,
        businessPartnerSourceExternalId: String,
        businessPartnerTargetExternalId: String,
        seed: String = externalId,
    ): RelationPutV7Entry{
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return RelationPutV7Entry(
            externalId = externalId,
            relationType = relationType,
            businessPartnerSourceExternalId = businessPartnerSourceExternalId,
            businessPartnerTargetExternalId = businessPartnerTargetExternalId,
            reasonCode = testMetadata.reasonCodes.random(random),
            validityPeriods = listOf(
                RelationValidityPeriodV7Dto(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31)))
        )
    }

    inner class SeededTestDataCreator(
        private val seed: String,
    ){
        private val longSeed = seed.hashCode().toLong()
        private val random = Random(longSeed)
        private val listRange = 1 .. 3


        fun createAllFieldsFilled(): BusinessPartnerInputV7Request{

            return BusinessPartnerInputV7Request(
                externalId = testRunData?.let { "${seed}_${testRunData.testTime}" } ?: seed ,
                nameParts = listRange.map { "Name Part $seed $it" },
                identifiers = emptyList(),
                roles = BusinessPartnerRole.entries,
                isOwnCompanyData = true,
                states = emptyList(),
                legalEntity = LegalEntityRepresentationInputV7Dto(
                    legalEntityBpn = "BPNL $seed",
                    legalName = "Legal Name $seed",
                    shortName = "Short Name $seed",
                    legalForm = testMetadata.legalForms.random(random),
                    ownershipUltimate = null,
                    ultimateOwnerBpnl = null
                ),
                site = SiteRepresentationInputV7Dto(
                    siteBpn = "BPNS $seed",
                    name = "Site Name $seed"
                ),
                address = AddressRepresentationInputV7Dto(
                    addressBpn = "BPNA $seed",
                    name = "Address Name $seed",
                    addressType = AddressType.AdditionalAddress,
                    physicalPostalAddress = createPhysicalAddress(),
                    alternativePostalAddress = createAlternativeAddress()
                )
            )
        }

        private fun createPhysicalAddress(): PhysicalPostalAddressV7Dto{
            return PhysicalPostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = getCountryReference(),
                administrativeAreaLevel1 = testMetadata.adminAreas.random(random),
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
            )
        }

        private fun createAlternativeAddress(): AlternativePostalAddressV7Dto{
            return AlternativePostalAddressV7Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = getCountryReference(),
                administrativeAreaLevel1 =  testMetadata.adminAreas.random(random),
                postalCode = "Alt Postal Code $seed",
                city = "Alt City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            )
        }

        private fun getCountryReference(): String =
            testMetadata.countries.random(random)
    }
}

data class InputTestData(
    val seed: String,
    val request: BusinessPartnerInputV7Request
)

data class TestMetadata(
    val identifierTypes: List<String>,
    val legalForms: List<String>,
    val adminAreas: List<String>,
    val reasonCodes: List<String>,
    val countries: List<String>
)

fun BusinessPartnerInputV7Request.withoutAnyBpn() = withoutLegalEntityBpn().withoutSiteBpn().withoutAddressBpn()
fun BusinessPartnerInputV7Request.withAddressType(addressType: AddressType?) = copy(address = address.copy(addressType = addressType))
fun BusinessPartnerInputV7Request.withoutLegalEntityBpn() = copy(legalEntity = legalEntity.copy(legalEntityBpn = null))
fun BusinessPartnerInputV7Request.withoutSiteBpn() = copy(site = site.copy(siteBpn = null))
fun BusinessPartnerInputV7Request.withoutAddressBpn() = copy(address = address.copy(addressBpn = null))