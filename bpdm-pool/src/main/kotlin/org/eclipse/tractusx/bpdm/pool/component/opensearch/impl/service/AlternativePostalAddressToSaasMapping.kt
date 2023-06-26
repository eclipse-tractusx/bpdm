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

import org.eclipse.tractusx.bpdm.common.dto.saas.GeoCoordinatesSaas
import org.eclipse.tractusx.bpdm.pool.entity.AlternativePostalAddress

class AlternativePostalAddressToSaasMapping(private val postalAdress: AlternativePostalAddress) : AddressToSaasMapping {

    override fun geoCoordinates(): GeoCoordinatesSaas? {

        return (postalAdress.geographicCoordinates?.longitude).let {

            GeoCoordinatesSaas(
                longitude = postalAdress.geographicCoordinates?.longitude,
                latitude = postalAdress.geographicCoordinates?.latitude
            )
        }
    }

    override fun country(): String {

        return postalAdress.country.alpha2
    }

    override fun administrativeAreas(): Collection<String> {

        return listOfNotNull(

            (postalAdress.administrativeAreaLevel1)?.let {
                postalAdress.administrativeAreaLevel1.regionCode
            }
        )
    }

    override fun postcodes(): Collection<String> {

        return listOfNotNull(

            postalAdress.postCode
        )
    }

    override fun localities(): Collection<String> {

        return listOfNotNull(

            postalAdress.city
        )
    }

    override fun thoroughfares(): Collection<String> {

        return listOfNotNull(

            postalAdress.street?.name,
            postalAdress.street?.houseNumber,
            postalAdress.street?.milestone,
            postalAdress.street?.direction,
        )
    }


    override fun premises(): Collection<String> {

        return listOf()
    }

    override fun postalDeliveryPoints(): Collection<String> {

        return listOfNotNull(

            postalAdress.deliveryServiceNumber
        )
    }

}