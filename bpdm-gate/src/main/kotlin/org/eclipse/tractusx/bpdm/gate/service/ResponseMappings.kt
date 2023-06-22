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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.temporal.ChronoUnit

fun AddressGateInputRequest.toAddressGate(legalEntity: LegalEntity?, site: Site?, datatype: OutputInputEnum): LogisticAddress {

    val logisticAddress = LogisticAddress(
        externalId = externalId,
        siteExternalId = siteExternalId.toString(),
        name = address.nameParts.firstOrNull(),
        physicalPostalAddress = address.physicalPostalAddress.toPhysicalPostalAddressEntity(),
        alternativePostalAddress = address.alternativePostalAddress?.toAlternativePostalAddressEntity(),
        legalEntity = legalEntity,
        site = site,
        dataType = datatype
    )

    logisticAddress.identifiers.addAll(this.address.identifiers.map { toEntityIdentifier(it, logisticAddress) }.toSet())
    logisticAddress.states.addAll(this.address.states.map { toEntityAddress(it, logisticAddress) }.toSet())

    return logisticAddress
}

fun AddressGateOutputRequest.toAddressGateOutput(legalEntity: LegalEntity?, site: Site?, datatype: OutputInputEnum): LogisticAddress {

    val logisticAddress = LogisticAddress(
        bpn = bpn,
        externalId = externalId,
        siteExternalId = siteExternalId.toString(),
        name = address.nameParts.firstOrNull(),
        physicalPostalAddress = address.physicalPostalAddress.toPhysicalPostalAddressEntity(),
        alternativePostalAddress = address.alternativePostalAddress?.toAlternativePostalAddressEntity(),
        legalEntity = legalEntity,
        site = site,
        dataType = datatype
    )

    logisticAddress.identifiers.addAll(this.address.identifiers.map { toEntityIdentifier(it, logisticAddress) }.toSet())
    logisticAddress.states.addAll(this.address.states.map { toEntityAddress(it, logisticAddress) }.toSet())

    return logisticAddress
}

fun toEntityAddress(dto: AddressStateDto, address: LogisticAddress): AddressState {
    return AddressState(dto.description, dto.validFrom, dto.validTo, dto.type, address)
}

fun toEntityIdentifier(dto: AddressIdentifierDto, address: LogisticAddress): AddressIdentifier {
    return AddressIdentifier(dto.value, dto.type, address)
}

fun AlternativePostalAddressDto.toAlternativePostalAddressEntity(): AlternativePostalAddress {

    return AlternativePostalAddress(
        geographicCoordinates = baseAddress.geographicCoordinates?.toGeographicCoordinateEntity(),
        country = baseAddress.country,
        administrativeAreaLevel1 = null, // TODO Add region mapping Logic
        postCode = baseAddress.postalCode,
        city = baseAddress.city,
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber
    )

}

fun PhysicalPostalAddressGateDto.toPhysicalPostalAddressEntity(): PhysicalPostalAddress {

    return PhysicalPostalAddress(
        geographicCoordinates = baseAddress.geographicCoordinates?.toGeographicCoordinateEntity(),
        country = baseAddress.country,
        administrativeAreaLevel1 = null, // TODO Add region mapping Logic
        administrativeAreaLevel2 = areaPart.administrativeAreaLevel2,
        administrativeAreaLevel3 = areaPart.administrativeAreaLevel3,
        postCode = baseAddress.postalCode,
        city = baseAddress.city,
        districtLevel1 = areaPart.district,
        street = street?.toStreetEntity(),
        companyPostCode = basePhysicalAddress.companyPostalCode,
        industrialZone = basePhysicalAddress.industrialZone,
        building = basePhysicalAddress.building,
        floor = basePhysicalAddress.floor,
        door = basePhysicalAddress.door
    )

}

fun GeoCoordinateDto.toGeographicCoordinateEntity(): GeographicCoordinate {
    return GeographicCoordinate(longitude, latitude, altitude)
}

private fun StreetGateDto.toStreetEntity(): Street {
    return Street(
        name = name,
        houseNumber = houseNumber,
        milestone = milestone,
        direction = direction
    )
}

fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

// Site Mappers
fun SiteGateInputRequest.toSiteGate(legalEntity: LegalEntity, datatype: OutputInputEnum): Site {

    val addressInputRequest = AddressGateInputRequest(
        address = mainAddress,
        externalId = getMainAddressForSiteExternalId(externalId),
        siteExternalId = externalId
    )

    val site = Site(
        name = site.nameParts.firstOrNull()?: "",
        externalId = externalId,
        legalEntity = legalEntity,
        dataType = datatype
    )

    site.states.addAll(this.site.states.map { toEntityAddress(it, site) }.toSet())
    site.mainAddress = addressInputRequest.toAddressGate(null, site, datatype)

    return site
}

fun SiteGateOutputRequest.toSiteGate(legalEntity: LegalEntity, datatype: OutputInputEnum): Site {

    val addressOutputRequest = AddressGateOutputRequest(
        address = mainAddress.address,
        externalId = getMainAddressForSiteExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = mainAddress.bpn
    )

    val site = Site(
        bpn = bpn,
        name = site.nameParts.firstOrNull()?: "",
        externalId = externalId,
        legalEntity = legalEntity,
        dataType = datatype
    )

    site.states.addAll(this.site.states.map { toEntityAddress(it, site) }.toSet())
    site.mainAddress = addressOutputRequest.toAddressGateOutput(null, site, datatype)

    return site
}

fun toEntityAddress(dto: SiteStateDto, site: Site): SiteState {
    return SiteState(dto.description, dto.validFrom, dto.validTo, dto.type, site)
}

fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}

fun LegalEntityGateInputRequest.toLegalEntity(datatype: OutputInputEnum): LegalEntity {

    val addressInputRequest = AddressGateInputRequest(
        address = legalAddress,
        externalId = getMainAddressForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId
    )

    val legalEntity = LegalEntity(
        externalId = externalId,
        currentness = createCurrentnessTimestamp(),
        legalForm = legalEntity.legalForm,
        legalName = Name(legalNameParts[0], legalEntity.legalShortName),
        dataType = datatype
    )

    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifier(it, legalEntity) })
    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })

    legalEntity.legalAddress = addressInputRequest.toAddressGate(legalEntity, null, datatype)

    return legalEntity

}

fun LegalEntityGateOutputRequest.toLegalEntity(datatype: OutputInputEnum): LegalEntity {

    val addressOutputRequest = AddressGateOutputRequest(
        address = legalAddress.address,
        externalId = getMainAddressForLegalEntityExternalId(externalId),
        legalEntityExternalId = externalId,
        bpn = legalAddress.bpn
    )

    val legalEntity = LegalEntity(
        bpn = bpn,
        externalId = externalId,
        currentness = createCurrentnessTimestamp(),
        legalForm = legalEntity.legalForm,
        legalName = Name(legalNameParts[0], legalEntity.legalShortName),
        dataType = datatype
    )

    legalEntity.identifiers.addAll(this.legalEntity.identifiers.map { toEntityIdentifier(it, legalEntity) })
    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it, legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })

    legalEntity.legalAddress = addressOutputRequest.toAddressGateOutput(legalEntity, null, datatype)

    return legalEntity

}

fun toEntityIdentifier(dto: LegalEntityIdentifierDto, legalEntity: LegalEntity): LegalEntityIdentifier {
    return LegalEntityIdentifier(dto.value, dto.type, dto.issuingBody, legalEntity)
}

fun toEntityState(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
    return LegalEntityState(dto.officialDenotation, dto.validFrom, dto.validTo, dto.type, legalEntity)
}

fun toEntityClassification(dto: ClassificationDto, legalEntity: LegalEntity): Classification {
    return Classification(dto.value, dto.code, dto.type, legalEntity)
}

private fun createCurrentnessTimestamp(): Instant {
    return Instant.now().truncatedTo(ChronoUnit.MICROS)
}

fun getMainAddressForSiteExternalId(siteExternalId: String): String {
    return siteExternalId + "_site"
}

fun getMainAddressForLegalEntityExternalId(siteExternalId: String): String {
    return siteExternalId + "_legalAddress"
}

//Logistic Address mapping to AddressGateInputResponse
fun LogisticAddress.toAddressGateInputResponse(logisticAddressPage: LogisticAddress): AddressGateInputResponse {

    val addressGateInputResponse = AddressGateInputResponse(
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
        nameParts = name?.let { listOf(name!!) }?: emptyList(),
        states = mapToDtoStates(states),
        identifiers = mapToDtoIdentifiers(identifiers),
        physicalPostalAddress = physicalPostalAddress.toPhysicalPostalAddress(),
        alternativePostalAddress = alternativePostalAddress?.toAlternativePostalAddressDto(),
    )

    return logisticAddress
}

fun mapToDtoStates(states: MutableSet<AddressState>): Collection<AddressStateDto> {
    return states.map { AddressStateDto(it.description, it.validFrom, it.validTo, it.type) }
}

fun mapToDtoIdentifiers(identifier: MutableSet<AddressIdentifier>): Collection<AddressIdentifierDto> {
    return identifier.map { AddressIdentifierDto(it.value, it.type) }
}

fun AlternativePostalAddress.toAlternativePostalAddressDto(): AlternativePostalAddressDto {

    val basePostalAddressDto = BasePostalAddressDto(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postCode,
        city = city
    )

    val areaDistrictAlternativDto = AreaDistrictAlternativDto(
        administrativeAreaLevel1 = null // TODO Add region mapping Logic
    )

    return AlternativePostalAddressDto(
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber,
        areaPart = areaDistrictAlternativDto,
        baseAddress = basePostalAddressDto
    )

}

fun PhysicalPostalAddress.toPhysicalPostalAddress(): PhysicalPostalAddressGateDto {

    val basePostalAddressDto = BasePostalAddressDto(
        geographicCoordinates = geographicCoordinates?.toGeographicCoordinateDto(),
        country = country,
        postalCode = postCode,
        city = city
    )

    val areaDistrictDto = AreaDistrictDto(
        administrativeAreaLevel1 = null, // TODO Add region mapping Logic
        administrativeAreaLevel2 = administrativeAreaLevel2,
        administrativeAreaLevel3 = administrativeAreaLevel3,
        district = districtLevel1
    )

    return PhysicalPostalAddressGateDto(
        baseAddress = basePostalAddressDto,
        basePhysicalAddress = BasePhysicalAddressDto(
            companyPostalCode = companyPostCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
        ),
        street = street?.toStreetDto(),
        areaPart = areaDistrictDto
    )

}

fun GeographicCoordinate.toGeographicCoordinateDto(): GeoCoordinateDto {
    return GeoCoordinateDto(longitude, latitude, altitude)
}

private fun Street.toStreetDto(): StreetGateDto {
    return StreetGateDto(
        name = name,
        houseNumber = houseNumber,
        milestone = milestone,
        direction = direction
    )
}

fun LegalEntity.toLegalEntityDto(): LegalEntityDto {
    return LegalEntityDto(
        legalForm = legalForm,
        legalShortName = legalName.shortName,
        states = mapToLegalEntityStateDto(states),
        classifications = mapToLegalEntityClassificationsDto(classifications),
        identifiers = mapToLegalEntityIdentifierDto(identifiers)
    )

}

fun mapToLegalEntityStateDto(states: MutableSet<LegalEntityState>): Collection<LegalEntityStateDto> {
    return states.map { LegalEntityStateDto(it.officialDenotation, it.validFrom, it.validTo, it.type) }
}

fun mapToLegalEntityIdentifierDto(identifier: MutableSet<LegalEntityIdentifier>): Collection<LegalEntityIdentifierDto> {
    return identifier.map { LegalEntityIdentifierDto(it.value, it.type.toString(), it.issuingBody) }
}

fun mapToLegalEntityClassificationsDto(classification: MutableSet<Classification>): Collection<ClassificationDto> {
    return classification.map { ClassificationDto(it.value, it.code, it.type) }
}

//LegalEntity mapping to LegalEntityGateInputResponse
fun LegalEntity.toLegalEntityGateInputResponse(legalEntity: LegalEntity): LegalEntityGateInputResponse {

    return LegalEntityGateInputResponse(
        legalEntity = legalEntity.toLegalEntityDto(),
        legalAddress = legalAddress.toAddressGateInputResponse(legalAddress),
        externalId = legalEntity.externalId,
        legalNameParts = listOf(legalEntity.legalName.value)
    )
}

//Site mapping to SiteDto
fun Site.toSiteDto(): SiteGateDto {

    return SiteGateDto(
        nameParts = if(name.isEmpty()) emptyList() else listOf(name),
        states = mapToDtoSitesStates(states)
    )
}

fun mapToDtoSitesStates(states: MutableSet<SiteState>): Collection<SiteStateDto> {
    return states.map { SiteStateDto(it.description, it.validFrom, it.validTo, it.type) }
}

//Site mapping to SiteGateInputResponse
fun Site.toSiteGateInputResponse(sitePage: Site): SiteGateInputResponse {

    return SiteGateInputResponse(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        mainAddress = mainAddress.toAddressGateInputResponse(mainAddress)
    )

}

//Logistic Address mapping to AddressGateOutputResponse
fun LogisticAddress.toAddressGateOutputResponse(logisticAddressPage: LogisticAddress): AddressGateOutputResponse {

    val addressGateOutputResponse = AddressGateOutputResponse(
        address = logisticAddressPage.toLogisticAddressDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity?.externalId,
        siteExternalId = site?.externalId,
        bpn = bpn!!,
    )

    return addressGateOutputResponse
}

//Site mapping to SiteGateOutputResponse
fun Site.toSiteGateOutputResponse(sitePage: Site): SiteGateOutputResponse {

    return SiteGateOutputResponse(
        site = sitePage.toSiteDto(),
        externalId = externalId,
        legalEntityExternalId = legalEntity.externalId,
        bpn = bpn!!,
        mainAddress = mainAddress.toAddressGateOutputResponse(mainAddress)
    )
}

//LegalEntity mapping to LegalEntityGateOutputResponse
fun LegalEntity.toLegalEntityGateOutputResponse(legalEntity: LegalEntity): LegalEntityGateOutputResponse {

    return LegalEntityGateOutputResponse(
        legalEntity = legalEntity.toLegalEntityDto(),
        legalNameParts = listOf(legalName.value),
        externalId = legalEntity.externalId,
        bpn = legalEntity.bpn!!,
        legalAddress = legalAddress.toAddressGateOutputResponse(legalAddress)
    )
}


