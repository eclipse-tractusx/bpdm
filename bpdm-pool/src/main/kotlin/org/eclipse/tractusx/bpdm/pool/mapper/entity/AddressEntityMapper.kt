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

package org.eclipse.tractusx.bpdm.pool.mapper.entity

import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.model.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

/**
 * Identifier/state sub-entities are built without their address back-reference — the caller wires it ([toEntity] for
 * creates, the mutator for updates). `numberOfSharingMembers` is caller-supplied, not derived here.
 */
@Component
class AddressEntityMapper {

    fun toEntity(bpn: String, parsed: AddressCreateParsed, numberOfSharingMembers: Int): LogisticAddressDb {
        val entity = LogisticAddressDb(
            bpn = bpn,
            legalEntity = parsed.legalEntity,
            name = parsed.address.name,
            physicalPostalAddress = toPhysical(parsed.address.physicalPostalAddress),
            alternativePostalAddress = parsed.address.alternativePostalAddress?.let { toAlternative(it) },
            confidenceCriteria = toConfidence(parsed.address.confidenceCriteria, numberOfSharingMembers),
            scriptVariants = toScriptVariants(parsed.address.scriptVariants).toMutableList()
        )
        parsed.site?.let { entity.sites.add(it) }
        entity.identifiers.addAll(toIdentifiers(parsed.address.identifiers).onEach { it.address = entity })
        entity.states.addAll(toStates(parsed.address.states).onEach { it.address = entity })
        return entity
    }

    fun toPhysical(parsed: PhysicalPostalAddressParsed): PhysicalPostalAddressDb =
        PhysicalPostalAddressDb(
            geographicCoordinates = parsed.geographicCoordinates?.let { toGeo(it) },
            country = parsed.country,
            administrativeAreaLevel1 = parsed.administrativeAreaLevel1,
            administrativeAreaLevel2 = parsed.administrativeAreaLevel2,
            administrativeAreaLevel3 = parsed.administrativeAreaLevel3,
            postCode = parsed.postalCode,
            city = parsed.city,
            districtLevel1 = parsed.district,
            street = parsed.street?.let { toStreet(it) },
            companyPostCode = parsed.companyPostalCode,
            industrialZone = parsed.industrialZone,
            building = parsed.building,
            floor = parsed.floor,
            door = parsed.door,
            taxJurisdictionCode = parsed.taxJurisdictionCode
        )

    fun toAlternative(parsed: AlternativePostalAddressParsed): AlternativePostalAddressDb =
        AlternativePostalAddressDb(
            geographicCoordinates = parsed.geographicCoordinates?.let { toGeo(it) },
            country = parsed.country,
            administrativeAreaLevel1 = parsed.administrativeAreaLevel1,
            postCode = parsed.postalCode,
            city = parsed.city,
            deliveryServiceType = parsed.deliveryServiceType,
            deliveryServiceNumber = parsed.deliveryServiceNumber,
            deliveryServiceQualifier = parsed.deliveryServiceQualifier
        )

    fun toConfidence(parsed: ConfidenceCriteriaParsed, numberOfSharingMembers: Int): ConfidenceCriteriaDb =
        ConfidenceCriteriaDb(
            sharedByOwner = parsed.sharedByOwner,
            checkedByExternalDataSource = parsed.checkedByExternalDataSource,
            numberOfSharingMembers = numberOfSharingMembers,
            lastConfidenceCheckAt = parsed.lastConfidenceCheckAt.toLocalDateTime(),
            nextConfidenceCheckAt = parsed.nextConfidenceCheckAt.toLocalDateTime()
        )

    fun toIdentifiers(parsed: List<AddressIdentifierParsed>): List<AddressIdentifierDb> =
        parsed.map { AddressIdentifierDb(value = it.value, type = it.type) }

    fun toStates(parsed: List<AddressState>): List<AddressStateDb> =
        parsed.map { AddressStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type) }

    fun toScriptVariants(parsed: List<AddressScriptVariantParsed>): List<LogisticAddressScriptVariantDb> =
        parsed.map { variant ->
            LogisticAddressScriptVariantDb(
                scriptCode = variant.scriptCode,
                name = variant.address.addressName,
                physicalAddress = toPhysicalScriptVariant(variant.address.physicalAddress),
                alternativeAddress = variant.address.alternativeAddress?.let { toAlternativeScriptVariant(it) }
            )
        }

    private fun toGeo(geo: GeoCoordinate) =
        GeographicCoordinateDb(latitude = geo.latitude, longitude = geo.longitude, altitude = geo.altitude)

    private fun toStreet(street: Street) =
        StreetDb(
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

    private fun toPhysicalScriptVariant(variant: PhysicalAddressScriptVariant) =
        PhysicalAddressScriptVariantDb(
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

    private fun toAlternativeScriptVariant(variant: AlternativeAddressScriptVariant) =
        AlternativeAddressScriptVariantDb(
            postalCode = variant.postalCode,
            city = variant.city,
            deliveryServiceQualifier = variant.deliveryServiceQualifier,
            deliveryServiceNumber = variant.deliveryServiceNumber
        )

    private fun Instant.toLocalDateTime() = atZone(ZoneOffset.UTC).toLocalDateTime()
}
