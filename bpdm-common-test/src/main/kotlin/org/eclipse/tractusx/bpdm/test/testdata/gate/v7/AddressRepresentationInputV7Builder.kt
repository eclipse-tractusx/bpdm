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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import java.time.LocalDateTime
import kotlin.random.Random

class AddressRepresentationInputV7Builder(seed: String) {

    private val random = Random(seed.hashCode())

    private var addressBpn: String = "BPNA $seed"
    private var name: String = "Address Name $seed"
    private var addressType: AddressType = AddressType.AdditionalAddress
    private var physicalPostalAddress: PhysicalPostalAddressDto = PhysicalPostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(
            longitude = random.nextDouble(),
            latitude = random.nextDouble(),
            altitude = random.nextDouble()
        ),
        country = CountryCode.entries.toTypedArray().random(random),
        administrativeAreaLevel1 = "Admin Level 1 $seed",
        administrativeAreaLevel2 = "Admin Level 2 $seed",
        administrativeAreaLevel3 = "Admin Level 3 $seed",
        postalCode = "Postal Code $seed",
        city = "City $seed",
        district = "District $seed",
        street = StreetDto(
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
        taxJurisdictionCode = "123"
    )
    private var alternativePostalAddress: AlternativePostalAddressDto = AlternativePostalAddressDto(
        geographicCoordinates = GeoCoordinateDto(
            longitude = random.nextDouble(),
            latitude = random.nextDouble(),
            altitude = random.nextDouble()
        ),
        country = CountryCode.entries.toTypedArray().random(random),
        administrativeAreaLevel1 = "Alt Admin Level 1 $seed",
        postalCode = "Alt Postal Code $seed",
        city = "Alt City $seed",
        deliveryServiceNumber = "Delivery Service Number $seed",
        deliveryServiceType = DeliveryServiceType.entries.random(random),
        deliveryServiceQualifier = "Delivery Service Qualifier $seed"
    )
    private var states: Collection<BusinessPartnerStateDto> = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            validTo = LocalDateTime.of(2025, 1, 1, 0, 0),
            type = BusinessStateType.ACTIVE
        )
    )

    fun withAddressBpn(addressBpn: String) = apply { this.addressBpn = addressBpn }
    fun withName(name: String) = apply { this.name = name }
    fun withAddressType(addressType: AddressType) = apply { this.addressType = addressType }
    fun withPhysicalPostalAddress(physicalPostalAddress: PhysicalPostalAddressDto) = apply { this.physicalPostalAddress = physicalPostalAddress }
    fun withAlternativePostalAddress(alternativePostalAddress: AlternativePostalAddressDto) = apply { this.alternativePostalAddress = alternativePostalAddress }
    fun withStates(states: Collection<BusinessPartnerStateDto>) = apply { this.states = states }

    fun build(): AddressRepresentationInputDto = AddressRepresentationInputDto(
        addressBpn = addressBpn,
        name = name,
        addressType = addressType,
        physicalPostalAddress = physicalPostalAddress,
        alternativePostalAddress = alternativePostalAddress,
        states = states
    )
}
