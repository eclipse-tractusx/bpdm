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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinate
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinateDb
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.StreetDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service

@Service
class OrchestratorMappings(
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun toBusinessPartnerGenericDto(entity: BusinessPartnerDb) = BusinessPartnerGeneric(
        nameParts = entity.nameParts,
        identifiers = entity.identifiers.map { toIdentifierDto(it) },
        states = entity.states.map { toStateDto(it) },
        roles = entity.roles,
        ownerBpnL = getOwnerBpnL(entity),
        legalEntity = toLegalEntityComponentDto(entity),
        site = toSiteComponentDto(entity),
        address = toAddressComponentDto(entity)

    )

    private fun toLegalEntityComponentDto(entity: BusinessPartnerDb) = LegalEntityRepresentation(
        legalEntityBpn = entity.bpnL,
        legalName = entity.legalName,
        shortName = entity.shortName,
        legalForm = entity.legalForm,
        classifications = entity.classifications.map { toClassificationDto(it) },
        confidenceCriteria = entity.legalEntityConfidence?.let { toConfidenceCriteria(it) }
    )

    private fun toSiteComponentDto(entity: BusinessPartnerDb) = SiteRepresentation(
        siteBpn = entity.bpnS,
        name = entity.siteName,
        confidenceCriteria = entity.siteConfidence?.let { toConfidenceCriteria(it) }
    )

    private fun toAddressComponentDto(entity: BusinessPartnerDb) = AddressRepresentation(
        addressBpn = entity.bpnA,
        name = entity.addressName,
        addressType = entity.postalAddress.addressType,
        physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(::toPhysicalPostalAddressDto),
        alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(this::toAlternativePostalAddressDto),
        confidenceCriteria = entity.addressConfidence?.let { toConfidenceCriteria(it) }
    )

    private fun toClassificationDto(entity: ClassificationDb) =
        BusinessPartnerClassification(type = entity.type, code = entity.code, value = entity.value)

    private fun toPostalAddressDto(entity: PostalAddressDb) =
        PostalAddress(
            addressType = entity.addressType,
            physicalPostalAddress = entity.physicalPostalAddress?.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = entity.alternativePostalAddress?.let(this::toAlternativePostalAddressDto)
        )

    private fun toPhysicalPostalAddressDto(entity: PhysicalPostalAddressDb) =
        PhysicalPostalAddress(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            administrativeAreaLevel2 = entity.administrativeAreaLevel2,
            administrativeAreaLevel3 = entity.administrativeAreaLevel3,
            postalCode = entity.postalCode,
            city = entity.city,
            district = entity.district,
            street = entity.street?.let(::toStreetDto),
            companyPostalCode = entity.companyPostalCode,
            industrialZone = entity.industrialZone,
            building = entity.building,
            floor = entity.floor,
            door = entity.door
        )

    private fun toAlternativePostalAddressDto(entity: AlternativePostalAddressDb): AlternativePostalAddress =
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

    private fun toStateDto(entity: StateDb) =
        BusinessPartnerState(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo)

    private fun toIdentifierDto(entity: IdentifierDb) =
        BusinessPartnerIdentifier(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toGeoCoordinateDto(entity: GeographicCoordinateDb) =
        GeoCoordinate(latitude = entity.latitude, longitude = entity.longitude, altitude = entity.altitude)

    private fun getOwnerBpnL(entity: BusinessPartnerDb): String? {
        return if (entity.isOwnCompanyData) bpnConfigProperties.ownerBpnL else {
            logger.warn { "Owner BPNL property is not configured" }
            null
        }
    }

    private fun toConfidenceCriteria(entity: ConfidenceCriteriaDb) =
        ConfidenceCriteria(
            sharedByOwner = entity.sharedByOwner,
            checkedByExternalDataSource = entity.checkedByExternalDataSource,
            numberOfBusinessPartners = entity.numberOfBusinessPartners,
            lastConfidenceCheckAt = entity.lastConfidenceCheckAt,
            nextConfidenceCheckAt = entity.nextConfidenceCheckAt,
            confidenceLevel = entity.confidenceLevel
        )

    fun toSharingStateType(resultState: ResultState) = when (resultState) {
        ResultState.Pending -> SharingStateType.Pending
        ResultState.Success -> SharingStateType.Success
        ResultState.Error -> SharingStateType.Error
    }

    //Mapping BusinessPartnerGenericDto from to BusinessPartner
    fun toBusinessPartner(dto: BusinessPartnerGeneric, externalId: String) = BusinessPartnerDb(
        externalId = externalId,
        nameParts = dto.nameParts.toMutableList(),
        shortName = dto.legalEntity.shortName,
        identifiers = dto.identifiers.mapNotNull { toIdentifier(it) }.toSortedSet(),
        legalName = dto.legalEntity.legalName,
        siteName = dto.site.name,
        addressName = dto.address.name,
        legalForm = dto.legalEntity.legalForm,
        states = dto.states.mapNotNull { toState(it) }.toSortedSet(),
        classifications = dto.legalEntity.classifications.map { toClassification(it) }.toSortedSet(),
        roles = dto.roles.toSortedSet(),
        postalAddress = toPostalAddress(dto.address),
        bpnL = dto.legalEntity.legalEntityBpn,
        bpnS = dto.site.siteBpn,
        bpnA = dto.address.addressBpn,
        stage = StageType.Output,
        legalEntityConfidence = dto.legalEntity.confidenceCriteria?.let { toConfidenceCriteria(it) },
        siteConfidence = dto.site.confidenceCriteria?.let { toConfidenceCriteria(it) },
        addressConfidence = dto.address.confidenceCriteria?.let { toConfidenceCriteria(it) },
    )

    private fun toIdentifier(dto: BusinessPartnerIdentifier) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                IdentifierDb(type = type, value = value, issuingBody = dto.issuingBody)
            }
        }

    private fun toState(dto: BusinessPartnerState) =
        dto.type?.let { StateDb(type = it, validFrom = dto.validFrom, validTo = dto.validTo) }

    private fun toClassification(dto: BusinessPartnerClassification) =
        ClassificationDb(type = dto.type, code = dto.code, value = dto.value)

    private fun toPostalAddress(dto: AddressRepresentation) =
        PostalAddressDb(
            addressType = dto.addressType,
            physicalPostalAddress = dto.physicalPostalAddress?.let(::toPhysicalPostalAddress),
            alternativePostalAddress = dto.alternativePostalAddress?.let(this::toAlternativePostalAddress)
        )

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

    private fun toGeographicCoordinate(dto: GeoCoordinate) =
        GeographicCoordinateDb(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)

    private fun toConfidenceCriteria(dto: ConfidenceCriteria) =
        ConfidenceCriteriaDb(
            sharedByOwner = dto.sharedByOwner,
            checkedByExternalDataSource = dto.checkedByExternalDataSource,
            numberOfBusinessPartners = dto.numberOfBusinessPartners,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt,
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt,
            confidenceLevel = dto.confidenceLevel
        )
}