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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.*
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.model.Street
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps a **v6** address request into the shared loose [LogisticAddressRequest]. No validation here (the parser's job),
 * so the country enum is passed on as its raw alpha-2 string for the parser to re-validate; Pool-computed confidence
 * values are dropped (not part of an upsert).
 */
@Component
class AddressDtoRequestMapperV6 {

    /**
     * Returns the create request for the address a client sent, under the parent BPN it named.
     */
    fun toCreateRequest(request: AddressPartnerCreateRequestV6): AddressCreateUntypedParentRequest =
        AddressCreateUntypedParentRequest(request.bpnParent, toContentRequest(request.address))

    /**
     * Returns the update request for the address a client sent, addressed by its BPN.
     */
    fun toUpdateRequest(request: AddressPartnerUpdateRequestV6): AddressUpdateRequest =
        AddressUpdateRequest(addressBpn = request.bpna, siteBpn = null, content = toContentRequest(request.address))

    /**
     * Returns the content of the given address as the shared loose request model.
     */
    fun toContentRequest(address: LogisticAddressDtoV6): LogisticAddressRequest =
        LogisticAddressRequest(
            name = address.name,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            // v6 has no script variants and an upsert replaces the full list, so a v6 write drops the script
            // variants the business partner may have gained through v7 or the task path.
            scriptVariants = emptyList()
        )

    private fun toPhysicalRequest(physical: PhysicalPostalAddressDtoV6): PhysicalPostalAddressRequest =
        PhysicalPostalAddressRequest(
            geographicCoordinates = physical.geographicCoordinates?.let { toGeoRequest(it) },
            country = physical.country.alpha2,
            administrativeAreaLevel1 = physical.administrativeAreaLevel1,
            administrativeAreaLevel2 = physical.administrativeAreaLevel2,
            administrativeAreaLevel3 = physical.administrativeAreaLevel3,
            postalCode = physical.postalCode,
            city = physical.city,
            district = physical.district,
            street = physical.street?.let { toStreet(it) },
            companyPostalCode = physical.companyPostalCode,
            industrialZone = physical.industrialZone,
            building = physical.building,
            floor = physical.floor,
            door = physical.door,
            taxJurisdictionCode = physical.taxJurisdictionCode
        )

    private fun toAlternativeRequest(alternative: AlternativePostalAddressDtoV6): AlternativePostalAddressRequest =
        AlternativePostalAddressRequest(
            geographicCoordinates = alternative.geographicCoordinates?.let { toGeoRequest(it) },
            country = alternative.country.alpha2,
            administrativeAreaLevel1 = alternative.administrativeAreaLevel1,
            postalCode = alternative.postalCode,
            city = alternative.city,
            deliveryServiceType = alternative.deliveryServiceType,
            deliveryServiceQualifier = alternative.deliveryServiceQualifier,
            deliveryServiceNumber = alternative.deliveryServiceNumber
        )

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDtoV6): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun toStateRequest(state: AddressStateDtoV6): AddressStateRequest =
        AddressStateRequest(validFrom = state.validFrom?.toUtcInstant(), validTo = state.validTo?.toUtcInstant(), type = state.type)

    private fun toIdentifierRequest(identifier: AddressIdentifierDtoV6): AddressIdentifierRequest =
        AddressIdentifierRequest(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: GeoCoordinateDto): GeoCoordinateRequest =
        GeoCoordinateRequest(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toStreet(street: StreetDtoV6): Street =
        Street(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
