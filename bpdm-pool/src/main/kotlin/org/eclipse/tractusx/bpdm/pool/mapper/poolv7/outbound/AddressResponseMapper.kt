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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.RegionV7Dto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.springframework.stereotype.Component

/**
 * Maps stored addresses to the v7 API address DTOs.
 */
@Component
class AddressResponseMapper(
    private val confidenceCriteriaResponseMapper: ConfidenceCriteriaResponseMapper,
    private val identifierTypeResponseMapper: IdentifierTypeResponseMapper,
    private val relationResponseMapper: RelationResponseMapper
) {

    /**
     * Returns the given address as the API reports it.
     */
    fun toAddress(address: LogisticAddressDb): LogisticAddressVerboseV7Dto =
        LogisticAddressVerboseV7Dto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants)
        )

    /**
     * Returns the script-invariant part of the given address as the API reports it.
     */
    fun toInvariantAddress(address: LogisticAddressDb): LogisticAddressInvariantVerboseV7Dto =
        LogisticAddressInvariantVerboseV7Dto(
            bpna = address.bpn,
            bpnLegalEntity = address.legalEntity?.bpn,
            bpnSite = address.mainSite?.bpn,
            additionalSites = address.additionalSites.map { it.bpn },
            createdAt = address.createdAt.instant,
            updatedAt = address.updatedAt.instant,
            name = address.name,
            states = address.states.map { toState(it) },
            identifiers = address.identifiers.map { toIdentifier(it) },
            relations = address.startAddressRelations.plus(address.endAddressRelations).map { relationResponseMapper.toAddressRelation(it) },
            physicalPostalAddress = toPhysicalPostalAddress(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let { toAlternativePostalAddress(it) },
            confidenceCriteria = confidenceCriteriaResponseMapper.toConfidenceCriteria(address.confidenceCriteria),
            isParticipantData = address.legalEntity?.isDataSpaceParticipant ?: address.mainSite?.legalEntity?.isDataSpaceParticipant ?: false,
            addressType = address.addressType
        )

    /**
     * Returns the given newly created address as the API reports it, tagged with the index of the request that created it.
     */
    fun toCreateResponse(address: LogisticAddressDb, index: String?): AddressPartnerCreateVerboseV7Dto =
        AddressPartnerCreateVerboseV7Dto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants),
            index = index
        )

    /**
     * Returns the given updated address as the API reports it.
     */
    fun toUpdateResponse(address: LogisticAddressDb): AddressPartnerUpdateVerboseV7Dto =
        AddressPartnerUpdateVerboseV7Dto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants)
        )

    /**
     * Returns the postal address of the given address script variant as the API reports it.
     */
    fun toPostalAddressScriptVariant(scriptVariant: LogisticAddressScriptVariantDb): PostalAddressScriptVariantV7Dto =
        PostalAddressScriptVariantV7Dto(
            scriptVariant.name,
            toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            scriptVariant.alternativeAddress?.let { AlternativeAddressScriptVariantV7Dto(it.city) }
        )

    private fun toScriptVariants(scriptVariants: Collection<LogisticAddressScriptVariantDb>): List<LogisticAddressScriptVariantV7Dto> =
        scriptVariants.map { LogisticAddressScriptVariantV7Dto(it.scriptCode.technicalKey, toPostalAddressScriptVariant(it)) }

    private fun toState(state: AddressStateDb): AddressStateVerboseV7Dto =
        AddressStateVerboseV7Dto(
            BusinessStateDates.reportValidFrom(state.validFrom),
            BusinessStateDates.reportValidTo(state.validTo),
            state.type.toDto()
        )

    private fun toIdentifier(identifier: AddressIdentifierDb): AddressIdentifierVerboseV7Dto =
        AddressIdentifierVerboseV7Dto(identifier.value, identifierTypeResponseMapper.toTypeKeyName(identifier.type))

    private fun toPhysicalPostalAddress(address: PhysicalPostalAddressDb): PhysicalPostalAddressVerboseV7Dto =
        with(address) {
            PhysicalPostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = TypeKeyNameVerboseDto(country.countryCode, country.name),
                postalCode = postCode,
                city = city,
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionV7Dto(it.country.countryCode, it.regionCode, it.regionName) },
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                district = districtLevel1,
                companyPostalCode = companyPostCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                street = street?.let { toStreet(it) },
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativePostalAddress(address: AlternativePostalAddressDb): AlternativePostalAddressVerboseV7Dto =
        with(address) {
            AlternativePostalAddressVerboseV7Dto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = TypeKeyNameVerboseDto(country.countryCode, country.name),
                postalCode = postCode,
                city = city,
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionV7Dto(it.country.countryCode, it.regionCode, it.regionName) },
                deliveryServiceType = deliveryServiceType,
                deliveryServiceNumber = deliveryServiceNumber,
                deliveryServiceQualifier = deliveryServiceQualifier
            )
        }

    private fun toGeoCoordinate(coordinate: GeographicCoordinateDb): GeoCoordinateDto =
        GeoCoordinateDto(coordinate.longitude, coordinate.latitude, coordinate.altitude)

    private fun toStreet(street: StreetDb): StreetV7Dto =
        with(street) {
            StreetV7Dto(
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

    private fun toPhysicalAddressScriptVariant(address: PhysicalAddressScriptVariantDb): PhysicalAddressScriptVariantV7Dto =
        with(address) {
            PhysicalAddressScriptVariantV7Dto(
                city = city,
                district = district,
                street = street?.let { toStreetScriptVariant(it) },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }

    private fun toStreetScriptVariant(street: StreetScriptVariantDb): StreetScriptVariantV7Dto =
        with(street) {
            StreetScriptVariantV7Dto(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
}
