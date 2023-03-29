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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.*

fun AddressGateInputRequest.toAddressGate(): AddressGate {

    val address = AddressGate(
        careOf = address.careOf,
        country = address.country,
        version = address.version.toAddressVersionGate(),
        geoCoordinates = address.geographicCoordinates?.toGeographicCoordinateGate(),
        externalId = externalId,
        legalEntityExternalId = legalEntityExternalId.toString(),
        siteExternalId = siteExternalId.toString(),
        bpn = bpn.toString(),
    )

    address.postCodes.addAll(this.address.postCodes.map { toEntity(it, address) }.toSet())
    address.administrativeAreas.addAll(this.address.administrativeAreas.map { toEntity(it, address) }.toSet())
    address.thoroughfares.addAll(this.address.thoroughfares.map { toEntity(it, address) }.toSet())
    address.localities.addAll(this.address.localities.map { toEntity(it, address) }.toSet())
    address.premises.addAll(this.address.premises.map { toEntity(it, address) }.toSet())
    address.postalDeliveryPoints.addAll(this.address.postalDeliveryPoints.map { toEntity(it, address) }.toSet())
    address.contexts.addAll(this.address.contexts)
    address.types.addAll(this.address.types)

    return address
}

fun toEntity(dto: PostCodeDto, address: AddressGate): PostCodeGate {
    return PostCodeGate(dto.value, dto.type, address)
}

fun toEntity(dto: AdministrativeAreaDto, address: AddressGate): AdministrativeAreaGate {
    return AdministrativeAreaGate(dto.value, dto.shortName, dto.fipsCode, dto.type, address.version.language, address.country, address)
}

fun toEntity(dto: ThoroughfareDto, address: AddressGate): ThoroughfareGate {
    return ThoroughfareGate(dto.value, dto.name, dto.shortName, dto.number, dto.direction, dto.type, address)
}

fun toEntity(dto: LocalityDto, address: AddressGate): LocalityGate {
    return LocalityGate(dto.value, dto.shortName, dto.type, address)
}

fun toEntity(dto: PremiseDto, address: AddressGate): PremiseGate {
    return PremiseGate(dto.value, dto.shortName, dto.number, dto.type, address)
}

fun toEntity(dto: PostalDeliveryPointDto, address: AddressGate): PostalDeliveryPointGate {
    return PostalDeliveryPointGate(dto.value, dto.shortName, dto.number, dto.type, address)
}

fun GeoCoordinateDto.toGeographicCoordinateGate(): GeographicCoordinateGate {

    return GeographicCoordinateGate(
        this.longitude,
        this.latitude,
        this.altitude
    )

}

fun AddressVersionDto.toAddressVersionGate(): AddressVersionGate {

    return AddressVersionGate(
        this.characterSet,
        this.language
    )

}

fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}
