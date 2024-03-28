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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.entity.*


fun AlternativePostalAddressDb.toAlternativePostalAddressDto() =
    AlternativePostalAddressDto(
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postalCode,
        city = city
    )

fun PhysicalPostalAddressDb.toPhysicalPostalAddress(): PhysicalPostalAddressDto =
    PhysicalPostalAddressDto(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postalCode,
        city = city,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = district,
        companyPostalCode = companyPostalCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door,
        street = street?.toStreetDto()
    )

fun GeographicCoordinateDb.toGeographicCoordinateDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

private fun StreetDb.toStreetDto(): StreetDto {

    return StreetDto(
        name = name,
        houseNumber = houseNumber,
        houseNumberSupplement = houseNumberSupplement,
        milestone = milestone,
        direction = direction,
        namePrefix = namePrefix,
        additionalNamePrefix = additionalNamePrefix,
        nameSuffix = nameSuffix,
        additionalNameSuffix = additionalNameSuffix
    )
}

fun ChangelogEntryDb.toGateDto(): ChangelogGateDto {
    return ChangelogGateDto(
        externalId = externalId,
        timestamp = createdAt,
        changelogType = changelogType
    )
}


