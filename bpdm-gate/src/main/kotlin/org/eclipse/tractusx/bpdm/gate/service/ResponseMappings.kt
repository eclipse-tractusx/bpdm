/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinate
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page

fun AddressGateInputRequest.toAddressGate(legalEntity: LegalEntityDb?, site: SiteDb?, datatype: StageType): LogisticAddressDb {

    val logisticAddress = LogisticAddressDb(
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

fun AddressGateOutputRequest.toAddressGateOutput(legalEntity: LegalEntityDb?, site: SiteDb?, datatype: StageType): LogisticAddressDb {

    val logisticAddress = LogisticAddressDb(
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

fun toEntityAddress(dto: AddressState, address: LogisticAddressDb): AddressStateDb {
    return AddressStateDb(dto.description, dto.validFrom, dto.validTo, dto.type, address)
}

fun toNameParts(namePartsValue: String, address: LogisticAddressDb?, site: SiteDb?, legalEntity: LegalEntityDb?): NamePartsDb {
    return NamePartsDb(address, site, legalEntity, namePartsValue)
}

fun AlternativePostalAddress.toAlternativePostalAddressEntity() =
    AlternativePostalAddressDb(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateEntity(),
        country = country,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        postalCode = postalCode,
        city = city,
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier
    )

fun PhysicalPostalAddress.toPhysicalPostalAddressEntity() =
    PhysicalPostalAddressDb(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateEntity(),
        country = country,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        postalCode = postalCode,
        city = city,
        district = district,
        street = street?.toStreetEntity(),
        companyPostalCode = companyPostalCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door
    )

fun GeoCoordinate.toGeographicCoordinateEntity(): GeographicCoordinateDb {
    return GeographicCoordinateDb(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude
    )
}

private fun Street.toStreetEntity(): StreetDb {
    return StreetDb(
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

fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageDto<T> {
    return PageDto(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

// Site Mappers
fun SiteGateInputRequest.toSiteGate(legalEntity: LegalEntityDb, datatype: StageType): SiteDb {

    val addressInputRequest = AddressGateInputRequest(
        address = mainAddress,
        externalId = getMainAddressExternalIdForSiteExternalId(externalId),
        siteExternalId = externalId
    )

    val site = SiteDb(
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

fun SiteGateOutputRequest.toSiteGate(legalEntity: LegalEntityDb, datatype: StageType): SiteDb {

    val addressOutputRequest = AddressGateOutputRequest(
        address = mainAddress.address,
        externalId = getMainAddressExternalIdForSiteExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = mainAddress.bpn
    )

    val site = SiteDb(
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

fun toEntityAddress(dto: SiteState, site: SiteDb): SiteStateDb {
    return SiteStateDb(dto.description, dto.validFrom, dto.validTo, dto.type, site)
}

fun ChangelogEntryDb.toGateDto(): ChangelogGateResponse {
    return ChangelogGateResponse(
        externalId = externalId,
        businessPartnerType = businessPartnerType,
        timestamp = createdAt,
        changelogType = changelogType
    )
}

fun LegalEntityGateInputRequest.toLegalEntity(datatype: StageType): LegalEntityDb {

    val addressInputRequest = AddressGateInputRequest(
        address = legalAddress,
        externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId
    )

    val legalEntity = LegalEntityDb(
        externalId = externalId,
        legalForm = legalEntity.legalForm,
        shortName = legalEntity.legalShortName,
        stage = datatype
    )

    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })
    legalEntity.nameParts.addAll(this.legalEntity.legalNameParts.map { toNameParts(it, null, null, legalEntity) })
    legalEntity.roles.addAll(this.legalEntity.roles.distinct().map { toRoles(it, legalEntity, null, null) })
    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifiers(it, legalEntity) })
    legalEntity.legalAddress = addressInputRequest.toAddressGate(legalEntity, null, datatype)

    return legalEntity
}

fun LegalEntityGateOutputRequest.toLegalEntity(datatype: StageType): LegalEntityDb {

    val addressOutputRequest = AddressGateOutputRequest(
        address = legalAddress.address,
        externalId = getLegalAddressExternalIdForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = legalAddress.bpn
    )

    val legalEntity = LegalEntityDb(
        bpn = bpn,
        externalId = externalId,
        legalForm = legalEntity.legalForm,
        shortName = legalEntity.legalShortName,
        stage = datatype
    )

    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })
    legalEntity.nameParts.addAll(this.legalEntity.legalNameParts.map { toNameParts(it, null, null, legalEntity) })
    legalEntity.roles.addAll(this.legalEntity.roles.distinct().map { toRoles(it, legalEntity, null, null) })
    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifiers(it, legalEntity) })
    legalEntity.legalAddress = addressOutputRequest.toAddressGateOutput(legalEntity, null, datatype)

    return legalEntity
}

fun toRoles(role: BusinessPartnerRole, legalEntity: LegalEntityDb?, site: SiteDb?, address: LogisticAddressDb?): RolesDb {
    return RolesDb(legalEntity, address, site, role)
}

fun toEntityAddressIdentifiers(dto: AddressIdentifier, address: LogisticAddressDb): AddressIdentifierDb {
    return AddressIdentifierDb(dto.value, dto.type, address)
}

fun toEntityState(dto: LegalEntityState, legalEntity: LegalEntityDb): LegalEntityStateDb {
    return LegalEntityStateDb(dto.description, dto.validFrom, dto.validTo, dto.type, legalEntity)
}

fun toEntityClassification(dto: LegalEntityClassification, legalEntity: LegalEntityDb): LegalEntityClassificationDb {
    return LegalEntityClassificationDb(dto.value, dto.code, dto.type, legalEntity)
}

fun toEntityIdentifiers(dto: LegalEntityIdentifier, legalEntity: LegalEntityDb): LegalEntityIdentifierDb {
    return LegalEntityIdentifierDb(dto.value, dto.type, dto.issuingBody, legalEntity)
}

fun getMainAddressExternalIdForSiteExternalId(siteExternalId: String): String {
    return siteExternalId + "_site"
}

fun getLegalAddressExternalIdForLegalEntityExternalId(legalEntityExternalId: String): String {
    return legalEntityExternalId + "_legalAddress"
}

//Logistic Address mapping to AddressGateInputResponse
fun LogisticAddressDb.toAddressGateInputResponse(logisticAddressPage: LogisticAddressDb): AddressGateInputResponse {

    return AddressGateInputResponse(
        address = logisticAddressPage.toLogisticAddressDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity?.externalId,
        siteExternalId = site?.externalId,
    )
}

//Logistic Address mapping to LogisticAddressDto
fun LogisticAddressDb.toLogisticAddressDto(): LogisticAddress {

    return LogisticAddress(
        nameParts = getNamePartValues(nameParts),
        states = mapToDtoStates(states),
        roles = roles.map { it.roleName },
        physicalPostalAddress = physicalPostalAddress.toPhysicalPostalAddress(),
        alternativePostalAddress = alternativePostalAddress?.toAlternativePostalAddressDto(),
        identifiers = mapToAddressIdentifiersDto(identifiers)
    )
}

fun getNamePartValues(nameparts: MutableSet<NamePartsDb>): Collection<String> {
    return nameparts.map { it.namePart }
}

fun mapToDtoStates(states: MutableSet<AddressStateDb>): Collection<AddressState> {
    return states.map { AddressState(it.description, it.validFrom, it.validTo, it.type) }
}

fun AlternativePostalAddressDb.toAlternativePostalAddressDto() =
    AlternativePostalAddress(
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        deliveryServiceQualifier = deliveryServiceQualifier,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postalCode,
        city = city
    )

fun PhysicalPostalAddressDb.toPhysicalPostalAddress(): PhysicalPostalAddress =
    PhysicalPostalAddress(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postalCode,
        city = city,
        administrativeAreaLevel1 = administrativeAreaLevel1,
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = district,
        companyPostalCode = companyPostalCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door,
        street = street?.toStreetDto()
    )

fun GeographicCoordinateDb.toGeographicCoordinateDto(): GeoCoordinate {
    return GeoCoordinate(longitude, latitude, altitude)
}

private fun StreetDb.toStreetDto(): Street {

    return Street(
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

fun LegalEntityDb.toLegalEntityDto(): LegalEntity {

    return LegalEntity(
        legalNameParts = getNamePartValues(nameParts),
        legalForm = legalForm,
        legalShortName = shortName,
        states = mapToLegalEntityStateDto(states),
        classifications = mapToLegalEntityClassificationsDto(classifications),
        identifiers = mapToLegalEntityIdentifiersDto(identifiers),
        roles = roles.map { it.roleName },
    )
}

fun mapToLegalEntityStateDto(states: MutableSet<LegalEntityStateDb>): Collection<LegalEntityState> {
    return states.map { LegalEntityState(it.description, it.validFrom, it.validTo, it.type) }
}

fun mapToLegalEntityClassificationsDto(classification: MutableSet<LegalEntityClassificationDb>): Collection<LegalEntityClassification> {
    return classification.map { LegalEntityClassification(it.type, it.code, it.value) }
}

fun mapToLegalEntityIdentifiersDto(identifiers: MutableSet<LegalEntityIdentifierDb>): Collection<LegalEntityIdentifier> {
    return identifiers.map { LegalEntityIdentifier(it.value, it.type, it.issuingBody) }
}

fun mapToAddressIdentifiersDto(identifiers: MutableSet<AddressIdentifierDb>): Collection<AddressIdentifier> {
    return identifiers.map { AddressIdentifier(it.value, it.type) }
}

fun AddressIdentifierDb.mapToAddressIdentifiersDto(): AddressIdentifier {
    return AddressIdentifier(value, type)
}

//LegalEntity mapping to LegalEntityGateInputResponse
fun LegalEntityDb.toLegalEntityGateInputResponse(): LegalEntityGateInputResponse {
    return LegalEntityGateInputResponse(
        legalEntity = toLegalEntityDto(),
        legalAddress = legalAddress.toAddressGateInputResponse(legalAddress),
        externalId = externalId,
    )
}

//Site mapping to SiteDto
fun SiteDb.toSiteDto(): SiteGate {
    return SiteGate(
        roles = roles.map { it.roleName },
        nameParts = getNamePartValues(nameParts),
        states = mapToDtoSitesStates(states)
    )
}

fun mapToDtoSitesStates(states: MutableSet<SiteStateDb>): Collection<SiteState> {
    return states.map { SiteState(it.description, it.validFrom, it.validTo, it.type) }
}

//Site mapping to SiteGateInputResponse
fun SiteDb.toSiteGateInputResponse(sitePage: SiteDb): SiteGateInputResponse {
    return SiteGateInputResponse(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        mainAddress = mainAddress.toAddressGateInputResponse(mainAddress)
    )

}

//Logistic Address mapping to AddressGateOutputResponse
fun LogisticAddressDb.toAddressGateOutputResponse(logisticAddressPage: LogisticAddressDb): AddressGateOutputResponse {
    return AddressGateOutputResponse(
        address = logisticAddressPage.toLogisticAddressDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity?.externalId,
        siteExternalId = site?.externalId,
        bpna = bpn!!,
    )
}

//Site mapping to SiteGateOutputResponse
fun SiteDb.toSiteGateOutputResponse(sitePage: SiteDb): SiteGateOutputResponse {
    return SiteGateOutputResponse(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        bpns = bpn!!,
        mainAddress = mainAddress.toAddressGateOutputResponse(mainAddress)
    )
}

//LegalEntity mapping to LegalEntityGateOutputResponse
fun LegalEntityDb.toLegalEntityGateOutputResponse(): LegalEntityGateOutputResponse {
    return LegalEntityGateOutputResponse(
        legalEntity = toLegalEntityDto(),
        externalId = externalId,
        bpnl = bpn!!,
        legalAddress = legalAddress.toAddressGateOutputResponse(legalAddress)
    )
}

fun AddressGateInputRequest.toSharingStateDTO(): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.ADDRESS, externalId)
}

fun SiteGateInputRequest.toSharingStateDTO(): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.SITE, externalId)
}

fun LegalEntityGateInputRequest.toSharingStateDTO(): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.LEGAL_ENTITY, externalId)
}

fun AddressGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.ADDRESS, externalId, sharingStateType = sharingStateType, bpn = bpn)
}

fun SiteGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.SITE, externalId, sharingStateType = sharingStateType, bpn = bpn)
}

fun LegalEntityGateOutputRequest.toSharingStateDTO(sharingStateType: SharingStateType): SharingStateResponse {

    return SharingStateResponse(BusinessPartnerType.LEGAL_ENTITY, externalId, sharingStateType = sharingStateType, bpn = bpn)
}


