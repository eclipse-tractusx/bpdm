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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.saas.AddressSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.TypeValueSaas
import org.eclipse.tractusx.bpdm.common.model.*

class SaasAddressToDtoMapping(private val address: AddressSaas) {

    private fun <T:TypeValueSaas> findType(values: Collection<T>, enumType: NamedType): T? {
        return values.find { it.type?.equals(enumType.getTypeName()) ?: false }
    }

    fun geoCoordinates(): GeoCoordinateDto? {

        return if (address.geographicCoordinates?.longitude != null
                    && address.geographicCoordinates.latitude != null)
                 GeoCoordinateDto(address.geographicCoordinates.longitude, address.geographicCoordinates.latitude, null)
            else
                null
    }

    fun countryCode(): CountryCode? {

        return this.address.country?.shortName;
    }

    fun adminAreaLevel1(): String? {
        val adminArea = findType( this.address.administrativeAreas, SaasAdministrativeAreaType.REGION)
        return adminArea?.value;
    }

    fun adminAreaLevel2(): String? {
        val adminArea = findType( this.address.administrativeAreas, SaasAdministrativeAreaType.COUNTY)
        return adminArea?.value;
    }

    fun postcode(): String? {
        val postcode = findType( this.address.postCodes, SaasPostCodeType.REGULAR)
        return postcode?.value;
    }

    fun city(): String? {
        val locality = findType( this.address.localities, SaasLocalityType.CITY)
        return locality?.value;
    }

    fun districtLevel1(): String? {
        val locality = findType( this.address.localities, SaasLocalityType.DISTRICT)
        return locality?.value;
    }

    fun districtLevel2(): String? {
        val locality = findType( this.address.localities, SaasLocalityType.QUARTER)
        return locality?.value;
    }

    fun streetName(): String? {
        val thoroughfare = findType( this.address.thoroughfares, SaasThoroughfareType.STREET)
        return thoroughfare?.name;
    }

    fun streetHouseNumber(): String? {
        val thoroughfare = findType( this.address.thoroughfares, SaasThoroughfareType.STREET)
        return thoroughfare?.number;
    }

    fun streetMilestone(): String? {
        val thoroughfare = findType( this.address.thoroughfares, SaasThoroughfareType.STREET)
        return thoroughfare?.shortName;
    }

    fun streetDirection(): String? {
        val thoroughfare = findType( this.address.thoroughfares, SaasThoroughfareType.STREET)
        return thoroughfare?.direction;
    }

    fun companyPostCode(): String? {
        val postcode = findType( this.address.postCodes, SaasPostCodeType.LARGE_MAIL_USER)
        return postcode?.value;
    }

    fun industrialZone(): String? {
        val thoroughfare = findType( this.address.thoroughfares, SaasThoroughfareType.INDUSTRIAL_ZONE)
        return thoroughfare?.name;
    }

    fun building(): String? {
        val premise = findType( this.address.premises, SaasPremiseType.BUILDING)
        return premise?.value;
    }

    fun floor(): String? {
        val premise = findType( this.address.premises, SaasPremiseType.LEVEL)
        return premise?.value;
    }

    fun door(): String? {
        val premise = findType( this.address.premises, SaasPremiseType.ROOM)
        return premise?.value;
    }

    fun deliveryServiceTypePrivateBag (): String? {
        val postalDeliveryPoint = findType( this.address.postalDeliveryPoints, SaasPostalDeliveryPointType.MAILBOX)
        return postalDeliveryPoint?.value;
    }

    fun deliveryServiceTypePoBox(): String? {
        val postalDeliveryPoint = findType( this.address.postalDeliveryPoints, SaasPostalDeliveryPointType.POST_OFFICE_BOX)
        return postalDeliveryPoint?.value;
    }
}