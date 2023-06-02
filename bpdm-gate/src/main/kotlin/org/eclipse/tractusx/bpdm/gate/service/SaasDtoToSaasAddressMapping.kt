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
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressGateDto

class SaasDtoToSaasAddressMapping(private val postalAdress: BasePostalAddressDto) {

    fun geoCoordinates(): GeoCoordinatesSaas? {
        return postalAdress.geographicCoordinates?.let {
            GeoCoordinatesSaas(longitude = it.longitude, latitude = it.latitude)
        }
    }

    fun country(): CountrySaas {
        return CountrySaas(shortName = postalAdress.country, value = postalAdress.country.getName())
    }

    fun administrativeAreas(physicalAddress: PhysicalPostalAddressGateDto?): Collection<AdministrativeAreaSaas> {
        return listOfNotNull(
            physicalAddress?.areaPart?.administrativeAreaLevel1?.let {
                AdministrativeAreaSaas(
                    value = it,
                    type = SaasAdministrativeAreaType.REGION.toSaasTypeDto()
                )
            },
            physicalAddress?.areaPart?.administrativeAreaLevel2?.let {
                AdministrativeAreaSaas(
                    value = it,
                    type = SaasAdministrativeAreaType.COUNTY.toSaasTypeDto()
                )
            }
            // TODO Where can administrativeAreaLevel3 be stored in SaaS model? It's just ignored for now!
        )
    }

    fun administrativeAreas(alternateAddress: AlternativePostalAddressDto?): Collection<AdministrativeAreaSaas> {
        return listOfNotNull(
            alternateAddress?.areaPart?.administrativeAreaLevel1?.let {
                AdministrativeAreaSaas(
                    value = it,
                    type = SaasAdministrativeAreaType.REGION.toSaasTypeDto()
                )
            },
        )
    }


    fun postcodes(physicalAddress: PhysicalPostalAddressGateDto?): Collection<PostCodeSaas> {
        return listOfNotNull(
            postalAdress.postalCode?.let {
                PostCodeSaas(
                    value = it,
                    type = SaasPostCodeType.REGULAR.toSaasTypeDto()
                )
            },
            physicalAddress?.basePhysicalAddress?.companyPostalCode?.let {
                PostCodeSaas(
                    value = it,
                    type = SaasPostCodeType.LARGE_MAIL_USER.toSaasTypeDto()
                )
            }
        )
    }

    fun localities(alternateAddress: AlternativePostalAddressDto?): Collection<LocalitySaas> {

        return listOfNotNull(
            LocalitySaas(
                value = postalAdress.city, type = SaasLocalityType.CITY.toSaasTypeDto()
            )
        )
    }

    fun localities(physicalAddress: PhysicalPostalAddressGateDto?): Collection<LocalitySaas> {

        return listOfNotNull(
            LocalitySaas(
                value = postalAdress.city, type = SaasLocalityType.CITY.toSaasTypeDto()
            ),
            physicalAddress?.areaPart?.district?.let {
                LocalitySaas(
                    value = it, type = SaasLocalityType.DISTRICT.toSaasTypeDto()
                )
            },
        )
    }

    fun thoroughfares(physicalAddress: PhysicalPostalAddressGateDto?): Collection<ThoroughfareSaas> {
        return listOfNotNull(
            physicalAddress?.street?.let {
                ThoroughfareSaas(
                    type = SaasThoroughfareType.STREET.toSaasTypeDto(),
                    name = it.name,
                    number = it.houseNumber,
                    shortName = it.milestone,
                    direction = it.direction
                )
            },
            physicalAddress?.basePhysicalAddress?.industrialZone?.let {
                ThoroughfareSaas(
                    type = SaasThoroughfareType.INDUSTRIAL_ZONE.toSaasTypeDto(),
                    name = it
                )
            }
        )
    }

    fun premises(physicalAddress: PhysicalPostalAddressGateDto?): Collection<PremiseSaas> {
        return listOfNotNull(
            physicalAddress?.basePhysicalAddress?.building?.let {
                PremiseSaas(
                    value = it,
                    type = SaasPremiseType.BUILDING.toSaasTypeDto()
                )
            },
            physicalAddress?.basePhysicalAddress?.floor?.let {
                PremiseSaas(
                    value = it,
                    type = SaasPremiseType.LEVEL.toSaasTypeDto()
                )
            },
            physicalAddress?.basePhysicalAddress?.door?.let {
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