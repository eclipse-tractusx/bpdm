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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AlternativePostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.PhysicalPostalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.StreetV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.RegionV6Dto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.stereotype.Component

/**
 * Maps stored addresses to the v6 API address DTOs.
 */
@Component
class AddressResponseMapperV6(
    private val confidenceCriteriaResponseMapperV6: ConfidenceCriteriaResponseMapperV6
) {

    /**
     * Returns the given address as the v6 API reports it.
     */
    fun toAddress(address: LogisticAddressDb): LogisticAddressVerboseV6Dto =
        LogisticAddressVerboseV6Dto(
            bpna = address.bpn,
            bpnLegalEntity = address.legalEntity?.bpn,
            bpnSite = address.mainSite?.bpn,
            createdAt = address.createdAt.instant,
            updatedAt = address.updatedAt.instant,
            name = address.name,
            states = address.states.map { toState(it) },
            identifiers = address.identifiers.map { toIdentifier(it) },
            physicalPostalAddress = toPhysicalPostalAddress(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativePostalAddress(it) },
            confidenceCriteria = confidenceCriteriaResponseMapperV6.toConfidenceCriteria(address.confidenceCriteria),
            isCatenaXMemberData = address.legalEntity?.isDataSpaceParticipant ?: address.mainSite?.legalEntity?.isDataSpaceParticipant ?: false,
            addressType = address.addressType
        )

    /**
     * Returns the given newly created address as the v6 API reports it, tagged with the index of the request that
     * created it.
     */
    fun toCreateResponse(address: LogisticAddressDb, index: String?): AddressPartnerCreateVerboseV6Dto =
        AddressPartnerCreateVerboseV6Dto(
            address = toAddress(address),
            index = index
        )

    private fun toState(state: AddressStateDb): AddressStateVerboseV6Dto =
        AddressStateVerboseV6Dto(
            BusinessStateDates.reportValidFrom(state.validFrom),
            BusinessStateDates.reportValidTo(state.validTo),
            state.type.toDto()
        )

    private fun toIdentifier(identifier: AddressIdentifierDb): AddressIdentifierVerboseV6Dto =
        AddressIdentifierVerboseV6Dto(identifier.value, TypeKeyNameVerboseDto(identifier.type.technicalKey, identifier.type.name))

    private fun toPhysicalPostalAddress(address: PhysicalPostalAddressDb): PhysicalPostalAddressVerboseV6Dto =
        with(address) {
            PhysicalPostalAddressVerboseV6Dto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = TypeKeyNameVerboseDto(country.countryCode, country.name),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { toRegion(it) },
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postCode,
                city = city,
                district = districtLevel1,
                street = street?.let { toStreet(it) },
                companyPostalCode = companyPostCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativePostalAddress(address: AlternativePostalAddressDb): AlternativePostalAddressVerboseV6Dto =
        with(address) {
            AlternativePostalAddressVerboseV6Dto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = TypeKeyNameVerboseDto(country.countryCode, country.name),
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { toRegion(it) },
                postalCode = postCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun toRegion(region: RegionDb): RegionV6Dto =
        RegionV6Dto(region.country.countryCode, region.regionCode, region.regionName)

    private fun toGeoCoordinate(coordinate: GeographicCoordinateDb): GeoCoordinateDto =
        GeoCoordinateDto(coordinate.longitude, coordinate.latitude, coordinate.altitude)

    private fun toStreet(street: StreetDb): StreetV6Dto =
        with(street) {
            StreetV6Dto(
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
}
