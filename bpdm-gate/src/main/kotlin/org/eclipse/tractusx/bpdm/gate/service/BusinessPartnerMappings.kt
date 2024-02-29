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


import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinate
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinateDb
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.StreetDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.springframework.stereotype.Service

@Service
class BusinessPartnerMappings {

    fun toBusinessPartnerInputDto(entity: BusinessPartnerDb): BusinessPartnerInputResponse {
        return BusinessPartnerInputResponse(
            externalId = entity.externalId,
            nameParts = entity.nameParts,
            identifiers = entity.identifiers.map(::toIdentifierDto),
            states = entity.states.map(::toStateDto),
            roles = entity.roles,
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = toLegalEntityComponentInputDto(entity),
            site = toSiteComponentInputDto(entity),
            address = toAddressComponentInputDto(entity),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toBusinessPartnerOutputDto(entity: BusinessPartnerDb): BusinessPartnerOutputResponse {
        return BusinessPartnerOutputResponse(
            externalId = entity.externalId,
            nameParts = entity.nameParts,
            identifiers = entity.identifiers.map(::toIdentifierDto),
            states = entity.states.map(::toStateDto),
            roles = entity.roles,
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = toLegalEntityComponentOutputDto(entity),
            site = toSiteComponentOutputDto(entity),
            address = toAddressComponentOutputDto(entity),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toBusinessPartnerInput(dto: BusinessPartnerInputRequest): BusinessPartnerDb {
        return BusinessPartnerDb(
            stage = StageType.Input,
            externalId = dto.externalId,
            nameParts = dto.nameParts.toMutableList(),
            roles = dto.roles.toSortedSet(),
            identifiers = dto.identifiers.mapNotNull(::toIdentifier).toSortedSet(),
            states = dto.states.mapNotNull(::toState).toSortedSet(),
            classifications = dto.legalEntity.classifications.mapNotNull(::toClassification).toSortedSet(),
            shortName = dto.legalEntity.shortName,
            legalName = dto.legalEntity.legalName,
            siteName = dto.site.name,
            addressName = dto.address.name,
            legalForm = dto.legalEntity.legalForm,
            isOwnCompanyData = dto.isOwnCompanyData,
            bpnL = dto.legalEntity.legalEntityBpn,
            bpnS = dto.site.siteBpn,
            bpnA = dto.address.addressBpn,
            postalAddress = toPostalAddress(dto.address),
            legalEntityConfidence = null,
            siteConfidence = null,
            addressConfidence = null
        )
    }

    private fun toLegalEntityComponentInputDto(entity: BusinessPartnerDb): LegalEntityRepresentationInputResponse {
        return LegalEntityRepresentationInputResponse(
            legalEntityBpn = entity.bpnL,
            legalName = entity.legalName,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            classifications = entity.classifications.map(::toClassificationDto)
        )
    }

    private fun toSiteComponentInputDto(entity: BusinessPartnerDb): SiteRepresentationInputResponse {
        return SiteRepresentationInputResponse(
            siteBpn = entity.bpnS,
            name = entity.siteName
        )
    }

    private fun toAddressComponentInputDto(entity: BusinessPartnerDb): AddressRepresentationInputResponse {
        return AddressRepresentationInputResponse(
            addressBpn = entity.bpnA,
            name = entity.addressName,
            addressType = entity.postalAddress.addressType,
            physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.toPhysicalPostalAddress() ?: PhysicalPostalAddress(),
            alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.toAlternativePostalAddressDto() ?: AlternativePostalAddress()
        )
    }

    private fun toLegalEntityComponentOutputDto(entity: BusinessPartnerDb): LegalEntityRepresentationOutputResponse {
        return LegalEntityRepresentationOutputResponse(
            legalEntityBpn = entity.bpnL ?: throw BpdmNullMappingException(
                BusinessPartnerDb::class,
                BusinessPartnerOutputResponse::class,
                BusinessPartnerDb::bpnL,
                entity.externalId
            ),
            legalName = entity.legalName,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            classifications = entity.classifications.map(::toClassificationDto),
            confidenceCriteria = entity.legalEntityConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                entity.externalId,
                "Missing address confidence criteria"
            )
        )
    }

    private fun toSiteComponentOutputDto(entity: BusinessPartnerDb): SiteRepresentationOutputResponse? {
        return entity
            .takeIf { it.bpnS != null }
            ?.let {
                SiteRepresentationOutputResponse(
                    siteBpn = entity.bpnS!!,
                    name = entity.siteName,
                    confidenceCriteria = entity.siteConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                        entity.externalId,
                        "Missing site confidence criteria"
                    )
                )
            }
    }

    private fun toAddressComponentOutputDto(entity: BusinessPartnerDb): AddressComponentOutputResponse {
        return AddressComponentOutputResponse(
            addressBpn = entity.bpnA ?: throw BpdmNullMappingException(
                BusinessPartnerDb::class,
                BusinessPartnerOutputResponse::class,
                BusinessPartnerDb::bpnA,
                entity.externalId
            ),
            entity.addressName,
            addressType = entity.postalAddress.addressType,
            physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.toPhysicalPostalAddress() ?: PhysicalPostalAddress(),
            alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.toAlternativePostalAddressDto() ?: AlternativePostalAddress(),
            confidenceCriteria = entity.addressConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                entity.externalId,
                "Missing legal entity confidence criteria"
            )
        )
    }

    private fun toConfidenceDto(entity: ConfidenceCriteriaDb) =
        ConfidenceCriteria(
            sharedByOwner = entity.sharedByOwner,
            checkedByExternalDataSource = entity.checkedByExternalDataSource,
            numberOfBusinessPartners = entity.numberOfBusinessPartners,
            lastConfidenceCheckAt = entity.lastConfidenceCheckAt,
            nextConfidenceCheckAt = entity.nextConfidenceCheckAt,
            confidenceLevel = entity.confidenceLevel
        )


    private fun toPostalAddress(dto: AddressRepresentationInputResponse) =
        PostalAddressDb(
            addressType = dto.addressType,
            physicalPostalAddress = normalize(dto.physicalPostalAddress)?.let(::toPhysicalPostalAddress),
            alternativePostalAddress = normalize(dto.alternativePostalAddress)?.let(::toAlternativePostalAddress)
        )

    // convert empty DTO to null
    private fun normalize(dto: PhysicalPostalAddress?) =
        if (dto != PhysicalPostalAddress()) dto
        else null

    private fun normalize(dto: AlternativePostalAddress?) =
        if (dto != AlternativePostalAddress()) dto
        else null

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddress) =
        PhysicalPostalAddressDb(
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


    private fun toAlternativePostalAddressDto(entity: AlternativePostalAddressDb) =
        AlternativePostalAddress(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            postalCode = entity.postalCode,
            city = entity.city,
            deliveryServiceType = entity.deliveryServiceType,
            deliveryServiceQualifier = entity.deliveryServiceQualifier,
            deliveryServiceNumber = entity.deliveryServiceNumber
        )

    private fun toAlternativePostalAddress(dto: AlternativePostalAddress) =
        AlternativePostalAddressDb(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeographicCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber
        )


    private fun toStreetDto(entity: StreetDb) =
        Street(
            name = entity.name,
            houseNumber = entity.houseNumber,
            houseNumberSupplement = entity.houseNumberSupplement,
            milestone = entity.milestone,
            direction = entity.direction,
            namePrefix = entity.namePrefix,
            additionalNamePrefix = entity.additionalNamePrefix,
            nameSuffix = entity.nameSuffix,
            additionalNameSuffix = entity.additionalNameSuffix
        )


    private fun toStreet(dto: Street) =
        StreetDb(
            name = dto.name,
            houseNumber = dto.houseNumber,
            houseNumberSupplement = dto.houseNumberSupplement,
            milestone = dto.milestone,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )

    private fun toIdentifierDto(entity: IdentifierDb) =
        BusinessPartnerIdentifier(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toIdentifier(dto: BusinessPartnerIdentifier) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                IdentifierDb(type = type, value = value, issuingBody = dto.issuingBody)
            }
        }

    private fun toStateDto(entity: StateDb) =
        BusinessPartnerState(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo)

    private fun toState(dto: BusinessPartnerState) =
        dto.type?.let { StateDb(type = it, validFrom = dto.validFrom, validTo = dto.validTo) }

    private fun toClassificationDto(entity: ClassificationDb) =
        BusinessPartnerClassification(type = entity.type, code = entity.code, value = entity.value)

    private fun toClassification(dto: BusinessPartnerClassification) =
        dto.type?.let { ClassificationDb(type = it, code = dto.code, value = dto.value) }

    private fun toGeoCoordinateDto(entity: GeographicCoordinateDb) =
        GeoCoordinate(latitude = entity.latitude, longitude = entity.longitude, altitude = entity.altitude)

    private fun toGeographicCoordinate(dto: GeoCoordinate) =
        GeographicCoordinateDb(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)
}
