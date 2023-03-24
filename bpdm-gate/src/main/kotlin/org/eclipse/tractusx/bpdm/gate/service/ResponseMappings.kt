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
import org.eclipse.tractusx.bpdm.common.dto.AddressDto
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>): PageResponse<T> {
    return PageResponse(this.totalElements, this.totalPages, this.number, this.numberOfElements, dtoContent)
}

fun AddressGate.toAddressGateInputRequest(): AddressGateInputRequest {

    return AddressGateInputRequest(
        AddressDto(),
        this.externalId,
        this.legalEntityExternalId,
        this.siteExternalId,
        this.bpn
    )
}

fun AddressDto.toAddressGateDto(): AddressGate {

    val geoCoords = this.geographicCoordinates?.let {
        GeoCoordinateDto(
            it.longitude,
            it.latitude,
            it.altitude
        ).toGeographicCoordinateGate()
    }

    return AddressGate(
        this.careOf,
        this.contexts.toMutableSet(),
        this.country,
        this.types.toMutableSet(),
        this.version.toAddressVersionGate(),
        geoCoords,
        "",
        "",
        "",
        ""
    )
}

fun AddressGateInputRequest.toAddressGate(): AddressGate {

    val geoCoords = this.address.geographicCoordinates?.let {
        GeoCoordinateDto(
            it.longitude,
            it.latitude,
            it.altitude
        ).toGeographicCoordinateGate()
    }

    return AddressGate(
        this.address.careOf,
        this.address.contexts.toMutableSet(),
        this.address.country,
        this.address.types.toMutableSet(),
        this.address.version.toAddressVersionGate(),
        geoCoords,
        this.externalId,
        this.legalEntityExternalId.toString(),
        this.siteExternalId.toString(),
        this.bpn.toString()
    )

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

fun AddressVersionGate.toAddressDto(): AddressVersionDto {

    return AddressVersionDto(
        this.characterSet,
        this.language
    )
}

//Legal Entities

//fun LegalEntityGate.LegalEntityGateInputRequest(): LegalEntityGateInputRequest {
//
//    return LegalEntityGateInputRequest(
//        LegalEntityDto(),
//        this.externalId,
//        this.bpn,
//    )
//
//}
//
//fun LegalEntityGateInputRequest.toLegalEntityGate(): LegalEntityGate {
//
//    return LegalEntityGate(
//        this.bpn.toString(),
//        this.legalEntity.legalForm,
//        this.legalEntity.types,
//        this.legalEntity,
//        this.externalId,
//    )
//}

// Site Mappers
fun SiteGate.toSiteGateInputRequest(): SiteGateInputRequest {

    return SiteGateInputRequest(
        SiteDto(this.name, AddressDto()),
        this.bpn,
        this.legalEntityExternalId,
        this.externalId
    )
}

fun SiteGateInputRequest.toSiteGate(): SiteGate {

    return SiteGate(
        this.bpn.toString(),
        this.site.name,
        this.externalId,
        this.legalEntityExternalId,
        this.site.mainAddress.toAddressGateDto(),
    )
}


fun ChangelogEntry.toGateDto(): ChangelogResponse {
    return ChangelogResponse(
        externalId,
        businessPartnerType,
        createdAt
    )
}
