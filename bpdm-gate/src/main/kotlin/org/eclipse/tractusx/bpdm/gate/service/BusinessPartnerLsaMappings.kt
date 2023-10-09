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

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinate
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.Street
import org.eclipse.tractusx.bpdm.gate.entity.generic.*

fun LegalEntityGateInputRequest.toBusinessPartnerDtoLegalEntity(): BusinessPartner {

    return BusinessPartner(
        externalId = externalId,
        nameParts = legalNameParts.toMutableList(),
        shortName = this.legalEntity.legalShortName,
        identifiers = this.legalEntity.identifiers.map(::toIdentifierLegalEntity).toSortedSet(),
        legalForm = this.legalEntity.legalForm,
        states = this.legalEntity.states.map(::toStateLegalEntity).toSortedSet(),
        classifications = this.legalEntity.classifications.map(::toClassificationLegalDto).toSortedSet(),
        roles = roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = legalAddress.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = legalAddress.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        stage = StageType.Input,
        parentId = null,
        parentType = null
    )
}

fun LegalEntityGateOutputRequest.toBusinessPartnerOutputDtoLegalEntity(): BusinessPartner {

    return BusinessPartner(
        externalId = externalId,
        nameParts = legalNameParts.toMutableList(),
        shortName = this.legalEntity.legalShortName,
        identifiers = this.legalEntity.identifiers.map(::toIdentifierLegalEntity).toSortedSet(),
        legalForm = this.legalEntity.legalForm,
        states = this.legalEntity.states.map(::toStateLegalEntity).toSortedSet(),
        classifications = this.legalEntity.classifications.map(::toClassificationLegalDto).toSortedSet(),
        roles = roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = legalAddress.address.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = legalAddress.address.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        bpnL = bpn,
        bpnA = legalAddress.bpn,
        stage = StageType.Output,
        parentId = null,
        parentType = null
    )
}

private fun toIdentifierLegalEntity(dto: LegalEntityIdentifierDto) =
    Identifier(type = dto.type, value = dto.value, issuingBody = dto.issuingBody)

private fun toStateLegalEntity(dto: LegalEntityStateDto) =
    State(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

private fun toClassificationLegalDto(dto: ClassificationDto) =
    Classification(type = dto.type, code = dto.code, value = dto.value)

fun SiteGateInputRequest.toBusinessPartnerDtoSite(): BusinessPartner {

    return BusinessPartner(
        externalId = externalId,
        nameParts = site.nameParts.toMutableList(),
        shortName = "",
        identifiers = sortedSetOf(),
        legalForm = "",
        states = site.states.map(::toStateAddress).toSortedSet(),
        classifications = sortedSetOf(),
        roles = site.roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.SiteMainAddress,
            physicalPostalAddress = mainAddress.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = mainAddress.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        stage = StageType.Input,
        parentId = legalEntityExternalId,
        parentType = BusinessPartnerType.LEGAL_ENTITY

    )
}

fun SiteGateOutputRequest.toBusinessPartnerOutputDtoSite(): BusinessPartner {

    return BusinessPartner(
        externalId = externalId,
        nameParts = site.nameParts.toMutableList(),
        shortName = "",
        identifiers = sortedSetOf(),
        legalForm = "",
        states = site.states.map(::toStateAddress).toSortedSet(),
        classifications = sortedSetOf(),
        roles = site.roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.SiteMainAddress,
            physicalPostalAddress = mainAddress.address.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = mainAddress.address.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        stage = StageType.Output,
        bpnA = mainAddress.bpn,
        bpnL = "NO_VALUE",
        bpnS = bpn,
        parentId = legalEntityExternalId,
        parentType = BusinessPartnerType.LEGAL_ENTITY
    )
}

private fun toStateAddress(dto: SiteStateDto) =
    State(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

fun AddressGateInputRequest.toBusinessPartnerDtoSite(parentId: String?, parentType: BusinessPartnerType?): BusinessPartner {

    return BusinessPartner(
        externalId = this.externalId,
        nameParts = this.address.nameParts.toMutableList(),
        shortName = "",
        identifiers = this.address.identifiers.map(::toIdentifierAddress).toSortedSet(),
        legalForm = "",
        states = this.address.states.map(::toStateAddress).toSortedSet(),
        classifications = sortedSetOf(),
        roles = this.address.roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = this.address.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = this.address.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        stage = StageType.Input,
        parentId = parentId,
        parentType = parentType
    )
}

fun AddressGateOutputRequest.toBusinessPartnerOutputDtoSite(parentId: String?, parentType: BusinessPartnerType?): BusinessPartner {

    return BusinessPartner(
        externalId = this.externalId,
        nameParts = this.address.nameParts.toMutableList(),
        shortName = "",
        identifiers = this.address.identifiers.map(::toIdentifierAddress).toSortedSet(),
        legalForm = "",
        states = this.address.states.map(::toStateAddress).toSortedSet(),
        classifications = sortedSetOf(),
        roles = this.address.roles.toSortedSet(),
        postalAddress = PostalAddress(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = this.address.physicalPostalAddress.let(::toPhysicalPostalAddress),
            alternativePostalAddress = this.address.alternativePostalAddress?.let(::toAlternativePostalAddress)
        ),
        isOwnCompanyData = false,
        stage = StageType.Output,
        bpnS = "NO_VALUE",
        bpnL = "NO_VALUE",
        bpnA = bpn,
        parentId = parentId,
        parentType = parentType
    )
}

private fun toIdentifierAddress(dto: AddressIdentifierDto) =
    Identifier(type = dto.type, value = dto.value, issuingBody = null)

private fun toStateAddress(dto: AddressStateDto) =
    State(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

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

private fun toAlternativePostalAddress(dto: AlternativePostalAddressGateDto) =
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

private fun toGeographicCoordinate(dto: GeoCoordinateDto) =
    GeographicCoordinate(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)


//Generic to L/S/A Types (Responses / Input)
fun BusinessPartner.toLegalEntityGateInputDto(): LegalEntityGateInputDto {

    return LegalEntityGateInputDto(
        externalId = externalId,
        legalNameParts = nameParts,
        roles = roles,

        legalEntity = LegalEntityDto(
            legalShortName = shortName,
            identifiers = identifiers.map(::toIdentifierLegalEntityDto),
            legalForm = legalForm,
            states = states.map(::toStateLegalEntityDto),
            classifications = classifications.map(::toClassificationLegalDto),
        ),

        legalAddress = AddressGateInputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
                alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
            ),
            externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        ),
    )
}

fun BusinessPartner.toLegalEntityGateOutputResponse(): LegalEntityGateOutputResponse {

    return LegalEntityGateOutputResponse(
        externalId = externalId,
        legalNameParts = nameParts,
        roles = roles,
        bpnl = bpnL.toString(),
        legalEntity = LegalEntityDto(
            legalShortName = shortName,
            identifiers = identifiers.map(::toIdentifierLegalEntityDto),
            legalForm = legalForm,
            states = states.map(::toStateLegalEntityDto),
            classifications = classifications.map(::toClassificationLegalDto),
        ),
        legalAddress = AddressGateOutputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
                alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
            ),
            externalId = "",
            bpna = bpnA.toString()
        ),
    )
}

private fun toIdentifierLegalEntityDto(dto: Identifier) =
    LegalEntityIdentifierDto(type = dto.type, value = dto.value, issuingBody = dto.issuingBody)

private fun toStateLegalEntityDto(dto: State) =
    LegalEntityStateDto(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

private fun toClassificationLegalDto(dto: Classification) =
    ClassificationDto(type = dto.type, code = dto.code, value = dto.value)

fun BusinessPartner.toSiteGateInputDto(parentId: String?): SiteGateInputDto {

    return SiteGateInputDto(
        externalId = externalId,
        legalEntityExternalId = parentId.toString(),
        site = SiteGateDto(
            nameParts = nameParts,
            states = states.map(::toStateSiteDto),
            roles = roles.toList(),
        ),
        mainAddress = AddressGateInputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
                alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
            ),
            externalId = "",
        ),
    )
}

fun BusinessPartner.toSiteGateOutputResponse(parentId: String?): SiteGateOutputResponse {

    return SiteGateOutputResponse(
        externalId = externalId,
        legalEntityExternalId = parentId.toString(),
        bpns = bpnS.toString(),
        site = SiteGateDto(
            nameParts = nameParts,
            states = states.map(::toStateSiteDto),
            roles = roles.toList(),
        ),
        mainAddress = AddressGateOutputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
                alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto)
            ),
            externalId = "",
            bpna = bpnA.toString()
        ),
    )
}

private fun toStateSiteDto(dto: State) =
    SiteStateDto(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

fun BusinessPartner.toAddressGateInputDto(parentId: String?, parentType: BusinessPartnerType?): AddressGateInputDto {

    val (legalEntityId, siteId) = when (parentType) {
        BusinessPartnerType.LEGAL_ENTITY -> parentId to null
        BusinessPartnerType.SITE -> null to parentId
        else -> null to null
    }

    return AddressGateInputDto(
        externalId = externalId,
        legalEntityExternalId = legalEntityId,
        siteExternalId = siteId,
        address = LogisticAddressGateDto(
            nameParts = nameParts,
            states = states.map(::toStateAddressDto),
            identifiers = identifiers.map(::toIdentifierAddressDto),
            physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto),
            roles = roles.toList(),
        ),
    )
}

fun BusinessPartner.toAddressGateOutputDto(parentId: String?, parentType: BusinessPartnerType?): AddressGateOutputDto {

    val (legalEntityId, siteId) = when (parentType) {
        BusinessPartnerType.LEGAL_ENTITY -> parentId to null
        BusinessPartnerType.SITE -> null to parentId
        else -> null to null
    }

    return AddressGateOutputDto(
        externalId = externalId,
        legalEntityExternalId = legalEntityId,
        siteExternalId = siteId,
        bpna = bpnA.toString(),
        address = LogisticAddressGateDto(
            nameParts = nameParts,
            states = states.map(::toStateAddressDto),
            identifiers = identifiers.map(::toIdentifierAddressDto),
            physicalPostalAddress = postalAddress.physicalPostalAddress.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativePostalAddressDto),
            roles = roles.toList(),
        ),
    )
}

private fun toIdentifierAddressDto(dto: Identifier) =
    AddressIdentifierDto(type = dto.type, value = dto.value)

private fun toStateAddressDto(dto: State) =
    AddressStateDto(type = dto.type, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description)

private fun toPhysicalPostalAddressDto(entity: PhysicalPostalAddress?) =
    PhysicalPostalAddressGateDto(
        geographicCoordinates = entity?.geographicCoordinates?.let(::toGeoCoordinateDto),
        country = entity?.country,
        administrativeAreaLevel1 = entity?.administrativeAreaLevel1,
        administrativeAreaLevel2 = entity?.administrativeAreaLevel2,
        administrativeAreaLevel3 = entity?.administrativeAreaLevel3,
        postalCode = entity?.postalCode,
        city = entity?.city,
        district = entity?.district,
        street = entity?.street?.let(::toStreetDto),
        companyPostalCode = entity?.companyPostalCode,
        industrialZone = entity?.industrialZone,
        building = entity?.building,
        floor = entity?.floor,
        door = entity?.door
    )

private fun toAlternativePostalAddressDto(entity: AlternativePostalAddress) =
    AlternativePostalAddressGateDto(
        geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
        country = entity.country,
        administrativeAreaLevel1 = entity.administrativeAreaLevel1,
        postalCode = entity.postalCode,
        city = entity.city,
        deliveryServiceType = entity.deliveryServiceType,
        deliveryServiceQualifier = entity.deliveryServiceQualifier,
        deliveryServiceNumber = entity.deliveryServiceNumber
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

private fun toGeoCoordinateDto(entity: GeographicCoordinate) =
    GeoCoordinateDto(latitude = entity.latitude, longitude = entity.longitude, altitude = entity.altitude)







