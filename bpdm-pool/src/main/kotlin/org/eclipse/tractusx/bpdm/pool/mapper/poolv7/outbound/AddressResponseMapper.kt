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

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateVerboseDto
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
    fun toAddress(address: LogisticAddressDb): LogisticAddressVerboseDto =
        LogisticAddressVerboseDto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants)
        )

    /**
     * Returns the script-invariant part of the given address as the API reports it.
     */
    fun toInvariantAddress(address: LogisticAddressDb): LogisticAddressInvariantVerboseDto =
        LogisticAddressInvariantVerboseDto(
            bpna = address.bpn,
            bpnLegalEntity = address.legalEntity?.bpn,
            bpnSite = address.mainSite?.bpn,
            additionalSites = address.additionalSites.map { it.bpn },
            createdAt = address.createdAt,
            updatedAt = address.updatedAt,
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
    fun toCreateResponse(address: LogisticAddressDb, index: String?): AddressPartnerCreateVerboseDto =
        AddressPartnerCreateVerboseDto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants),
            index = index
        )

    /**
     * Returns the given updated address as the API reports it.
     */
    fun toUpdateResponse(address: LogisticAddressDb): AddressPartnerUpdateVerboseDto =
        AddressPartnerUpdateVerboseDto(
            address = toInvariantAddress(address),
            scriptVariants = toScriptVariants(address.scriptVariants)
        )

    /**
     * Returns the postal address of the given address script variant as the API reports it.
     */
    fun toPostalAddressScriptVariant(scriptVariant: LogisticAddressScriptVariantDb): PostalAddressScriptVariantDto =
        PostalAddressScriptVariantDto(
            scriptVariant.name,
            toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            scriptVariant.alternativeAddress?.let { AlternativeAddressScriptVariantDto(it.city) }
        )

    private fun toScriptVariants(scriptVariants: Collection<LogisticAddressScriptVariantDb>): List<LogisticAddressScriptVariantDto> =
        scriptVariants.map { LogisticAddressScriptVariantDto(it.scriptCode.technicalKey, toPostalAddressScriptVariant(it)) }

    private fun toState(state: AddressStateDb): AddressStateVerboseDto =
        AddressStateVerboseDto(state.validFrom, state.validTo, state.type.toDto())

    private fun toIdentifier(identifier: AddressIdentifierDb): AddressIdentifierVerboseDto =
        AddressIdentifierVerboseDto(identifier.value, identifierTypeResponseMapper.toTypeKeyName(identifier.type))

    private fun toPhysicalPostalAddress(address: PhysicalPostalAddressDb): PhysicalPostalAddressVerboseDto =
        with(address) {
            PhysicalPostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = country.toDto(),
                postalCode = postCode,
                city = city,
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
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

    private fun toAlternativePostalAddress(address: AlternativePostalAddressDb): AlternativePostalAddressVerboseDto =
        with(address) {
            AlternativePostalAddressVerboseDto(
                geographicCoordinates = geographicCoordinates?.let { toGeoCoordinate(it) },
                countryVerbose = country.toDto(),
                postalCode = postCode,
                city = city,
                administrativeAreaLevel1Verbose = administrativeAreaLevel1?.let { RegionDto(it.countryCode, it.regionCode, it.regionName) },
                deliveryServiceType = deliveryServiceType,
                deliveryServiceNumber = deliveryServiceNumber,
                deliveryServiceQualifier = deliveryServiceQualifier
            )
        }

    private fun toGeoCoordinate(coordinate: GeographicCoordinateDb): GeoCoordinateDto =
        GeoCoordinateDto(coordinate.longitude, coordinate.latitude, coordinate.altitude)

    private fun toStreet(street: StreetDb): StreetDto =
        with(street) {
            StreetDto(
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

    private fun toPhysicalAddressScriptVariant(address: PhysicalAddressScriptVariantDb): PhysicalAddressScriptVariantDto =
        with(address) {
            PhysicalAddressScriptVariantDto(
                city = city,
                district = district,
                street = street?.let { toStreetScriptVariant(it) },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }

    private fun toStreetScriptVariant(street: StreetScriptVariantDb): StreetScriptVariantDto =
        with(street) {
            StreetScriptVariantDto(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
}
