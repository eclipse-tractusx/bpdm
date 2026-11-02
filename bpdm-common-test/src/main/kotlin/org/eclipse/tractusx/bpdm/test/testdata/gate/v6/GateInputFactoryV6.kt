/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v6

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.StreetV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.AddressRepresentationInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.LegalEntityRepresentationInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.SiteRepresentationInputV6Dto
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import kotlin.random.Random

class GateInputFactoryV6(
    private val testMetadata: TestMetadata,
    private val testRunData: TestRunData?
) {
    fun createFullValid(seed: String, externalId: String = seed, withTestRunContext: Boolean = true): BusinessPartnerInputV6Request {
        return SeededTestDataCreator(seed).createAllFieldsFilled().copy(externalId = testRunData?.toExternalId(externalId)?.takeIf { withTestRunContext } ?: externalId)
    }

    inner class SeededTestDataCreator(
        private val seed: String,
    ){
        private val longSeed = seed.hashCode().toLong()
        private val random = Random(longSeed)
        private val listRange = 1 .. 3


        fun createAllFieldsFilled(): BusinessPartnerInputV6Request{

            return BusinessPartnerInputV6Request(
                externalId = testRunData?.let { "${seed}_${testRunData.testTime}" } ?: seed ,
                nameParts = listRange.map { "Name Part $seed $it" },
                identifiers = emptyList(),
                roles = BusinessPartnerRole.entries,
                isOwnCompanyData = true,
                states = emptyList(),
                legalEntity = LegalEntityRepresentationInputV6Dto(
                    legalEntityBpn = "BPNL $seed",
                    legalName = "Legal Name $seed",
                    shortName = "Short Name $seed",
                    legalForm = testMetadata.legalForms.random(random),
                    ownershipUltimate = null,
                    ultimateOwnerBpnl = null
                ),
                site = SiteRepresentationInputV6Dto(
                    siteBpn = "BPNS $seed",
                    name = "Site Name $seed"
                ),
                address = AddressRepresentationInputV6Dto(
                    addressBpn = "BPNA $seed",
                    name = "Address Name $seed",
                    addressType = AddressType.AdditionalAddress,
                    physicalPostalAddress = createPhysicalAddress(),
                    alternativePostalAddress = createAlternativeAddress()
                )
            )
        }

        private fun createPhysicalAddress(): PhysicalPostalAddressV6Dto{
            return PhysicalPostalAddressV6Dto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextDouble(), latitude = random.nextDouble(), altitude = random.nextDouble()),
                country = getCountryReference(),
                administrativeAreaLevel1 = testMetadata.adminAreas.random(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetV6Dto(
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

        private fun createAlternativeAddress(): AlternativePostalAddressV6Dto{
            return AlternativePostalAddressV6Dto(
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
