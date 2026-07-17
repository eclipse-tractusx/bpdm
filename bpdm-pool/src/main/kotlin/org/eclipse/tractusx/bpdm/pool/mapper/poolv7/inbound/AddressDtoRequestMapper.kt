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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativeAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.PostalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.dto.LogisticAddressWithScriptVariantsDto
import org.eclipse.tractusx.bpdm.pool.model.*
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps a Pool API logistic address into the loose [LogisticAddressRequest]. No validation here (the parser's job), so the
 * country enum is passed on as its raw alpha-2 string for the parser to re-validate; Pool-computed confidence values are
 * dropped (not part of an upsert).
 */
@Component
class AddressDtoRequestMapper {

    fun toContentRequest(address: LogisticAddressDto, scriptVariants: List<LogisticAddressScriptVariantDto>): LogisticAddressRequest =
        LogisticAddressRequest(
            name = address.name,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            scriptVariants = scriptVariants.map { toScriptVariant(it) }
        )

    fun toContentRequest(addressWithScriptVariants: LogisticAddressWithScriptVariantsDto): LogisticAddressRequest =
        toContentRequest(addressWithScriptVariants.address, addressWithScriptVariants.scriptVariants)

    private fun toPhysicalRequest(physical: PhysicalPostalAddressDto): PhysicalPostalAddressRequest =
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

    private fun toAlternativeRequest(alternative: AlternativePostalAddressDto): AlternativePostalAddressRequest =
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

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDto): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun toStateRequest(state: AddressStateDto): AddressStateRequest =
        AddressStateRequest(validFrom = state.validFrom?.toUtcInstant(), validTo = state.validTo?.toUtcInstant(), type = state.type)

    private fun toIdentifierRequest(identifier: AddressIdentifierDto): AddressIdentifierRequest =
        AddressIdentifierRequest(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: GeoCoordinateDto): GeoCoordinateRequest =
        GeoCoordinateRequest(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toScriptVariant(variant: LogisticAddressScriptVariantDto): AddressScriptVariant =
        AddressScriptVariant(
            scriptCode = variant.scriptCode,
            address = toPostalScriptVariant(variant.address)
        )

    private fun toPostalScriptVariant(variant: PostalAddressScriptVariantDto): PostalAddressScriptVariant =
        PostalAddressScriptVariant(
            addressName = variant.addressName,
            physicalAddress = toPhysicalScriptVariant(variant.physicalAddress),
            alternativeAddress = variant.alternativeAddress?.let { toAlternativeScriptVariant(it) }
        )

    private fun toPhysicalScriptVariant(variant: PhysicalAddressScriptVariantDto): PhysicalAddressScriptVariant =
        PhysicalAddressScriptVariant(
            postalCode = variant.postalCode,
            city = variant.city,
            district = variant.district,
            street = variant.street?.let { toStreet(it) },
            companyPostalCode = variant.companyPostalCode,
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door,
            taxJurisdictionCode = variant.taxJurisdictionCode
        )

    private fun toAlternativeScriptVariant(variant: AlternativeAddressScriptVariantDto): AlternativeAddressScriptVariant =
        AlternativeAddressScriptVariant(
            postalCode = variant.postalCode,
            city = variant.city,
            deliveryServiceQualifier = variant.deliveryServiceQualifier,
            deliveryServiceNumber = variant.deliveryServiceNumber
        )

    private fun toStreet(street: StreetDto): Street =
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
