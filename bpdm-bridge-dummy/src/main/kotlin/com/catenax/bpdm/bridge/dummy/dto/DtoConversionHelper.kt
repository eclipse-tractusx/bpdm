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

package com.catenax.bpdm.bridge.dummy.dto

import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutputChildRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGate
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddress
import java.time.LocalDateTime
import kotlin.reflect.KProperty
import org.eclipse.tractusx.bpdm.gate.api.model.AddressIdentifier as Gate_AddressIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.AddressState as Gate_AddressStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntity as Gate_LegalEntityDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityClassification as Gate_LegalEntityClassificationDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityIdentifier as Gate_LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityState as Gate_LegalEntityStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.LogisticAddress as Gate_LogisticAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalPostalAddress as Gate_PhysicalPostalAddressDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteState as Gate_SiteStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifier as Pool_AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.AddressState as Pool_AddressStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntity as Pool_LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityClassification as Pool_LegalEntityClassificationDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifier as Pool_LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityState as Pool_LegalEntityStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerbose as Pool_LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddress as Pool_PhysicalPostalAddressDto1
import org.eclipse.tractusx.bpdm.pool.api.model.SiteState as Pool_SiteStateDto

val dummyConfidenceCriteria = ConfidenceCriteria(
    sharedByOwner = false,
    numberOfBusinessPartners = 1,
    checkedByExternalDataSource = false,
    lastConfidenceCheckAt = LocalDateTime.now(),
    nextConfidenceCheckAt = LocalDateTime.now().plusDays(5),
    confidenceLevel = 0
)

fun gateToPoolLegalEntity(gateDto: Gate_LegalEntityDto): Pool_LegalEntityDto {
    return Pool_LegalEntityDto(
        identifiers = gateDto.identifiers.map(::gateToPoolLegalEntityIdentifier),
        legalName = gateDto.legalNameParts.firstOrNull() ?: "",
        legalShortName = gateDto.legalShortName,
        legalForm = gateDto.legalForm,
        states = gateDto.states.map(::gateToPoolLegalEntityState),
        classifications = gateDto.classifications.map(::gateToPoolLegalEntityClassification),
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun gateToPoolLegalEntityIdentifier(gateDto: Gate_LegalEntityIdentifierDto): Pool_LegalEntityIdentifierDto {
    return Pool_LegalEntityIdentifierDto(
        type = gateDto.type,
        value = gateDto.value,
        issuingBody = gateDto.issuingBody
    )
}

fun gateToPoolLegalEntityState(gateDto: Gate_LegalEntityStateDto): Pool_LegalEntityStateDto {
    return Pool_LegalEntityStateDto(
        validFrom = gateDto.validFrom,
        validTo = gateDto.validTo,
        type = gateDto.type
    )
}

fun gateToPoolLegalEntityClassification(gateDto: Gate_LegalEntityClassificationDto): Pool_LegalEntityClassificationDto {
    return Pool_LegalEntityClassificationDto(
        type = gateDto.type,
        code = gateDto.code,
        value = gateDto.value
    )
}

fun gateToPoolSiteState(gateDto: Gate_SiteStateDto): Pool_SiteStateDto {
    return Pool_SiteStateDto(
        validFrom = gateDto.validFrom,
        validTo = gateDto.validTo,
        type = gateDto.type
    )
}

fun gateToPoolLogisticAddress(gateDto: Gate_LogisticAddressDto): LogisticAddress {
    return LogisticAddress(
        name = gateDto.nameParts.firstOrNull(),
        states = gateDto.states.map(::gateToPoolAddressState),
        identifiers = gateDto.identifiers.map(::gateToPoolAddressIdentifier),
        physicalPostalAddress = gateToPoolPhysicalAddress(gateDto.physicalPostalAddress),
        alternativePostalAddress = gateDto.alternativePostalAddress?.let(::gateToPoolAlternativeAddress),
        confidenceCriteria = dummyConfidenceCriteria
    )
}

fun gateToPoolAddressState(gateDto: Gate_AddressStateDto): Pool_AddressStateDto {
    return Pool_AddressStateDto(
        validFrom = gateDto.validFrom,
        validTo = gateDto.validTo,
        type = gateDto.type
    )
}

fun gateToPoolAddressIdentifier(gateDto: Gate_AddressIdentifierDto): Pool_AddressIdentifierDto {
    return Pool_AddressIdentifierDto(
        type = gateDto.type,
        value = gateDto.value
    )
}

fun gateToPoolAlternativeAddress(gateDto: org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddress): AlternativePostalAddress {
    fun buildNullMappingException(nullField: KProperty<*>) =
        BpdmNullMappingException(AlternativePostalAddress::class, AlternativePostalAddress::class, nullField)

    return AlternativePostalAddress(
        geographicCoordinates = gateDto.geographicCoordinates,
        country = gateDto.country
            ?: throw buildNullMappingException(AlternativePostalAddress::country),
        administrativeAreaLevel1 = gateDto.administrativeAreaLevel1,
        postalCode = gateDto.postalCode,
        city = gateDto.city
            ?: throw buildNullMappingException(AlternativePostalAddress::city),
        deliveryServiceType = gateDto.deliveryServiceType
            ?: throw buildNullMappingException(AlternativePostalAddress::deliveryServiceType),
        deliveryServiceQualifier = gateDto.deliveryServiceQualifier,
        deliveryServiceNumber = gateDto.deliveryServiceNumber
            ?: throw buildNullMappingException(AlternativePostalAddress::deliveryServiceNumber)
    )
}

fun gateToPoolPhysicalAddress(gateDto: Gate_PhysicalPostalAddressDto): Pool_PhysicalPostalAddressDto1 {
    fun buildNullMappingException(nullField: KProperty<*>) =
        BpdmNullMappingException(Gate_PhysicalPostalAddressDto::class, Pool_PhysicalPostalAddressDto1::class, nullField)

    return Pool_PhysicalPostalAddressDto1(
        geographicCoordinates = gateDto.geographicCoordinates,
        country = gateDto.country
            ?: throw buildNullMappingException(Gate_PhysicalPostalAddressDto::country),
        postalCode = gateDto.postalCode,
        city = gateDto.city
            ?: throw buildNullMappingException(Gate_PhysicalPostalAddressDto::city),
        administrativeAreaLevel1 = gateDto.administrativeAreaLevel1,
        administrativeAreaLevel2 = gateDto.administrativeAreaLevel2,
        administrativeAreaLevel3 = gateDto.administrativeAreaLevel3,
        district = gateDto.district,
        companyPostalCode = gateDto.companyPostalCode,
        industrialZone = gateDto.industrialZone,
        building = gateDto.building,
        floor = gateDto.floor,
        door = gateDto.door,
        street = Street(
            name = gateDto.street?.name,
            houseNumber = gateDto.street?.houseNumber,
            milestone = gateDto.street?.milestone,
            direction = gateDto.street?.direction,
            houseNumberSupplement = gateDto.street?.houseNumberSupplement,
            namePrefix = gateDto.street?.namePrefix,
            additionalNamePrefix = gateDto.street?.additionalNamePrefix,
            nameSuffix = gateDto.street?.nameSuffix,
            additionalNameSuffix = gateDto.street?.additionalNameSuffix
        )
    )
}


fun poolToGateLegalEntity(legalEntity: LegalEntityVerbose): Gate_LegalEntityDto {
    val identifiers = legalEntity.identifiers.map {
        Gate_LegalEntityIdentifierDto(
            value = it.value,
            type = it.typeVerbose.technicalKey,
            issuingBody = it.issuingBody
        )
    }
    val states = legalEntity.states.map {
        Gate_LegalEntityStateDto(
            description = null,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.typeVerbose.technicalKey
        )
    }
    val classifications = legalEntity.classifications.map {
        Gate_LegalEntityClassificationDto(
            type = it.typeVerbose.technicalKey,
            code = it.code,
            value = it.value
        )
    }
    return Gate_LegalEntityDto(
        legalNameParts = listOfNotNull(legalEntity.legalName),
        legalShortName = legalEntity.legalShortName,
        legalForm = legalEntity.legalFormVerbose?.technicalKey,
        identifiers = identifiers,
        states = states,
        classifications = classifications
    )
}

fun poolToGateSite(site: SiteVerbose): SiteGate {
    val states = site.states.map {
        Gate_SiteStateDto(
            description = null,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.typeVerbose.technicalKey
        )
    }
    return SiteGate(
        nameParts = listOfNotNull(site.name),
        states = states
    )
}

fun poolToGateAddressChild(address: Pool_LogisticAddressVerboseDto): AddressGateOutputChildRequest {
    return AddressGateOutputChildRequest(
        address = poolToGateLogisticAddress(address),
        bpn = address.bpna
    )
}

fun poolToGateLogisticAddress(address: Pool_LogisticAddressVerboseDto): Gate_LogisticAddressDto {
    val states = address.states.map {
        Gate_AddressStateDto(
            description = null,
            validFrom = it.validFrom,
            validTo = it.validTo,
            type = it.typeVerbose.technicalKey
        )
    }
    val identifiers = address.identifiers.map {
        Gate_AddressIdentifierDto(
            value = it.value,
            type = it.typeVerbose.technicalKey
        )
    }
    return Gate_LogisticAddressDto(
        nameParts = listOfNotNull(address.name),
        states = states,
        identifiers = identifiers,
        physicalPostalAddress = poolToGatePhysicalAddress(address.physicalPostalAddress),
        alternativePostalAddress = address.alternativePostalAddress?.let(::poolToGateAlternativeAddress)
    )
}

private fun poolToGatePhysicalAddress(address: PhysicalPostalAddressVerbose): Gate_PhysicalPostalAddressDto {
    val street = address.street?.let {
        org.eclipse.tractusx.bpdm.gate.api.model.Street(
            name = it.name,
            houseNumber = it.houseNumber,
            milestone = it.milestone,
            direction = it.direction
        )
    }
    return Gate_PhysicalPostalAddressDto(
        geographicCoordinates = address.geographicCoordinates,
        country = address.country,
        postalCode = address.postalCode,
        city = address.city,
        administrativeAreaLevel1 = address.administrativeAreaLevel1,
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

private fun poolToGateAlternativeAddress(address: AlternativePostalAddressVerboseDto): org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddress {
    return org.eclipse.tractusx.bpdm.gate.api.model.AlternativePostalAddress(
        geographicCoordinates = address.geographicCoordinates,
        country = address.country,
        postalCode = address.postalCode,
        city = address.city,
        administrativeAreaLevel1 = address.administrativeAreaLevel1,
        deliveryServiceNumber = address.deliveryServiceNumber,
        deliveryServiceType = address.deliveryServiceType,
        deliveryServiceQualifier = address.deliveryServiceQualifier
    )
}
