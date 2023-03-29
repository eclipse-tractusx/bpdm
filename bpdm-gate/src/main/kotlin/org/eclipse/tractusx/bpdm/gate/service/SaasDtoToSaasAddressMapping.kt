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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.BasePostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.*

class SaasDtoToSaasAddressMapping(private val postalAdress: BasePostalAddressDto) {

    fun geoCoordinates(): GeoCoordinatesSaas? {

        return if (postalAdress.geographicCoordinates?.longitude != null
                        && postalAdress.geographicCoordinates?.latitude != null)
            GeoCoordinatesSaas(longitude = postalAdress.geographicCoordinates!!.longitude,
                latitude = postalAdress.geographicCoordinates!!.latitude)
        else
            null
    }

    fun country(): CountrySaas{

        return CountrySaas(shortName = postalAdress.country, value = postalAdress.country.getName())
    }

    fun administrativeAreas(): Collection<AdministrativeAreaSaas> {

        val areas: MutableList<AdministrativeAreaSaas> = mutableListOf()

        if (postalAdress.administrativeAreaLevel1 != null) {
            areas.add(AdministrativeAreaSaas(value = postalAdress.administrativeAreaLevel1
                , type = TypeKeyNameUrlSaas(SaasAdministrativeAreaType.REGION.getTypeName())))
        }
        if (postalAdress.administrativeAreaLevel2 != null) {
            areas.add(AdministrativeAreaSaas(value = postalAdress.administrativeAreaLevel2
                , type = TypeKeyNameUrlSaas(SaasAdministrativeAreaType.COUNTY.getTypeName())))
        }
        return areas;
    }

    fun postcodes(): Collection<PostCodeSaas> {

        val postcodes: MutableList<PostCodeSaas> = mutableListOf()

        if (postalAdress.postCode != null) {
            postcodes.add(PostCodeSaas(value = postalAdress.postCode
                , type = TypeKeyNameUrlSaas(SaasPostCodeType.REGULAR.getTypeName())))
        }

        return postcodes;
    }

    fun localities(): Collection<LocalitySaas> {

        val localities: MutableList<LocalitySaas> = mutableListOf()

        localities.add(LocalitySaas(value = postalAdress.postCode
            , type = TypeKeyNameUrlSaas(SaasLocalityType.CITY.getTypeName())))

        if (postalAdress.districtLevel1 != null) {
            localities.add(LocalitySaas(value = postalAdress.districtLevel1
                , type = TypeKeyNameUrlSaas(SaasLocalityType.DISTRICT.getTypeName())))
        }
        if (postalAdress.districtLevel2 != null) {
            localities.add(LocalitySaas(value = postalAdress.districtLevel1
                , type = TypeKeyNameUrlSaas(SaasLocalityType.QUARTER.getTypeName())))
        }
        return localities;
    }

    fun thoroughfares(physicalAddress: PhysicalPostalAddressDto? ): Collection<ThoroughfareSaas> {

        val thoroughfares: MutableList<ThoroughfareSaas> = mutableListOf()

        if (postalAdress.street != null) {

            val street = ThoroughfareSaas(
                type = TypeKeyNameUrlSaas(SaasThoroughfareType.STREET.getTypeName()),
                name = postalAdress.street?.name,
                number = postalAdress.street?.houseNumber,
                shortName = postalAdress.street?.milestone,
                direction = postalAdress.street?.direction,
            )
            thoroughfares.add(street)
        }
        if (physicalAddress?.industrialZone != null) {
            thoroughfares.add(ThoroughfareSaas(name = physicalAddress.industrialZone
                , type = TypeKeyNameUrlSaas(SaasThoroughfareType.INDUSTRIAL_ZONE.getTypeName())))
        }

        return thoroughfares;
    }

    fun premesis(physicalAddress: PhysicalPostalAddressDto? ): Collection<PremiseSaas> {

        val premesis: MutableList<PremiseSaas> = mutableListOf()

        if (physicalAddress?.building != null) {
            premesis.add(PremiseSaas(value = physicalAddress.building
                , type = TypeKeyNameUrlSaas(SaasPremiseType.BUILDING.getTypeName())))
        }
        if (physicalAddress?.floor != null) {
            premesis.add(PremiseSaas(value = physicalAddress.floor
                , type = TypeKeyNameUrlSaas(SaasPremiseType.LEVEL.getTypeName())))
        }
        if (physicalAddress?.door != null) {
            premesis.add(PremiseSaas(value = physicalAddress.door
                , type = TypeKeyNameUrlSaas(SaasPremiseType.ROOM.getTypeName())))
        }

        return premesis;
    }

    fun postalDeliveryPoints(alternativeAddress : AlternativePostalAddressDto?): Collection<PostalDeliveryPointSaas> {

        val postalDeliveryPoints: MutableList<PostalDeliveryPointSaas> = mutableListOf()

        if (alternativeAddress != null) {
            postalDeliveryPoints.add(PostalDeliveryPointSaas(value = alternativeAddress.deliveryServiceNumber
                , type = TypeKeyNameUrlSaas(alternativeAddress.type.getTypeName())))
        }

        return postalDeliveryPoints;
    }

}