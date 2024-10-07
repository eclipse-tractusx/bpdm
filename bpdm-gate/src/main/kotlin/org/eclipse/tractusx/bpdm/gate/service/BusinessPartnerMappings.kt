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


import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.springframework.stereotype.Service

@Service
class BusinessPartnerMappings {

    fun toBusinessPartnerInputDto(entity: BusinessPartnerDb): BusinessPartnerInputDto {
        return BusinessPartnerInputDto(
            externalId = entity.sharingState.externalId,
            nameParts = entity.nameParts,
            identifiers = entity.identifiers.map(::toIdentifierDto),
            states = entity.states.filter { it.businessPartnerTyp == BusinessPartnerType.GENERIC }.map(::toStateDto),
            roles = entity.roles,
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = toLegalEntityComponentInputDto(entity),
            site = toSiteComponentInputDto(entity),
            address = toAddressComponentInputDto(entity),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toBusinessPartnerOutputDto(entity: BusinessPartnerDb): BusinessPartnerOutputDto {
        return BusinessPartnerOutputDto(
            externalId = entity.sharingState.externalId,
            nameParts = entity.nameParts,
            identifiers = entity.postalAddress.addressType?.let { addressType ->
                entity.identifiers
                    .filter { it.businessPartnerType == when (addressType) {
                        AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> BusinessPartnerType.LEGAL_ENTITY
                        AddressType.AdditionalAddress -> BusinessPartnerType.ADDRESS
                        AddressType.SiteMainAddress -> return@let emptyList() // No identifiers for SiteMainAddress
                    } }
                    .map(::toIdentifierDto)
            }?: emptyList(),
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

    fun toBusinessPartnerInput(dto: BusinessPartnerInputRequest, sharingState: SharingStateDb): BusinessPartnerDb {
        return BusinessPartnerDb(
            stage = StageType.Input,
            nameParts = dto.nameParts.toMutableList(),
            roles = dto.roles.toSortedSet(),
            identifiers = dto.identifiers.mapNotNull{toIdentifier(it, BusinessPartnerType.GENERIC)}.toSortedSet(),
            states = dto.states.asSequence().mapNotNull{toState(it, BusinessPartnerType.GENERIC)}
                .plus(dto.legalEntity.states.mapNotNull { toState(it, BusinessPartnerType.LEGAL_ENTITY) })
                .plus(dto.site.states.mapNotNull { toState(it, BusinessPartnerType.SITE) })
                .plus(dto.address.states.mapNotNull { toState(it, BusinessPartnerType.ADDRESS) })
                .toSortedSet(),
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
            addressConfidence = null,
            sharingState = sharingState
        )
    }

    private fun toLegalEntityComponentInputDto(entity: BusinessPartnerDb): LegalEntityRepresentationInputDto {
        return LegalEntityRepresentationInputDto(
            legalEntityBpn = entity.bpnL,
            legalName = entity.legalName,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            states = toStateDtos(entity.states, BusinessPartnerType.LEGAL_ENTITY)
        )
    }

    private fun toSiteComponentInputDto(entity: BusinessPartnerDb): SiteRepresentationInputDto {
        return SiteRepresentationInputDto(
            siteBpn = entity.bpnS,
            name = entity.siteName,
            states = toStateDtos(entity.states, BusinessPartnerType.SITE)
        )
    }

    private fun toAddressComponentInputDto(entity: BusinessPartnerDb): AddressRepresentationInputDto {
        return AddressRepresentationInputDto(
            addressBpn = entity.bpnA,
            name = entity.addressName,
            addressType = entity.postalAddress.addressType,
            physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.toPhysicalPostalAddress() ?: PhysicalPostalAddressDto(),
            alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.toAlternativePostalAddressDto(),
            states = toStateDtos(entity.states, BusinessPartnerType.ADDRESS)
        )
    }

    private fun toLegalEntityComponentOutputDto(entity: BusinessPartnerDb): LegalEntityRepresentationOutputDto {
        return LegalEntityRepresentationOutputDto(
            legalEntityBpn = entity.bpnL ?: throw BpdmNullMappingException(
                BusinessPartnerDb::class,
                BusinessPartnerOutputDto::class,
                BusinessPartnerDb::bpnL,
                entity.sharingState.externalId
            ),
            legalName = entity.legalName,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            confidenceCriteria = entity.legalEntityConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                entity.sharingState.externalId,
                "Missing address confidence criteria"
            ),
            states = toStateDtos(entity.states, BusinessPartnerType.LEGAL_ENTITY)
        )
    }

    private fun toSiteComponentOutputDto(entity: BusinessPartnerDb): SiteRepresentationOutputDto? {
        return entity
            .takeIf { it.bpnS != null }
            ?.let {
                SiteRepresentationOutputDto(
                    siteBpn = entity.bpnS!!,
                    name = entity.siteName,
                    confidenceCriteria = entity.siteConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                        entity.sharingState.externalId,
                        "Missing site confidence criteria"
                    ),
                    states = toStateDtos(entity.states, BusinessPartnerType.SITE)
                )
            }
    }

    private fun toAddressComponentOutputDto(entity: BusinessPartnerDb): AddressComponentOutputDto {
        return AddressComponentOutputDto(
            addressBpn = entity.bpnA ?: throw BpdmNullMappingException(
                BusinessPartnerDb::class,
                BusinessPartnerOutputDto::class,
                BusinessPartnerDb::bpnA,
                entity.sharingState.externalId
            ),
            entity.addressName,
            addressType = entity.postalAddress.addressType,
            physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.toPhysicalPostalAddress() ?: PhysicalPostalAddressDto(),
            alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.toAlternativePostalAddressDto() ?: AlternativePostalAddressDto(),
            confidenceCriteria = entity.addressConfidence?.let { toConfidenceDto(it) } ?: throw BpdmInvalidPartnerException(
                entity.sharingState.externalId,
                "Missing legal entity confidence criteria"
            ),
            states = toStateDtos(entity.states, BusinessPartnerType.ADDRESS)
        )
    }

    private fun toConfidenceDto(entity: ConfidenceCriteriaDb) =
        ConfidenceCriteriaDto(
            sharedByOwner = entity.sharedByOwner,
            checkedByExternalDataSource = entity.checkedByExternalDataSource,
            numberOfSharingMembers = entity.numberOfBusinessPartners,
            lastConfidenceCheckAt = entity.lastConfidenceCheckAt,
            nextConfidenceCheckAt = entity.nextConfidenceCheckAt,
            confidenceLevel = entity.confidenceLevel
        )


    private fun toPostalAddress(dto: AddressRepresentationInputDto) =
        PostalAddressDb(
            addressType = dto.addressType,
            physicalPostalAddress = normalize(dto.physicalPostalAddress)?.let(::toPhysicalPostalAddress),
            alternativePostalAddress = normalize(dto.alternativePostalAddress)?.let(::toAlternativePostalAddress)
        )

    // convert empty DTO to null
    private fun normalize(dto: PhysicalPostalAddressDto?) =
        if (dto != PhysicalPostalAddressDto()) dto
        else null

    private fun normalize(dto: AlternativePostalAddressDto?) =
        if (dto != AlternativePostalAddressDto()) dto
        else null

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddressDto) =
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
            door = dto.door,
            taxJurisdictionCode = dto.taxJurisdictionCode
        )

    private fun toAlternativePostalAddress(dto: AlternativePostalAddressDto) =
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


    private fun toStreet(dto: StreetDto) =
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
        BusinessPartnerIdentifierDto(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toIdentifier(dto: BusinessPartnerIdentifierDto, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                IdentifierDb(type = type, value = value, issuingBody = dto.issuingBody, businessPartnerType)
            }
        }

    private fun toStateDtos(entities: Collection<StateDb>, businessPartnerType: BusinessPartnerType) =
        entities.filter { it.businessPartnerTyp == businessPartnerType }.map(::toStateDto)

    private fun toStateDto(entity: StateDb) =
        BusinessPartnerStateDto(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo)

    private fun toState(dto: BusinessPartnerStateDto, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { StateDb(type = it, validFrom = dto.validFrom, validTo = dto.validTo, businessPartnerTyp = businessPartnerType) }

    private fun toGeographicCoordinate(dto: GeoCoordinateDto) =
        GeographicCoordinateDb(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)
}
