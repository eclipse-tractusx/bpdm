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

import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AlternativeAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AlternativePostalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LogisticAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PhysicalAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PostalAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.dto.LogisticAddressWithScriptVariantsDto
import org.eclipse.tractusx.bpdm.pool.model.Street
import org.eclipse.tractusx.bpdm.pool.model.StreetScriptVariant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps a Pool API logistic address into the loose [LogisticAddressRaw]. No validation here (the parser's job), so the
 * country enum is passed on as its raw alpha-2 string for the parser to re-validate; Pool-computed confidence values are
 * dropped (not part of an upsert).
 */
@Component
class AddressDtoRequestMapper {

    fun toContentRequest(address: LogisticAddressV7Dto, scriptVariants: List<LogisticAddressScriptVariantV7Dto>): LogisticAddressRaw =
        LogisticAddressRaw(
            name = address.name,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            scriptVariants = scriptVariants.map { toScriptVariant(it) }
        )

    fun toContentRequest(addressWithScriptVariants: LogisticAddressWithScriptVariantsDto): LogisticAddressRaw =
        toContentRequest(addressWithScriptVariants.address, addressWithScriptVariants.scriptVariants)

    private fun toPhysicalRequest(physical: PhysicalPostalAddressV7Dto): PhysicalPostalAddressRaw =
        PhysicalPostalAddressRaw(
            geographicCoordinates = physical.geographicCoordinates?.let { toGeoRequest(it) },
            country = physical.country,
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

    private fun toAlternativeRequest(alternative: AlternativePostalAddressV7Dto): AlternativePostalAddressRaw =
        AlternativePostalAddressRaw(
            geographicCoordinates = alternative.geographicCoordinates?.let { toGeoRequest(it) },
            country = alternative.country,
            administrativeAreaLevel1 = alternative.administrativeAreaLevel1,
            postalCode = alternative.postalCode,
            city = alternative.city,
            deliveryServiceType = alternative.deliveryServiceType,
            deliveryServiceQualifier = alternative.deliveryServiceQualifier,
            deliveryServiceNumber = alternative.deliveryServiceNumber
        )

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaV7Dto): ConfidenceCriteriaRaw =
        ConfidenceCriteriaRaw(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun toStateRequest(state: AddressStateV7Dto): AddressStateRaw =
        AddressStateRaw(validFrom = state.validFrom.toValidFromInstant(), validTo = state.validTo.toValidToInstant(), type = state.type)

    private fun toIdentifierRequest(identifier: AddressIdentifierV7Dto): AddressIdentifierRaw =
        AddressIdentifierRaw(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: GeoCoordinateDto): GeoCoordinateRaw =
        GeoCoordinateRaw(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toScriptVariant(variant: LogisticAddressScriptVariantV7Dto): AddressScriptVariantRaw =
        AddressScriptVariantRaw(
            scriptCode = variant.scriptCode,
            address = toPostalScriptVariant(variant.address)
        )

    private fun toPostalScriptVariant(variant: PostalAddressScriptVariantV7Dto): PostalAddressScriptVariantRaw =
        PostalAddressScriptVariantRaw(
            addressName = variant.addressName,
            physicalAddress = toPhysicalScriptVariant(variant.physicalAddress),
            alternativeAddress = variant.alternativeAddress?.let { toAlternativeScriptVariant(it) }
        )

    private fun toPhysicalScriptVariant(variant: PhysicalAddressScriptVariantV7Dto): PhysicalAddressScriptVariantRaw =
        PhysicalAddressScriptVariantRaw(
            city = variant.city,
            district = variant.district,
            street = variant.street?.let { toScriptVariantStreet(it) },
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door
        )

    private fun toAlternativeScriptVariant(variant: AlternativeAddressScriptVariantV7Dto): AlternativeAddressScriptVariantRaw =
        AlternativeAddressScriptVariantRaw(
            city = variant.city
        )

    private fun toScriptVariantStreet(street: StreetScriptVariantV7Dto): StreetScriptVariant =
        StreetScriptVariant(
            name = street.name,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun toStreet(street: StreetV7Dto): Street =
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

    private fun LocalDateTime?.toValidFromInstant() = BusinessStateDates.receiveValidFrom(this)?.toUtcInstant()

    private fun LocalDateTime?.toValidToInstant() = BusinessStateDates.receiveValidTo(this)?.toUtcInstant()
}
