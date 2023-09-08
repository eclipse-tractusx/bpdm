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
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page

fun AddressGateInputRequest.toAddressGate(legalEntity: LegalEntity?, site: Site?, datatype: StageType): LogisticAddress {

    val logisticAddress = LogisticAddress(
        externalId = externalId,
        physicalPostalAddress = address.physicalPostalAddress.toPhysicalPostalAddressEntity(),
        alternativePostalAddress = address.alternativePostalAddress?.toAlternativePostalAddressEntity(),
        legalEntity = legalEntity,
        site = site,
        stage = datatype
    )

    logisticAddress.states.addAll(this.address.states.map { toEntityAddress(it, logisticAddress) }.toSet())
    logisticAddress.nameParts.addAll(this.address.nameParts.map { toNameParts(it, logisticAddress, null, null) }.toSet())
    logisticAddress.roles.addAll(this.address.roles.distinct().map { toRoles(it, null, null, logisticAddress) }.toSet())
    logisticAddress.identifiers.addAll(this.address.identifiers.distinct().map { toEntityAddressIdentifiers(it, logisticAddress) }.toSet())
    return logisticAddress
}

fun AddressGateOutputRequest.toAddressGateOutput(legalEntity: LegalEntity?, site: Site?, datatype: StageType): LogisticAddress {

    val logisticAddress = LogisticAddress(
        bpn = bpn,
        externalId = externalId,
        physicalPostalAddress = address.physicalPostalAddress.toPhysicalPostalAddressEntity(),
        alternativePostalAddress = address.alternativePostalAddress?.toAlternativePostalAddressEntity(),
        legalEntity = legalEntity,
        site = site,
        stage = datatype
    )

    logisticAddress.states.addAll(this.address.states.map { toEntityAddress(it, logisticAddress) }.toSet())
    logisticAddress.nameParts.addAll(this.address.nameParts.map { toNameParts(it, logisticAddress, null, null) }.toSet())
    logisticAddress.roles.addAll(this.address.roles.distinct().map { toRoles(it, null, null, logisticAddress) }.toSet())
    logisticAddress.identifiers.addAll(this.address.identifiers.distinct().map { toEntityAddressIdentifiers(it, logisticAddress) }.toSet())
    return logisticAddress
}

fun toEntityAddress(dto: AddressStateDto, address: LogisticAddress): AddressState {
    return AddressState(dto.description, dto.validFrom, dto.validTo, dto.type, address)
}

fun toNameParts(namePartsValue: String, address: LogisticAddress?, site: Site?, legalEntity: LegalEntity?): NameParts {
    return NameParts(address, site, legalEntity, namePartsValue)
}

fun AlternativePostalAddressDto.toAlternativePostalAddressEntity() =
    AlternativePostalAddress(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateEntity(),
        country = country,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        postCode = postalCode,
        city = city,
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier
    )

fun PhysicalPostalAddressGateDto.toPhysicalPostalAddressEntity() =
    PhysicalPostalAddress(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateEntity(),
        country = country,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        postCode = postalCode,
        city = city,
        districtLevel1 = district,
        street = street?.toStreetEntity(),
        companyPostCode = companyPostalCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door
    )

fun GeoCoordinateDto.toGeographicCoordinateEntity(): GeographicCoordinate {
    return GeographicCoordinate(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude
    )
}

private fun StreetGateDto.toStreetEntity(): Street {
    return Street(
        name = name,
        houseNumber = houseNumber,
        milestone = milestone,
        direction = direction,
        namePrefix = namePrefix,
        additionalNamePrefix = additionalNamePrefix,
        nameSuffix = nameSuffix,
        additionalNameSuffix = additionalNameSuffix
    )
}

fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

// Site Mappers
fun SiteGateInputRequest.toSiteGate(legalEntity: LegalEntity, datatype: StageType): Site {

    val addressInputRequest = AddressGateInputRequest(
        address = mainAddress,
        externalId = getMainAddressExternalIdForSiteExternalId(externalId),
        siteExternalId = externalId
    )

    val site = Site(
        externalId = externalId,
        legalEntity = legalEntity,
        stage = datatype
    )

    site.states.addAll(this.site.states.map { toEntityAddress(it, site) }.toSet())
    site.nameParts.addAll(this.site.nameParts.map { toNameParts(it, null, site, null) }.toSet())
    site.roles.addAll(this.site.roles.distinct().map { toRoles(it, null, site, null) })

    site.mainAddress = addressInputRequest.toAddressGate(null, site, datatype)

    return site
}

fun SiteGateOutputRequest.toSiteGate(legalEntity: LegalEntity, datatype: StageType): Site {

    val addressOutputRequest = AddressGateOutputRequest(
        address = mainAddress.address,
        externalId = getMainAddressExternalIdForSiteExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = mainAddress.bpn
    )

    val site = Site(
        bpn = bpn,
        externalId = externalId,
        legalEntity = legalEntity,
        stage = datatype
    )

    site.states.addAll(this.site.states.map { toEntityAddress(it, site) }.toSet())
    site.nameParts.addAll(this.site.nameParts.map { toNameParts(it, null, site, null) }.toSet())
    site.roles.addAll(this.site.roles.distinct().map { toRoles(it, null, site, null) })

    site.mainAddress = addressOutputRequest.toAddressGateOutput(null, site, datatype)

    return site
}

fun toEntityAddress(dto: SiteStateDto, site: Site): SiteState {
    return SiteState(dto.description, dto.validFrom, dto.validTo, dto.type, site)
}

fun ChangelogEntry.toGateDto(): ChangelogGateDto {
    return ChangelogGateDto(
        externalId = externalId,
        businessPartnerType = businessPartnerType,
        timestamp = createdAt,
        changelogType = changelogType
    )
}

fun LegalEntityGateInputRequest.toLegalEntity(datatype: StageType): LegalEntity {

    val addressInputRequest = AddressGateInputRequest(
        address = legalAddress,
        externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId
    )

    val legalEntity = LegalEntity(
        externalId = externalId,
        legalForm = legalEntity.legalForm,
        shortName = legalEntity.legalShortName,
        stage = datatype
    )

    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })
    legalEntity.nameParts.addAll(this.legalNameParts.map { toNameParts(it, null, null, legalEntity) })
    legalEntity.roles.addAll(this.roles.distinct().map { toRoles(it, legalEntity, null, null) })
    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifiers(it, legalEntity) })
    legalEntity.legalAddress = addressInputRequest.toAddressGate(legalEntity, null, datatype)

    return legalEntity

}

fun LegalEntityGateOutputRequest.toLegalEntity(datatype: StageType): LegalEntity {

    val addressOutputRequest = AddressGateOutputRequest(
        address = legalAddress.address,
        externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = legalAddress.bpn
    )

    val legalEntity = LegalEntity(
        bpn = bpn,
        externalId = externalId,
        legalForm = legalEntity.legalForm,
        shortName = legalEntity.legalShortName,
        stage = datatype
    )

    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })
    legalEntity.nameParts.addAll(this.legalNameParts.map { toNameParts(it, null, null, legalEntity) })
    legalEntity.roles.addAll(this.roles.distinct().map { toRoles(it, legalEntity, null, null) })
    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifiers(it, legalEntity) })
    legalEntity.legalAddress = addressOutputRequest.toAddressGateOutput(legalEntity, null, datatype)

    return legalEntity

}

fun toRoles(role: BusinessPartnerRole, legalEntity: LegalEntity?, site: Site?, address: LogisticAddress?): Roles {
    return Roles(legalEntity, address, site, role)
}

fun toEntityAddressIdentifiers(dto: AddressIdentifierDto, address: LogisticAddress): AddressIdentifier {
    return AddressIdentifier(dto.value, dto.type, address)
}

fun toEntityState(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
    return LegalEntityState(dto.description, dto.validFrom, dto.validTo, dto.type, legalEntity)
}

fun toEntityClassification(dto: ClassificationDto, legalEntity: LegalEntity): Classification {
    return Classification(dto.value, dto.code, dto.type, legalEntity)
}

fun toEntityIdentifiers(dto: LegalEntityIdentifierDto, legalEntity: LegalEntity): LegalEntityIdentifier {
    return LegalEntityIdentifier(dto.value, dto.type, dto.issuingBody, legalEntity)
}

fun getMainAddressExternalIdForSiteExternalId(siteExternalId: String): String {
    return siteExternalId + "_site"
}

fun getLegalAddressExternalIdForLegalEntityExternalId(legalEntityExternalId: String): String {
    return legalEntityExternalId + "_legalAddress"
}

//Logistic Address mapping to AddressGateInputResponse
fun LogisticAddress.toAddressGateInputResponse(logisticAddressPage: LogisticAddress): AddressGateInputDto {

    val addressGateInputResponse = AddressGateInputDto(
        address = logisticAddressPage.toLogisticAddressDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity?.externalId,
        siteExternalId = site?.externalId,
    )

    return addressGateInputResponse
}

//Logistic Address mapping to LogisticAddressDto
fun LogisticAddress.toLogisticAddressDto(): LogisticAddressGateDto {

    val logisticAddress = LogisticAddressGateDto(
        nameParts = getNamePartValues(nameParts),
        states = mapToDtoStates(states),
        roles = roles.map { it.roleName },
        physicalPostalAddress = physicalPostalAddress.toPhysicalPostalAddress(),
        alternativePostalAddress = alternativePostalAddress?.toAlternativePostalAddressDto(),
        identifiers = mapToAddressIdentifiersDto(identifiers)
    )

    return logisticAddress
}

fun getNamePartValues(nameparts: MutableSet<NameParts>): Collection<String> {
    return nameparts.map { it.namePart }
}

fun mapToDtoStates(states: MutableSet<AddressState>): Collection<AddressStateDto> {
    return states.map { AddressStateDto(it.description, it.validFrom, it.validTo, it.type) }
}

fun AlternativePostalAddress.toAlternativePostalAddressDto(): AlternativePostalAddressDto =
    AlternativePostalAddressDto(
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postCode,
        city = city
    )

fun PhysicalPostalAddress.toPhysicalPostalAddress(): PhysicalPostalAddressGateDto =
    PhysicalPostalAddressGateDto(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postCode,
        city = city,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = districtLevel1,
        companyPostalCode = companyPostCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door,
        street = street?.toStreetDto()
    )

fun GeographicCoordinate.toGeographicCoordinateDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

private fun Street.toStreetDto(): StreetGateDto {

    return StreetGateDto(
        name = name,
        houseNumber = houseNumber,
        milestone = milestone,
        direction = direction,
        namePrefix = namePrefix,
        additionalNamePrefix = additionalNamePrefix,
        nameSuffix = nameSuffix,
        additionalNameSuffix = additionalNameSuffix
    )
}

fun LegalEntity.toLegalEntityDto(): LegalEntityDto {

    return LegalEntityDto(
        legalForm = legalForm,
        legalShortName = shortName,
        states = mapToLegalEntityStateDto(states),
        classifications = mapToLegalEntityClassificationsDto(classifications),
        identifiers = mapToLegalEntityIdentifiersDto(identifiers)
    )

}

fun mapToLegalEntityStateDto(states: MutableSet<LegalEntityState>): Collection<LegalEntityStateDto> {
    return states.map { LegalEntityStateDto(it.description, it.validFrom, it.validTo, it.type) }
}

fun mapToLegalEntityClassificationsDto(classification: MutableSet<Classification>): Collection<ClassificationDto> {
    return classification.map { ClassificationDto(it.value, it.code, it.type) }
}

fun mapToLegalEntityIdentifiersDto(identifiers: MutableSet<LegalEntityIdentifier>): Collection<LegalEntityIdentifierDto> {
    return identifiers.map { LegalEntityIdentifierDto(it.value, it.type, it.issuingBody) }
}

fun mapToAddressIdentifiersDto(identifiers: MutableSet<AddressIdentifier>): Collection<AddressIdentifierDto> {
    return identifiers.map { AddressIdentifierDto(it.value, it.type) }
}

fun AddressIdentifier.mapToAddressIdentifiersDto(): AddressIdentifierDto {
    return AddressIdentifierDto(value, type)
}

//LegalEntity mapping to LegalEntityGateInputResponse
fun LegalEntity.toLegalEntityGateInputResponse(legalEntity: LegalEntity): LegalEntityGateInputDto {

    return LegalEntityGateInputDto(
        legalEntity = legalEntity.toLegalEntityDto(),
        legalAddress = legalAddress.toAddressGateInputResponse(legalAddress),
        roles = roles.map { it.roleName },
        externalId = legalEntity.externalId,
        legalNameParts = getNamePartValues(nameParts)
    )
}

//Site mapping to SiteDto
fun Site.toSiteDto(): SiteGateDto {

    return SiteGateDto(
        roles = roles.map { it.roleName },
        nameParts = getNamePartValues(nameParts),
        states = mapToDtoSitesStates(states)
    )
}

fun mapToDtoSitesStates(states: MutableSet<SiteState>): Collection<SiteStateDto> {
    return states.map { SiteStateDto(it.description, it.validFrom, it.validTo, it.type) }
}

//Site mapping to SiteGateInputResponse
fun Site.toSiteGateInputResponse(sitePage: Site): SiteGateInputDto {

    return SiteGateInputDto(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        mainAddress = mainAddress.toAddressGateInputResponse(mainAddress)
    )

}

//Logistic Address mapping to AddressGateOutputResponse
fun LogisticAddress.toAddressGateOutputResponse(logisticAddressPage: LogisticAddress): AddressGateOutputDto {

    val addressGateOutputResponse = AddressGateOutputDto(
        address = logisticAddressPage.toLogisticAddressDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity?.externalId,
        siteExternalId = site?.externalId,
        bpna = bpn!!,
    )

    return addressGateOutputResponse
}

//Site mapping to SiteGateOutputResponse
fun Site.toSiteGateOutputResponse(sitePage: Site): SiteGateOutputResponse {

    return SiteGateOutputResponse(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        bpns = bpn!!,
        mainAddress = mainAddress.toAddressGateOutputResponse(mainAddress)
    )
}

//LegalEntity mapping to LegalEntityGateOutputResponse
fun LegalEntity.toLegalEntityGateOutputResponse(legalEntity: LegalEntity): LegalEntityGateOutputResponse {

    return LegalEntityGateOutputResponse(
        legalEntity = legalEntity.toLegalEntityDto(),
        legalNameParts = getNamePartValues(legalEntity.nameParts),
        externalId = legalEntity.externalId,
        bpnl = legalEntity.bpn!!,
        roles = roles.map { it.roleName },
        legalAddress = legalAddress.toAddressGateOutputResponse(legalAddress)
    )
}

fun AddressGateInputRequest.toSharingStateDTO(): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.ADDRESS, externalId)
}

fun SiteGateInputRequest.toSharingStateDTO(): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.SITE, externalId)
}

fun LegalEntityGateInputRequest.toSharingStateDTO(): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.LEGAL_ENTITY, externalId)
}

fun AddressGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.ADDRESS, externalId, sharingStateType = sharingStateType, bpn = bpn)
}

fun SiteGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.SITE, externalId, sharingStateType = sharingStateType, bpn = bpn)
}

fun LegalEntityGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateDto {

    return SharingStateDto(BusinessPartnerType.LEGAL_ENTITY, externalId, sharingStateType = sharingStateType, bpn = bpn)
}


