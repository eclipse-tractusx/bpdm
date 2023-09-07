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

package com.catenax.bpdm.bridge.dummy.dto

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.gate.api.model.*

fun gateToPoolLogisticAddress(gateDto: LogisticAddressGateDto): LogisticAddressDto {
    return LogisticAddressDto(
        name = gateDto.nameParts.firstOrNull(),
        states = gateDto.states,
        identifiers = gateDto.identifiers,
        physicalPostalAddress = gateToPoolPhysicalAddress(gateDto.physicalPostalAddress),
        alternativePostalAddress = gateDto.alternativePostalAddress
    )
}

fun gateToPoolPhysicalAddress(gateDto: PhysicalPostalAddressGateDto): PhysicalPostalAddressDto {
    return PhysicalPostalAddressDto(
        geographicCoordinates = gateDto.geographicCoordinates,
        country = gateDto.country,
        postalCode = gateDto.postalCode,
        city = gateDto.city,
        administrativeAreaLevel1 = gateDto.administrativeAreaLevel1,
        administrativeAreaLevel2 = gateDto.administrativeAreaLevel2,
        administrativeAreaLevel3 = gateDto.administrativeAreaLevel3,
        district = gateDto.district,
        companyPostalCode = gateDto.companyPostalCode,
        industrialZone = gateDto.industrialZone,
        building = gateDto.building,
        floor = gateDto.floor,
        door = gateDto.door,
        street = StreetDto(
            name = gateDto.street?.name,
            houseNumber = gateDto.street?.houseNumber,
            milestone = gateDto.street?.milestone,
            direction = gateDto.street?.direction
        )
    )
}


fun poolToGateLegalEntity(legalEntity: LegalEntityVerboseDto): LegalEntityDto {
    val identifiers = legalEntity.identifiers.map {
        LegalEntityIdentifierDto(
            value = it.value,
            type = it.type.technicalKey,
            issuingBody = it.issuingBody
        )
    }
    val states = legalEntity.states.map {
        LegalEntityStateDto(
            description = it.description,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.type.technicalKey
        )
    }
    val classifications = legalEntity.classifications.map {
        ClassificationDto(
            value = it.value,
            code = it.code,
            type = it.type.technicalKey
        )
    }
    return LegalEntityDto(
        identifiers = identifiers,
        legalShortName = legalEntity.legalShortName,
        legalForm = legalEntity.legalForm?.technicalKey,
        states = states,
        classifications = classifications
    )
}

fun poolToGateSite(site: SiteVerboseDto): SiteGateDto {
    val states = site.states.map {
        SiteStateDto(
            description = it.description,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.type.technicalKey
        )
    }
    return SiteGateDto(
        nameParts = listOfNotNull(site.name),
        states = states
    )
}

fun poolToGateAddressChild(address: LogisticAddressVerboseDto): AddressGateOutputChildRequest {
    return AddressGateOutputChildRequest(
        address = poolToGateLogisticAddress(address),
        bpn = address.bpna
    )
}

fun poolToGateLogisticAddress(address: LogisticAddressVerboseDto): LogisticAddressGateDto {
    val states = address.states.map {
        AddressStateDto(
            description = it.description,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.type.technicalKey
        )
    }
    val identifiers = address.identifiers.map {
        AddressIdentifierDto(
            value = it.value,
            type = it.type.technicalKey
        )
    }
    return LogisticAddressGateDto(
        nameParts = listOfNotNull(address.name),
        states = states,
        identifiers = identifiers,
        physicalPostalAddress = poolToGatePhysicalAddress(address.physicalPostalAddress),
        alternativePostalAddress = address.alternativePostalAddress?.let { poolToGateAlternativeAddress(it) }
    )
}

private fun poolToGatePhysicalAddress(address: PhysicalPostalAddressVerboseDto): PhysicalPostalAddressGateDto {
    val street = address.street?.let {
        StreetGateDto(
            name = it.name,
            houseNumber = it.houseNumber,
            milestone = it.milestone,
            direction = it.direction
        )
    }
    return PhysicalPostalAddressGateDto(
        geographicCoordinates = address.geographicCoordinates,
        country = address.country.technicalKey,
        postalCode = address.postalCode,
        city = address.city,
        administrativeAreaLevel1 = address.administrativeAreaLevel1?.regionCode,
        administrativeAreaLevel2 = address.administrativeAreaLevel2,
        administrativeAreaLevel3 = address.administrativeAreaLevel3,
        district = address.district,
        companyPostalCode = address.companyPostalCode,
        industrialZone = address.industrialZone,
        building = address.building,
        floor = address.floor,
        door = address.door,
        street = street,
    )
}

private fun poolToGateAlternativeAddress(address: AlternativePostalAddressVerboseDto): AlternativePostalAddressDto {
    return AlternativePostalAddressDto(
        geographicCoordinates = address.geographicCoordinates,
        country = address.country.technicalKey,
        postalCode = address.postalCode,
        city = address.city,
        administrativeAreaLevel1 = address.administrativeAreaLevel1?.regionCode,
        deliveryServiceNumber = address.deliveryServiceNumber,
        deliveryServiceType = address.deliveryServiceType,
        deliveryServiceQualifier = address.deliveryServiceQualifier
    )
}
