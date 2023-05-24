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
        return postalAdress.geographicCoordinates?.let {
            GeoCoordinatesSaas(longitude = it.longitude, latitude = it.latitude)
        }
    }

    fun country(): CountrySaas {
        return CountrySaas(shortName = postalAdress.country, value = postalAdress.country.getName())
    }

    fun administrativeAreas(): Collection<AdministrativeAreaSaas> {
        return listOfNotNull(
            postalAdress.administrativeAreaLevel1?.let {
                AdministrativeAreaSaas(
                    value = it,
                    type = SaasAdministrativeAreaType.REGION.toSaasTypeDto()
                )
            },
            postalAdress.administrativeAreaLevel2?.let {
                AdministrativeAreaSaas(
                    value = it,
                    type = SaasAdministrativeAreaType.COUNTY.toSaasTypeDto()
                )
            }
            // TODO Where can administrativeAreaLevel3 be stored in SaaS model? It's just ignored for now!
        )
    }

    fun postcodes(physicalAddress: PhysicalPostalAddressDto?): Collection<PostCodeSaas> {
        return listOfNotNull(
            postalAdress.postCode?.let {
                PostCodeSaas(
                    value = it,
                    type = SaasPostCodeType.REGULAR.toSaasTypeDto()
                )
            },
            physicalAddress?.companyPostCode?.let {
                PostCodeSaas(
                    value = it,
                    type = SaasPostCodeType.LARGE_MAIL_USER.toSaasTypeDto()
                )
            }
        )
    }

    fun localities(): Collection<LocalitySaas> {

        return listOfNotNull(
            LocalitySaas(
                value = postalAdress.city, type = SaasLocalityType.CITY.toSaasTypeDto()
            ),
            postalAdress.districtLevel1?.let {
                LocalitySaas(
                    value = it, type = SaasLocalityType.DISTRICT.toSaasTypeDto()
                )
            },
            postalAdress.districtLevel2?.let {
                LocalitySaas(
                    value = it, type = SaasLocalityType.QUARTER.toSaasTypeDto()
                )
            }
        )
    }

    fun thoroughfares(physicalAddress: PhysicalPostalAddressDto?): Collection<ThoroughfareSaas> {
        return listOfNotNull(
            postalAdress.street?.let {
                ThoroughfareSaas(
                    type = SaasThoroughfareType.STREET.toSaasTypeDto(),
                    name = it.name,
                    number = it.houseNumber,
                    shortName = it.milestone,
                    direction = it.direction
                )
            },
            physicalAddress?.industrialZone?.let {
                ThoroughfareSaas(
                    type = SaasThoroughfareType.INDUSTRIAL_ZONE.toSaasTypeDto(),
                    name = it
                )
            }
        )
    }

    fun premises(physicalAddress: PhysicalPostalAddressDto?): Collection<PremiseSaas> {
        return listOfNotNull(
            physicalAddress?.building?.let {
                PremiseSaas(
                    value = it,
                    type = SaasPremiseType.BUILDING.toSaasTypeDto()
                )
            },
            physicalAddress?.floor?.let {
                PremiseSaas(
                    value = it,
                    type = SaasPremiseType.LEVEL.toSaasTypeDto()
                )
            },
            physicalAddress?.door?.let {
                PremiseSaas(
                    value = it,
                    type = SaasPremiseType.ROOM.toSaasTypeDto()
                )
            }
        )
    }

    fun postalDeliveryPoints(alternativeAddress: AlternativePostalAddressDto?): Collection<PostalDeliveryPointSaas> {
        val postalDeliveryPointSaas = when (alternativeAddress?.deliveryServiceType) {
            DeliveryServiceType.PO_BOX ->
                PostalDeliveryPointSaas(
                    value = alternativeAddress.deliveryServiceNumber,
                    type = SaasPostalDeliveryPointType.POST_OFFICE_BOX.toSaasTypeDto()
                )

            DeliveryServiceType.PRIVATE_BAG ->
                PostalDeliveryPointSaas(
                    value = alternativeAddress.deliveryServiceNumber,
                    type = SaasPostalDeliveryPointType.MAILBOX.toSaasTypeDto()
                )

            else -> null
        }

        return listOfNotNull(postalDeliveryPointSaas)
    }

}