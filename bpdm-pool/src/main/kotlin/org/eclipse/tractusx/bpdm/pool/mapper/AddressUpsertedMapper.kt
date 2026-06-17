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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.model.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Pure translation from a persisted address entity to the entity-free [AddressUpserted] result. Resolves metadata
 * entities back to their keys (identifier type and script code to technical key, region to region code) and exposes
 * parents as BPNs, so nothing downstream is coupled to persistence entities. The `legalEntity` back-reference is always
 * present on the addresses these services write, hence the `!!`.
 */
@Component
class AddressUpsertedMapper {

    fun toUpserted(entity: LogisticAddressDb): AddressUpserted =
        AddressUpserted(
            bpn = entity.bpn,
            legalEntityBpn = entity.legalEntity!!.bpn,
            siteBpn = entity.site?.bpn,
            address = toAddress(entity),
            scriptVariants = entity.scriptVariants.map { toScriptVariant(it) }
        )

    private fun toAddress(entity: LogisticAddressDb): LogisticAddress =
        LogisticAddress(
            name = entity.name,
            states = entity.states.map { AddressState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            identifiers = entity.identifiers.map { AddressIdentifier(it.value, it.type.technicalKey) },
            physicalPostalAddress = toPhysical(entity.physicalPostalAddress),
            alternativePostalAddress = entity.alternativePostalAddress?.let { toAlternative(it) },
            confidenceCriteria = toConfidence(entity.confidenceCriteria)
        )

    private fun toPhysical(db: PhysicalPostalAddressDb): PhysicalPostalAddress =
        PhysicalPostalAddress(
            geographicCoordinates = db.geographicCoordinates?.let { toGeo(it) },
            country = db.country,
            administrativeAreaLevel1 = db.administrativeAreaLevel1?.regionCode,
            administrativeAreaLevel2 = db.administrativeAreaLevel2,
            administrativeAreaLevel3 = db.administrativeAreaLevel3,
            postalCode = db.postCode,
            city = db.city,
            district = db.districtLevel1,
            street = db.street?.let { toStreet(it) },
            companyPostalCode = db.companyPostCode,
            industrialZone = db.industrialZone,
            building = db.building,
            floor = db.floor,
            door = db.door,
            taxJurisdictionCode = db.taxJurisdictionCode
        )

    private fun toAlternative(db: AlternativePostalAddressDb): AlternativePostalAddress =
        AlternativePostalAddress(
            geographicCoordinates = db.geographicCoordinates?.let { toGeo(it) },
            country = db.country,
            administrativeAreaLevel1 = db.administrativeAreaLevel1?.regionCode,
            postalCode = db.postCode,
            city = db.city,
            deliveryServiceType = db.deliveryServiceType,
            deliveryServiceQualifier = db.deliveryServiceQualifier,
            deliveryServiceNumber = db.deliveryServiceNumber
        )

    private fun toConfidence(db: ConfidenceCriteriaDb): ConfidenceCriteria =
        ConfidenceCriteria(
            sharedByOwner = db.sharedByOwner,
            checkedByExternalDataSource = db.checkedByExternalDataSource,
            numberOfSharingMembers = db.numberOfSharingMembers,
            lastConfidenceCheckAt = db.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = db.nextConfidenceCheckAt.toUtcInstant(),
            confidenceLevel = db.confidenceLevel
        )

    private fun toScriptVariant(db: LogisticAddressScriptVariantDb): AddressScriptVariant =
        AddressScriptVariant(
            scriptCode = db.scriptCode.technicalKey,
            address = PostalAddressScriptVariant(
                addressName = db.name,
                physicalAddress = toPhysicalScriptVariant(db.physicalAddress),
                alternativeAddress = db.alternativeAddress?.let { toAlternativeScriptVariant(it) }
            )
        )

    private fun toPhysicalScriptVariant(db: PhysicalAddressScriptVariantDb): PhysicalAddressScriptVariant =
        PhysicalAddressScriptVariant(
            postalCode = db.postalCode,
            city = db.city,
            district = db.district,
            street = db.street?.let { toStreet(it) },
            companyPostalCode = db.companyPostalCode,
            industrialZone = db.industrialZone,
            building = db.building,
            floor = db.floor,
            door = db.door,
            taxJurisdictionCode = db.taxJurisdictionCode
        )

    private fun toAlternativeScriptVariant(db: AlternativeAddressScriptVariantDb): AlternativeAddressScriptVariant =
        AlternativeAddressScriptVariant(
            postalCode = db.postalCode,
            city = db.city,
            deliveryServiceQualifier = db.deliveryServiceQualifier,
            deliveryServiceNumber = db.deliveryServiceNumber
        )

    private fun toGeo(db: GeographicCoordinateDb) =
        GeoCoordinate(longitude = db.longitude, latitude = db.latitude, altitude = db.altitude)

    private fun toStreet(db: StreetDb) =
        Street(
            name = db.name,
            houseNumber = db.houseNumber,
            houseNumberSupplement = db.houseNumberSupplement,
            milestone = db.milestone,
            direction = db.direction,
            namePrefix = db.namePrefix,
            additionalNamePrefix = db.additionalNamePrefix,
            nameSuffix = db.nameSuffix,
            additionalNameSuffix = db.additionalNameSuffix
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
