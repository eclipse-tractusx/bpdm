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
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.LogisticAddressGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddressGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*


fun LegalEntityGateInputRequest.toBusinessPartnerDto(): BusinessPartnerInputRequest {

    return BusinessPartnerInputRequest(
        externalId = externalId,
        nameParts = legalNameParts.toList(),
        shortName = this.legalEntity.legalShortName,
        identifiers = this.legalEntity.identifiers.map(::toBusinessPartnerIdentifier),
        legalForm = this.legalEntity.legalForm,
        states = this.legalEntity.states.map(::toBusinessPartnerState),
        classifications = this.legalEntity.classifications,
        roles = roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = legalAddress.physicalPostalAddress,
            alternativePostalAddress = legalAddress.alternativePostalAddress
        ),
        isOwner = false
    )
}

fun SiteGateInputRequest.toBusinessPartnerDto(): BusinessPartnerInputRequest {

    return BusinessPartnerInputRequest(
        externalId = externalId,
        nameParts = site.nameParts.toList(),
        shortName = "",
        identifiers = emptyList(),
        legalForm = "",
        states = site.states.map(::toBusinessPartnerState),
        classifications = emptyList(),
        roles = site.roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.SiteMainAddress,
            physicalPostalAddress = mainAddress.physicalPostalAddress,
            alternativePostalAddress = mainAddress.alternativePostalAddress
        ),
        isOwner = false,
        parentId = legalEntityExternalId,
        parentType = BusinessPartnerType.LEGAL_ENTITY
    )
}

fun AddressGateInputRequest.toBusinessPartnerDto(parentId: String?, parentType: BusinessPartnerType?): BusinessPartnerInputRequest {

    return BusinessPartnerInputRequest(
        externalId = this.externalId,
        nameParts = this.address.nameParts.toList(),
        shortName = "",
        identifiers = this.address.identifiers.map(::toBusinessPartnerIdentifier),
        legalForm = "",
        states = this.address.states.map(::toBusinessPartnerState),
        classifications = emptyList(),
        roles = this.address.roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = this.address.physicalPostalAddress,
            alternativePostalAddress = this.address.alternativePostalAddress
        ),
        isOwner = false,
        parentId = parentId,
        parentType = parentType
    )
}


private fun toBusinessPartnerIdentifier(dto: LegalEntityIdentifierDto): BusinessPartnerIdentifierDto {

    return BusinessPartnerIdentifierDto(
        value = dto.value,
        type = dto.type,
        issuingBody = dto.issuingBody
    )
}

private fun toBusinessPartnerIdentifier(dto: AddressIdentifierDto): BusinessPartnerIdentifierDto {

    return BusinessPartnerIdentifierDto(
        value = dto.value,
        type = dto.type,
        issuingBody = null
    )
}

fun toBusinessPartnerState(dto: LegalEntityStateDto): BusinessPartnerStateDto {

    return BusinessPartnerStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

fun toBusinessPartnerState(dto: SiteStateDto): BusinessPartnerStateDto {

    return BusinessPartnerStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

fun toBusinessPartnerState(dto: AddressStateDto): BusinessPartnerStateDto {

    return BusinessPartnerStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

//Generic to L/S/A Types (Input)
fun BusinessPartnerInputDto.toLegalEntityGateInputDto(): LegalEntityGateInputDto {

    return LegalEntityGateInputDto(
        externalId = externalId,
        legalNameParts = nameParts,
        roles = roles,

        legalEntity = LegalEntityDto(
            legalShortName = shortName,
            identifiers = identifiers.map(::toLegalEntityIdentifierDto),
            legalForm = legalForm,
            states = states.map(::toLegalEntityStateDto),
            classifications = classifications,
        ),

        legalAddress = AddressGateInputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
                alternativePostalAddress = postalAddress.alternativePostalAddress
            ),
            externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        ),
    )
}

fun BusinessPartnerInputDto.toSiteGateInputDto(): SiteGateInputDto {

    return SiteGateInputDto(
        externalId = externalId,
        legalEntityExternalId = "",
        site = SiteGateDto(
            nameParts = nameParts,
            states = states.map(::toSiteStateDto),
            roles = roles.toList(),
        ),
        mainAddress = AddressGateInputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
                alternativePostalAddress = postalAddress.alternativePostalAddress
            ),
            externalId = "",
        ),
    )
}

fun BusinessPartnerInputDto.toAddressGateInputDto(): AddressGateInputDto {

    return AddressGateInputDto(
        externalId = externalId,
        address = LogisticAddressGateDto(
            nameParts = nameParts,
            states = states.map(::toAddressStateDto),
            identifiers = identifiers.map(::toAddressIdentifierDto),
            physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
            alternativePostalAddress = postalAddress.alternativePostalAddress,
            roles = roles.toList(),
        ),
    )
}

fun toPhysicalPostalAddressGateDto(dto: PhysicalPostalAddressGateDto?): PhysicalPostalAddressGateDto {

    return PhysicalPostalAddressGateDto(
        geographicCoordinates = dto?.geographicCoordinates,
        country = dto?.country,
        administrativeAreaLevel1 = dto?.administrativeAreaLevel1,
        administrativeAreaLevel2 = dto?.administrativeAreaLevel2,
        administrativeAreaLevel3 = dto?.administrativeAreaLevel3,
        postalCode = dto?.postalCode,
        city = dto?.city.toString(),
        district = dto?.district,
        street = dto?.street,
        companyPostalCode = dto?.companyPostalCode,
        industrialZone = dto?.industrialZone,
        building = dto?.building,
        floor = dto?.floor,
        door = dto?.door
    )
}

fun toLegalEntityStateDto(dto: BusinessPartnerStateDto): LegalEntityStateDto {

    return LegalEntityStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

fun toSiteStateDto(dto: BusinessPartnerStateDto): SiteStateDto {

    return SiteStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

fun toAddressStateDto(dto: BusinessPartnerStateDto): AddressStateDto {

    return AddressStateDto(
        description = dto.description,
        validFrom = dto.validFrom,
        validTo = dto.validTo,
        type = dto.type
    )
}

fun toLegalEntityIdentifierDto(dto: BusinessPartnerIdentifierDto): LegalEntityIdentifierDto {

    return LegalEntityIdentifierDto(
        value = dto.value,
        type = dto.type,
        issuingBody = dto.issuingBody
    )
}

private fun toAddressIdentifierDto(dto: BusinessPartnerIdentifierDto): AddressIdentifierDto {

    return AddressIdentifierDto(
        value = dto.value,
        type = dto.type,
    )
}

//Generic to L/S/A Types (Output)
fun BusinessPartnerOutputDto.toLegalEntityGateOutputResponse(): LegalEntityGateOutputResponse {

    return LegalEntityGateOutputResponse(
        externalId = externalId,
        legalNameParts = nameParts,
        roles = roles,
        bpnl = bpnL,
        legalEntity = LegalEntityDto(
            legalShortName = shortName,
            identifiers = identifiers.map(::toLegalEntityIdentifierDto),
            legalForm = legalForm,
            states = states.map(::toLegalEntityStateDto),
            classifications = classifications,
        ),
        legalAddress = AddressGateOutputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
                alternativePostalAddress = postalAddress.alternativePostalAddress
            ),
            externalId = "",
            bpna = bpnA
        ),
    )
}

fun BusinessPartnerOutputDto.toSiteGateOutputResponse(): SiteGateOutputResponse {

    return SiteGateOutputResponse(
        externalId = externalId,
        legalEntityExternalId = "",
        bpns = bpnS.toString(),
        site = SiteGateDto(
            nameParts = nameParts,
            states = states.map(::toSiteStateDto),
            roles = roles.toList(),
        ),
        mainAddress = AddressGateOutputDto(
            address = LogisticAddressGateDto(
                physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
                alternativePostalAddress = postalAddress.alternativePostalAddress
            ),
            externalId = "",
            bpna = bpnA
        ),
    )
}

fun BusinessPartnerOutputDto.toAddressGateOutputDto(): AddressGateOutputDto {

    return AddressGateOutputDto(
        externalId = externalId,
        bpna = bpnA,
        address = LogisticAddressGateDto(
            nameParts = nameParts,
            states = states.map(::toAddressStateDto),
            identifiers = identifiers.map(::toAddressIdentifierDto),
            physicalPostalAddress = toPhysicalPostalAddressGateDto(postalAddress.physicalPostalAddress),
            alternativePostalAddress = postalAddress.alternativePostalAddress,
            roles = roles.toList(),
        ),
    )
}

//Mapping of Output Upserts

fun LegalEntityGateOutputRequest.toBusinessPartnerOutputDto(): BusinessPartnerOutputRequest {

    return BusinessPartnerOutputRequest(
        externalId = externalId,
        nameParts = legalNameParts.toList(),
        shortName = this.legalEntity.legalShortName,
        identifiers = this.legalEntity.identifiers.map(::toBusinessPartnerIdentifier),
        legalForm = this.legalEntity.legalForm,
        states = this.legalEntity.states.map(::toBusinessPartnerState),
        classifications = this.legalEntity.classifications,
        roles = roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = legalAddress.address.physicalPostalAddress,
            alternativePostalAddress = legalAddress.address.alternativePostalAddress
        ),
        isOwner = false,
        bpnL = bpn,
        bpnA = legalAddress.bpn
    )
}

fun SiteGateOutputRequest.toBusinessPartnerOutputDto(): BusinessPartnerOutputRequest {

    return BusinessPartnerOutputRequest(
        externalId = externalId,
        nameParts = site.nameParts.toList(),
        shortName = "",
        identifiers = emptyList(),
        legalForm = "",
        states = site.states.map(::toBusinessPartnerState),
        classifications = emptyList(),
        roles = site.roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.SiteMainAddress,
            physicalPostalAddress = mainAddress.address.physicalPostalAddress,
            alternativePostalAddress = mainAddress.address.alternativePostalAddress
        ),
        isOwner = false,
        parentId = legalEntityExternalId,
        parentType = BusinessPartnerType.LEGAL_ENTITY,
        bpnS = bpn
    )
}

fun AddressGateOutputRequest.toBusinessPartnerOutputDto(parentId: String?, parentType: BusinessPartnerType?): BusinessPartnerOutputRequest {

    return BusinessPartnerOutputRequest(
        externalId = this.externalId,
        nameParts = this.address.nameParts.toList(),
        shortName = "",
        identifiers = this.address.identifiers.map(::toBusinessPartnerIdentifier),
        legalForm = "",
        states = this.address.states.map(::toBusinessPartnerState),
        classifications = emptyList(),
        roles = this.address.roles,
        postalAddress = BusinessPartnerPostalAddressDto(
            addressType = AddressType.AdditionalAddress,
            physicalPostalAddress = this.address.physicalPostalAddress,
            alternativePostalAddress = this.address.alternativePostalAddress
        ),
        isOwner = false,
        parentId = parentId,
        parentType = parentType,
        bpnA = bpn
    )
}



