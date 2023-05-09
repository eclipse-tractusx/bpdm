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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.temporal.ChronoUnit

fun AddressGateInputRequest.toAddressGate(legalEntity: LegalEntity?): LogisticAddress {

    val logisticAddress = LogisticAddress(
        bpn = bpn,
        externalId = externalId,
        siteExternalId = siteExternalId.toString(),
        name = address.name,
        physicalPostalAddress = address.physicalPostalAddress.toPhysicalPostalAddressEntity(),
        alternativePostalAddress = address.alternativePostalAddress?.toAlternativePostalAddressEntity(),
        legalEntity = legalEntity,
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
        administrativeAreaLevel2 = baseAddress.administrativeAreaLevel2,
        administrativeAreaLevel3 = baseAddress.administrativeAreaLevel3,
        administrativeAreaLevel4 = baseAddress.administrativeAreaLevel4,
        postCode = baseAddress.postCode,
        city = baseAddress.city,
        districtLevel1 = baseAddress.districtLevel1,
        districtLevel2 = baseAddress.districtLevel2,
        street = baseAddress.street?.toStreetEntity(),
        deliveryServiceType = deliveryServiceType,
        deliveryServiceNumber = deliveryServiceNumber
    )

}

fun PhysicalPostalAddressDto.toPhysicalPostalAddressEntity(): PhysicalPostalAddress {

    return PhysicalPostalAddress(
        geographicCoordinates = baseAddress.geographicCoordinates?.toGeographicCoordinateEntity(),
        country = baseAddress.country,
        administrativeAreaLevel1 = null, // TODO Add region mapping Logic
        administrativeAreaLevel2 = baseAddress.administrativeAreaLevel2,
        administrativeAreaLevel3 = baseAddress.administrativeAreaLevel3,
        administrativeAreaLevel4 = baseAddress.administrativeAreaLevel4,
        postCode = baseAddress.postCode,
        city = baseAddress.city,
        districtLevel1 = baseAddress.districtLevel1,
        districtLevel2 = baseAddress.districtLevel2,
        street = baseAddress.street?.toStreetEntity(),
        companyPostCode = companyPostCode,
        industrialZone = industrialZone,
        building = building,
        floor = floor,
        door = door
    )

}

fun GeoCoordinateDto.toGeographicCoordinateEntity(): GeographicCoordinate {
    return GeographicCoordinate(longitude, latitude, altitude)
}

private fun StreetDto.toStreetEntity(): Street {
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

fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}

fun LegalEntityGateInputRequest.toLegalEntity(): LegalEntity {

    val addressInputRequest =AddressGateInputRequest(
        address= legalEntity.legalAddress,
        externalId= externalId+"_legalAddress",
        legalEntityExternalId= externalId
    )

    val legalEntity= LegalEntity(
        bpn = bpn,
        externalId = externalId,
        currentness = createCurrentnessTimestamp(),
        legalForm = legalEntity.legalForm,
        legalName = legalEntity.legalName.toName()
    )

    legalEntity.identifiers.addAll( this.legalEntity.identifiers.map {toEntityIdentifier(it,legalEntity)})
    legalEntity.states.addAll(this.legalEntity.states.map { toEntityState(it,legalEntity) })
    legalEntity.classifications.addAll(this.legalEntity.classifications.map { toEntityClassification(it,legalEntity) })
    legalEntity.legalAddress = addressInputRequest.toAddressGate(legalEntity)

    return legalEntity

}
fun toEntityIdentifier(dto: LegalEntityIdentifierDto, legalEntity: LegalEntity): LegalEntityIdentifier {
    return LegalEntityIdentifier(dto.value, dto.type,dto.issuingBody, legalEntity)
}
fun toEntityState(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
    return LegalEntityState(dto.officialDenotation,dto.validFrom,dto.validTo,dto.type,legalEntity)
}
fun toEntityClassification(dto: ClassificationDto, legalEntity: LegalEntity): Classification {
    return Classification(dto.value,dto.code,dto.type,legalEntity)
}
fun NameDto.toName(): Name{
    return Name(value, shortName)
}
private fun createCurrentnessTimestamp(): Instant {
    return Instant.now().truncatedTo(ChronoUnit.MICROS)
}