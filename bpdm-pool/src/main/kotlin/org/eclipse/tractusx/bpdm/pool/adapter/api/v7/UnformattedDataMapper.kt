/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.adapter.api.v7

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.dto.input.*
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class UnformattedDataMapper {

    fun toAddressContent(dto: LogisticAddressDto): Address{
        return Address(
            name = dto.name,
            identifiers = dto.identifiers.map { Identifier(it.value, it.type, null) },
            businessStates = dto.states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
            confidenceCriteria = toUnformattedConfidence(dto.confidenceCriteria),
            physicalAddress = toUnformattedPhysicalAddress(dto.physicalPostalAddress),
            alternativeAddress = dto.alternativePostalAddress?.let { toUnformattedAlternativeAddress(it) }
        )
    }

    private fun toUnformattedConfidence(dto: ConfidenceCriteriaDto): Confidence{
        return Confidence(
            sharedByOwner = dto.sharedByOwner,
            checkedByExternalDataSource = dto.checkedByExternalDataSource,
            numberOfSharingMembers = dto.numberOfSharingMembers,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt.toInstant(
                ZoneOffset.UTC),
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt.toInstant(ZoneOffset.UTC),
            confidenceLevel = dto.confidenceLevel
        )
    }

    private fun toUnformattedPhysicalAddress(dto: PhysicalPostalAddressDto): PhysicalAddress{
        return PhysicalAddress(
            geographicCoordinates = toUnformattedGeoCoordinates(dto.geographicCoordinates),
            country = dto.country.alpha2,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postCode = dto.postalCode,
            city = dto.city,
            district = dto.district,
            street = toUnformattedStreet(dto.street),
            companyPostCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door,
            taxJurisdictionCode = dto.taxJurisdictionCode
        )
    }

    private fun toUnformattedAlternativeAddress(dto: AlternativePostalAddressDto): AlternativeAddress{
        return AlternativeAddress(
            geographicCoordinates = toUnformattedGeoCoordinates(dto.geographicCoordinates),
            country = dto.country.alpha2,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceNumber = dto.deliveryServiceNumber,
            deliveryServiceQualifier = dto.deliveryServiceQualifier
        )
    }

    private fun toUnformattedGeoCoordinates(dto: GeoCoordinateDto?): GeoData{
        return dto?.let {
            GeoData(dto.latitude, dto.longitude, dto.altitude)
        } ?: GeoData(null, null, null)
    }

    private fun toUnformattedStreet(dto: StreetDto?): Street{
        return dto?.let {
            Street(
                name = dto.name,
                houseNumber = dto.houseNumber,
                houseNumberSupplement = dto.houseNumberSupplement,
                milestone = dto.milestone,
                direction = dto.direction,
                namePrefix = dto.namePrefix,
                additionalNamePrefix = dto.additionalNamePrefix,
                nameSuffix = dto.nameSuffix,
                additionalNameSuffix = dto.additionalNameSuffix
            )
        } ?: Street(null, null, null, null, null, null, null, null, null)
    }
}