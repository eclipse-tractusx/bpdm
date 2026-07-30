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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.AlternativeAddressScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.PhysicalAddressScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.PostalAddressScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.Street
import org.eclipse.tractusx.bpdm.pool.model.StreetScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress as TaskAlternativeAddress
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddressScriptVariant as TaskAlternativeAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.BusinessState as TaskBusinessState
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria as TaskConfidenceCriteria
import org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate as TaskGeoCoordinate
import org.eclipse.tractusx.orchestrator.api.model.Identifier as TaskIdentifier
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress as TaskPhysicalAddress
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddressScriptVariant as TaskPhysicalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddress as TaskPostalAddress
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariant as TaskPostalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode as TaskScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants as TaskAddress
import org.eclipse.tractusx.orchestrator.api.model.Street as TaskStreet
import org.eclipse.tractusx.orchestrator.api.model.StreetScriptVariant as TaskStreetScriptVariant

/**
 * Maps a cleaning task's address into the loose [LogisticAddressRequest]. Pool-computed confidence values
 * (`numberOfSharingMembers`, `confidenceLevel`) are dropped — not part of an upsert. Orchestrator types are aliased
 * `Task*` to disambiguate them from the identically named request types.
 */
@Component
class GoldenRecordTaskAddressRequestMapper {

    fun toContentRequest(address: TaskAddress): LogisticAddressRequest =
        toContentRequest(address.postalProperties, address.scriptVariants)

    /** Legal / site main address: postal properties and script variants travel separately, not bundled in a [TaskAddress]. */
    fun toContentRequest(address: TaskPostalAddress, scriptVariants: List<TaskScriptVariant>): LogisticAddressRequest =
        LogisticAddressRequest(
            name = address.addressName,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalAddress),
            alternativePostalAddress = address.alternativeAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            scriptVariants = scriptVariants.map { toScriptVariant(it) }
        )

    fun toConfidenceRequest(confidence: TaskConfidenceCriteria): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt
        )

    private fun toPhysicalRequest(physical: TaskPhysicalAddress): PhysicalPostalAddressRequest =
        PhysicalPostalAddressRequest(
            geographicCoordinates = toGeoRequest(physical.geographicCoordinates),
            country = physical.country,
            administrativeAreaLevel1 = physical.administrativeAreaLevel1,
            administrativeAreaLevel2 = physical.administrativeAreaLevel2,
            administrativeAreaLevel3 = physical.administrativeAreaLevel3,
            postalCode = physical.postalCode,
            city = physical.city,
            district = physical.district,
            street = toStreet(physical.street),
            companyPostalCode = physical.companyPostalCode,
            industrialZone = physical.industrialZone,
            building = physical.building,
            floor = physical.floor,
            door = physical.door,
            taxJurisdictionCode = physical.taxJurisdictionCode
        )

    private fun toAlternativeRequest(alternative: TaskAlternativeAddress): AlternativePostalAddressRequest =
        AlternativePostalAddressRequest(
            geographicCoordinates = toGeoRequest(alternative.geographicCoordinates),
            country = alternative.country,
            administrativeAreaLevel1 = alternative.administrativeAreaLevel1,
            postalCode = alternative.postalCode,
            city = alternative.city,
            deliveryServiceType = alternative.deliveryServiceType,
            deliveryServiceQualifier = alternative.deliveryServiceQualifier,
            deliveryServiceNumber = alternative.deliveryServiceNumber
        )

    private fun toStateRequest(state: TaskBusinessState): AddressStateRequest =
        AddressStateRequest(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toIdentifierRequest(identifier: TaskIdentifier): AddressIdentifierRequest =
        AddressIdentifierRequest(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: TaskGeoCoordinate): GeoCoordinateRequest =
        GeoCoordinateRequest(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toScriptVariant(variant: TaskScriptVariant): AddressScriptVariant =
        AddressScriptVariant(
            scriptCode = variant.scriptCode,
            address = toPostalScriptVariant(variant.postalProperties)
        )

    private fun toPostalScriptVariant(variant: TaskPostalAddressScriptVariant): PostalAddressScriptVariant =
        PostalAddressScriptVariant(
            addressName = variant.addressName,
            physicalAddress = toPhysicalScriptVariant(variant.physicalAddress),
            alternativeAddress = variant.alternativeAddress?.let { toAlternativeScriptVariant(it) }
        )

    private fun toPhysicalScriptVariant(variant: TaskPhysicalAddressScriptVariant): PhysicalAddressScriptVariant =
        PhysicalAddressScriptVariant(
            city = variant.city,
            district = variant.district,
            street = toScriptVariantStreet(variant.street),
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door
        )

    private fun toAlternativeScriptVariant(variant: TaskAlternativeAddressScriptVariant): AlternativeAddressScriptVariant =
        AlternativeAddressScriptVariant(
            city = variant.city
        )

    private fun toScriptVariantStreet(street: TaskStreetScriptVariant): StreetScriptVariant =
        StreetScriptVariant(
            name = street.name,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun toStreet(street: TaskStreet): Street =
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
}
