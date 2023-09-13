/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.AlternativePostalAddressDto
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinate
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.Street
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.springframework.stereotype.Service

@Service
class BusinessPartnerMappings {

    fun toBusinessPartnerInputDto(entity: BusinessPartner): BusinessPartnerInputDto {
        return BusinessPartnerInputDto(
            externalId = entity.externalId,
            nameParts = entity.nameParts,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            identifiers = entity.identifiers.map(::toIdentifierDto),
            states = entity.states.map(::toStateDto),
            classifications = entity.classifications.map(::toClassificationDto),
            roles = entity.roles,
            postalAddress = toPostalAddressInputDto(entity.postalAddress),
            isOwner = entity.isOwner ?: false,      // TODO should be mandatory in entity
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toBusinessPartnerOutputDto(entity: BusinessPartner): BusinessPartnerOutputDto {
        return BusinessPartnerOutputDto(
            externalId = entity.externalId,
            nameParts = entity.nameParts,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            identifiers = entity.identifiers.map(::toIdentifierDto),
            states = entity.states.map(::toStateDto),
            classifications = entity.classifications.map(::toClassificationDto),
            roles = entity.roles,
            postalAddress = toPostalAddressOutputDto(entity.postalAddress),
            isOwner = entity.isOwner ?: false,      // TODO should be mandatory in entity
            bpnl = entity.bpnL ?: throw NullPointerException("bpnL is null"),
            bpns = entity.bpnS,
            bpna = entity.bpnA ?: throw NullPointerException("bpnA is null"),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toBusinessPartner(dto: BusinessPartnerInputRequest, stage: StageType): BusinessPartner {
        return BusinessPartner(
            stage = stage,
            externalId = dto.externalId,
            nameParts = dto.nameParts.toMutableList(),
            roles = dto.roles.toSortedSet(),
            identifiers = dto.identifiers.map(::toIdentifier).toSortedSet(),
            states = dto.states.map(::toState).toSortedSet(),
            classifications = dto.classifications.map(::toClassification).toSortedSet(),
            shortName = dto.shortName,
            legalForm = dto.legalForm,
            isOwner = dto.isOwner,
            bpnL = null,
            bpnS = null,
            bpnA = null,
            postalAddress = toPostalAddress(dto.postalAddress)
        )
    }

    fun updateBusinessPartner(entity: BusinessPartner, dto: BusinessPartnerInputRequest) {
        entity.nameParts.replace(dto.nameParts)
        entity.roles.replace(dto.roles)
        entity.identifiers.replace(dto.identifiers.map(::toIdentifier))
        entity.states.replace(dto.states.map(::toState))
        entity.classifications.replace(dto.classifications.map(::toClassification))
        entity.shortName = dto.shortName
        entity.legalForm = dto.legalForm
        entity.isOwner = dto.isOwner
        updatePostalAddress(entity.postalAddress, dto.postalAddress)
    }

    private fun toPostalAddressInputDto(entity: PostalAddress) =
        BusinessPartnerPostalAddressInputDto(
            addressType = entity.addressType,
            physicalPostalAddress = entity.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = entity.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
        )

    private fun toPostalAddressOutputDto(entity: PostalAddress) =
        BusinessPartnerPostalAddressOutputDto(
            addressType = entity.addressType,
            physicalPostalAddress = entity.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = entity.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
        )

    private fun toPostalAddress(dto: BusinessPartnerPostalAddressInputDto) =
        PostalAddress(
            addressType = dto.addressType ?: AddressType.AdditionalAddress,     // TODO should be optional in entity
            physicalPostalAddress = dto.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = dto.alternativePostalAddress?.let(::toAlternativePostalAddress)
        )

    private fun updatePostalAddress(entity: PostalAddress, dto: BusinessPartnerPostalAddressInputDto) {
        entity.addressType = dto.addressType ?: AddressType.AdditionalAddress
        entity.physicalPostalAddress = dto.physicalPostalAddress.let(::toPhysicalPostalAddress)
        entity.alternativePostalAddress = dto.alternativePostalAddress?.let(::toAlternativePostalAddress)
    }

    private fun toPhysicalPostalAddressDto(entity: PhysicalPostalAddress) =
        PhysicalPostalAddressGateDto(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            administrativeAreaLevel2 = entity.administrativeAreaLevel2,
            administrativeAreaLevel3 = entity.administrativeAreaLevel3,
            postalCode = entity.postalCode,
            city = entity.city,
            district = entity.district,
            street = entity.street?.let(::toStreetDto),
            companyPostalCode = entity.companyPostalCode,
            industrialZone = entity.industrialZone,
            building = entity.building,
            floor = entity.floor,
            door = entity.door
        )

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddressGateDto) =
        PhysicalPostalAddress(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeographicCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city,
            district = dto.district,
            street = dto.street?.let(::toStreet),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door
        )

    private fun toAlternativePostalAddressDto(entity: AlternativePostalAddress) =
        AlternativePostalAddressDto(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            postalCode = entity.postalCode,
            city = entity.city,
            deliveryServiceType = entity.deliveryServiceType,
            deliveryServiceQualifier = entity.deliveryServiceQualifier,
            deliveryServiceNumber = entity.deliveryServiceNumber
        )

    private fun toAlternativePostalAddress(dto: AlternativePostalAddressDto) =
        AlternativePostalAddress(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeographicCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber
        )

    private fun toStreetDto(entity: Street) =
        StreetGateDto(
            name = entity.name,
            houseNumber = entity.houseNumber,
            milestone = entity.milestone,
            direction = entity.direction,
            namePrefix = entity.namePrefix,
            additionalNamePrefix = entity.additionalNamePrefix,
            nameSuffix = entity.nameSuffix,
            additionalNameSuffix = entity.additionalNameSuffix
        )

    private fun toStreet(dto: StreetGateDto) =
        Street(
            name = dto.name,
            houseNumber = dto.houseNumber,
            milestone = dto.milestone,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )

    private fun toIdentifierDto(entity: Identifier) =
        BusinessPartnerIdentifierDto(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toIdentifier(dto: BusinessPartnerIdentifierDto) =
        Identifier(type = dto.type, value = dto.value, issuingBody = dto.issuingBody)

    private fun toStateDto(entity: State) =
        BusinessPartnerStateDto(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo, description = entity.description)

    private fun toState(dto: BusinessPartnerStateDto) =
        State(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

    private fun toClassificationDto(entity: Classification) =
        ClassificationDto(type = entity.type, code = entity.code, value = entity.value)

    private fun toClassification(dto: ClassificationDto) =
        Classification(type = dto.type, code = dto.code, value = dto.value)

    private fun toGeoCoordinateDto(entity: GeographicCoordinate) =
        GeoCoordinateDto(latitude = entity.latitude, longitude = entity.longitude, altitude = entity.altitude)

    private fun toGeographicCoordinate(dto: GeoCoordinateDto) =
        GeographicCoordinate(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)
}
