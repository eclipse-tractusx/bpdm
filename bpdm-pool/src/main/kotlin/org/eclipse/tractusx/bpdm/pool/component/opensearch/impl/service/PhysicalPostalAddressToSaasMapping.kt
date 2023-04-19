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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import org.eclipse.tractusx.bpdm.common.dto.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.common.model.*
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalPostalAddress

class PhysicalPostalAddressToSaasMapping(private val postalAdress: PhysicalPostalAddress) : AddressToSaasMapping {

    override fun geoCoordinates(): GeoCoordinatesSaas? {

        return if (postalAdress.geographicCoordinates?.longitude != null)
            GeoCoordinatesSaas(longitude = postalAdress.geographicCoordinates.longitude,
                latitude = postalAdress.geographicCoordinates.latitude)
        else
            null
    }

    override fun country(): String {

        return postalAdress.country.alpha2;
    }

    override fun administrativeAreas(): Collection<String> {

        val areas: MutableList<String> = mutableListOf()

        if (postalAdress.administrativeAreaLevel1 != null) {
            areas.add(postalAdress.administrativeAreaLevel1.regionCode)
        }
        if (postalAdress.administrativeAreaLevel2 != null) {
            areas.add(postalAdress.administrativeAreaLevel2)
        }
        return areas;
    }

    override fun postcodes(): Collection<String> {

        val postcodes: MutableList<String> = mutableListOf()

        if (postalAdress.postCode != null) {
            postcodes.add(postalAdress.postCode)
        }

        return postcodes;
    }

    override fun localities(): Collection<String> {

        val localities: MutableList<String> = mutableListOf()

        if (postalAdress.postCode != null) {
            localities.add(postalAdress.city)
        }

        if (postalAdress.districtLevel1 != null) {
            localities.add(postalAdress.districtLevel1)
        }
        if (postalAdress.districtLevel2 != null) {
            localities.add(postalAdress.districtLevel2)
        }
        return localities;
    }

    override fun thoroughfares( ): Collection<String> {

        val thoroughfares: MutableList<String> = mutableListOf()

        if (postalAdress.street != null) {

            if (postalAdress.street.name != null) {
                thoroughfares.add(postalAdress.street.name,)
            }
            if (postalAdress.street.houseNumber != null) {
                thoroughfares.add(postalAdress.street.houseNumber,)
            }
            if (postalAdress.street.milestone != null) {
                thoroughfares.add(postalAdress.street.milestone,)
            }
            if (postalAdress.street.direction != null) {
                thoroughfares.add(postalAdress.street.direction,)
            }

        }
        if (postalAdress.industrialZone != null) {
            thoroughfares.add(postalAdress.industrialZone)
        }

        return thoroughfares;
    }

    override fun premises( ): Collection<String> {

        val premesis: MutableList<String> = mutableListOf()

        if (postalAdress.building != null) {
            premesis.add(postalAdress.building)
        }
        if (postalAdress.floor != null) {
            premesis.add(postalAdress.floor)
        }
        if (postalAdress.door != null) {
            premesis.add(postalAdress.door)
        }

        return premesis;
    }

    override fun postalDeliveryPoints(): Collection<String> {

        return listOf();
    }

}