/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.service

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.response.PhysicalPostalAddressResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.AddressSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.TypeValueSaas
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.pool.util.ResponseValues
import org.eclipse.tractusx.bpdm.pool.util.SaasValues
import org.junit.jupiter.api.Test

class SaasAddressToDtoMappingTest {

    @Test
    fun addressPartnerTest() {

        checkMappingResponsePhysicalAddress(ResponseValues.addressPartner1.physicalPostalAddress, SaasValues.addressPartnerSaas1.addresses.first())
        checkMappingResponsePhysicalAddress(ResponseValues.addressPartner2.physicalPostalAddress, SaasValues.addressPartnerSaas2.addresses.first())
        checkMappingResponsePhysicalAddress(ResponseValues.addressPartner3.physicalPostalAddress, SaasValues.addressPartnerSaas3.addresses.first())
    }

    @Test
    fun saasPhysicalAddressMappingTest() {

        val addressesMapping = SaasAddressesMapping(SaasValues.addressPartner1.addresses)
        val address = addressesMapping.saasPhysicalAddressMapping()!!
        val physicalAddressDto = SaasMappings.toPhysicalAddress(address, "")

        checkMappingDtoPhysicalAddress(physicalAddressDto, SaasValues.addressPartner1.addresses.first())

    }

    private fun checkMappingResponsePhysicalAddress(physicalAddressDto: PhysicalPostalAddressResponse, addressSaas: AddressSaas) {

        val baseAddressDto = physicalAddressDto.baseAddress
        val areaDto = physicalAddressDto.areaPart
        val streetDto = physicalAddressDto.street
//TODO        Assertions.assertThat(baseAddressDto.administrativeAreaLevel1?.name).isEqualTo(findValue(addressSaas.administrativeAreas, SaasAdministrativeAreaType.REGION))
        Assertions.assertThat(areaDto.administrativeAreaLevel2).isEqualTo(findValue(addressSaas.administrativeAreas, SaasAdministrativeAreaType.COUNTY))
        Assertions.assertThat(areaDto.administrativeAreaLevel3).isEqualTo(null)
        Assertions.assertThat(baseAddressDto.city).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.CITY))
        Assertions.assertThat(baseAddressDto.country.technicalKey).isEqualTo(addressSaas.country?.shortName)
        Assertions.assertThat(areaDto.district).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.DISTRICT))
        Assertions.assertThat(baseAddressDto.geographicCoordinates?.latitude).isEqualTo(addressSaas.geographicCoordinates?.latitude)
        Assertions.assertThat(baseAddressDto.geographicCoordinates?.longitude).isEqualTo(addressSaas.geographicCoordinates?.longitude)
        Assertions.assertThat(baseAddressDto.postalCode).isEqualTo(findValue(addressSaas.postCodes, SaasPostCodeType.REGULAR))
        Assertions.assertThat(streetDto?.name).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.name)
        Assertions.assertThat(streetDto?.houseNumber).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.number)
        Assertions.assertThat(streetDto?.direction).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.direction)
        Assertions.assertThat(streetDto?.milestone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.shortName)

        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.industrialZone)
            .isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)?.name)
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.building).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.BUILDING))
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.floor).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.LEVEL))
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.door).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.ROOM))
    }

    private fun checkMappingDtoPhysicalAddress(physicalAddressDto: PhysicalPostalAddressDto, addressSaas: AddressSaas) {

        val baseAddressDto = physicalAddressDto.baseAddress
        val areaDto = physicalAddressDto.areaPart
        val streetDto = physicalAddressDto.street
        Assertions.assertThat(areaDto.administrativeAreaLevel1).isEqualTo(findValue(addressSaas.administrativeAreas, SaasAdministrativeAreaType.REGION))
        Assertions.assertThat(areaDto.administrativeAreaLevel2).isEqualTo(findValue(addressSaas.administrativeAreas, SaasAdministrativeAreaType.COUNTY))
        Assertions.assertThat(areaDto.administrativeAreaLevel3).isEqualTo(null)
        Assertions.assertThat(baseAddressDto.city).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.CITY))
        Assertions.assertThat(baseAddressDto.country).isEqualTo(addressSaas.country?.shortName)
        Assertions.assertThat(areaDto.district).isEqualTo(findValue(addressSaas.localities, SaasLocalityType.DISTRICT))
        Assertions.assertThat(baseAddressDto.geographicCoordinates?.latitude).isEqualTo(addressSaas.geographicCoordinates?.latitude)
        Assertions.assertThat(baseAddressDto.geographicCoordinates?.longitude).isEqualTo(addressSaas.geographicCoordinates?.longitude)
        Assertions.assertThat(baseAddressDto.postalCode).isEqualTo(findValue(addressSaas.postCodes, SaasPostCodeType.REGULAR))
        Assertions.assertThat(streetDto?.name).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.name)
        Assertions.assertThat(streetDto?.houseNumber).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.number)
        Assertions.assertThat(streetDto?.direction).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.direction)
        Assertions.assertThat(streetDto?.milestone).isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.STREET)?.shortName)

        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.industrialZone)
            .isEqualTo(findObject(addressSaas.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)?.name)
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.building).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.BUILDING))
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.floor).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.LEVEL))
        Assertions.assertThat(physicalAddressDto.basePhysicalAddress.door).isEqualTo(findValue(addressSaas.premises, SaasPremiseType.ROOM))
    }

    private fun <T : TypeValueSaas> findValue(values: Collection<T>, enumType: SaasType): String? {
        return findObject(values, enumType)?.value
    }

    private fun <T : TypeValueSaas> findObject(values: Collection<T>, enumType: SaasType): T? {
        return values.find { it.type?.technicalKey == enumType.getTechnicalKey() }
    }
}