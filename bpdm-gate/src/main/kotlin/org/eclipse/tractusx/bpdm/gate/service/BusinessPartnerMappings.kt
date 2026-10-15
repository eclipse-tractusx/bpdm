/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.SiteGoldenRecordRelationType
import org.springframework.stereotype.Service
import java.time.Instant

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
            externalSequenceTimestamp = entity.externalSequenceTimestamp,
            scriptVariants = entity.scriptVariants.map { variant -> toScriptVariantDto(variant) },
            additionalSites = entity.additionalSites.map { AdditionalSiteInputDto(siteBpn = it.bpn, name = it.name) },
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
            states = entity.postalAddress.addressType?.let { addressType ->
                entity.states
                    .filter { it.businessPartnerTyp == when (addressType) {
                        AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> BusinessPartnerType.LEGAL_ENTITY
                        AddressType.AdditionalAddress -> BusinessPartnerType.ADDRESS
                        AddressType.SiteMainAddress -> BusinessPartnerType.SITE
                    } }
                    .map(::toStateDto)
            }?: emptyList(),
            roles = entity.roles,
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = toLegalEntityComponentOutputDto(entity),
            site = toSiteComponentOutputDto(entity),
            address = toAddressComponentOutputDto(entity),
            externalSequenceTimestamp = entity.externalSequenceTimestamp,
            scriptVariants = entity.scriptVariants.map { variant -> toScriptVariantDto(variant) },
            additionalSites = entity.additionalSites.map { toAdditionalSiteOutputDto(it, entity) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun toAdditionalSiteOutputDto(additionalSite: AdditionalSiteDb, entity: BusinessPartnerDb) =
        AdditionalSiteOutputDto(
            siteBpn = additionalSite.bpn ?: throw BpdmNullMappingException(
                AdditionalSiteDb::class,
                BusinessPartnerOutputDto::class,
                AdditionalSiteDb::bpn,
                entity.sharingState.externalId
            ),
            name = additionalSite.name
        )

    private fun toLegalEntityComponentInputDto(entity: BusinessPartnerDb): LegalEntityRepresentationInputDto {
        return LegalEntityRepresentationInputDto(
            legalEntityBpn = entity.bpnL,
            legalName = entity.legalName,
            shortName = entity.shortName,
            legalForm = entity.legalForm,
            ownershipUltimate = entity.ownershipUltimate,
            ultimateOwnerBpnl = entity.ultimateOwnerBpnl,
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
            ownershipUltimate = entity.ownershipUltimate,
            ultimateOwnerBpnl = entity.ultimateOwnerBpnl,
            states = toStateDtos(entity.states, BusinessPartnerType.LEGAL_ENTITY),
            goldenRecordRelations = entity.legalEntityGoldenRecordRelations.map { LegalEntityGoldenRecordRelationDto(toLeRelationType(it.relationType), it.sourceBpn, it.targetBpn) },
            updatedAt = entity.legalEntityUpdatedAt ?: Instant.EPOCH
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
                    states = toStateDtos(entity.states, BusinessPartnerType.SITE),
                    goldenRecordRelations = entity.siteGoldenRecordRelations.map { relation ->
                        SiteGoldenRecordRelationDto(toSiteRelationType(relation.relationType), relation.sourceBpn, relation.targetBpn)
                    },
                    updatedAt = entity.siteUpdatedAt ?: Instant.EPOCH
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
            states = toStateDtos(entity.states, BusinessPartnerType.ADDRESS),
            goldenRecordRelations = entity.addressGoldenRecordRelations.map { AddressGoldenRecordRelationDto(toAddressRelationType(it.relationType), it.sourceBpn, it.targetBpn) },
            updatedAt = entity.addressUpdatedAt ?: Instant.EPOCH
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


    fun toScriptVariantDto(entity: BusinessPartnerScriptVariantDb): BusinessPartnerScriptVariantDto{
        return BusinessPartnerScriptVariantDto(
            scriptCode = entity.scriptCode,
            nameParts = entity.nameParts,
            legalEntity = toLegalEntityScriptVariantDto(entity),
            site = toSiteScriptVariantDto(entity),
            address = toAddressScriptVariantDto(entity)
        )
    }

    private fun toLegalEntityScriptVariantDto(entity: BusinessPartnerScriptVariantDb): LegalEntityScriptVariantDto{
        return LegalEntityScriptVariantDto(entity.legalName, entity.shortName)
    }

    private fun toSiteScriptVariantDto(entity: BusinessPartnerScriptVariantDb): SiteScriptVariantDto{
        return SiteScriptVariantDto(entity.siteName)
    }

    private fun toAddressScriptVariantDto(entity: BusinessPartnerScriptVariantDb): AddressScriptVariantDto{
        return AddressScriptVariantDto(
            name = entity.addressName,
            physicalAddress = entity.physicalAddress?.let {  toPhysicalAddressScriptVariantDto(it) } ?: PhysicalAddressScriptVariantDto(),
            alternativeAddress = entity.alternativeAddress?.let { toAlternativeAddressScriptVariantDto(it) })
    }

    private fun toPhysicalAddressScriptVariantDto(entity: PhysicalPostalAddressScriptVariantDb): PhysicalAddressScriptVariantDto{
        return with(entity) {
            PhysicalAddressScriptVariantDto(
                city = city,
                district = district,
                street = street?.toStreetScriptVariantDto() ?: StreetScriptVariantDto(),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
    }

    private fun toAlternativeAddressScriptVariantDto(entity: AlternativePostalAddressScriptVariantDb): AlternativeAddressScriptVariantDto{
        return with(entity){
            AlternativeAddressScriptVariantDto(
                city = city
            )
        }
    }

    fun toScriptVariantDb(businessPartner: BusinessPartnerDb, dto: BusinessPartnerScriptVariantDto): BusinessPartnerScriptVariantDb{
        return with(dto){
            BusinessPartnerScriptVariantDb(
                scriptCode = dto.scriptCode,
                businessPartner = businessPartner,
                nameParts = nameParts.toMutableList(),
                shortName = legalEntity.shortName,
                legalName = legalEntity.legalName,
                siteName = site.name,
                addressName = address.name,
                physicalAddress = toPhysicalAddressScriptVariantDb(address.physicalAddress),
                alternativeAddress = toAlternativeAddressScriptVariantDb(address.alternativeAddress))
        }
    }

    private fun toPhysicalAddressScriptVariantDb(dto: PhysicalAddressScriptVariantDto): PhysicalPostalAddressScriptVariantDb{
        return with(dto){
            PhysicalPostalAddressScriptVariantDb(
                city = city,
                district = district,
                street = toStreetScriptVariant(street),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
    }

    private fun toAlternativeAddressScriptVariantDb(dto: AlternativeAddressScriptVariantDto?): AlternativePostalAddressScriptVariantDb?{
        return dto?.city?.let { AlternativePostalAddressScriptVariantDb(city = it) }
    }

    private fun toStreetScriptVariant(dto: StreetScriptVariantDto): StreetScriptVariantDb{
        return with(dto){
            StreetScriptVariantDb(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
    }

    private fun StreetScriptVariantDb.toStreetScriptVariantDto(): StreetScriptVariantDto{
        return StreetScriptVariantDto(
            name = name,
            direction = direction,
            namePrefix = namePrefix,
            additionalNamePrefix = additionalNamePrefix,
            nameSuffix = nameSuffix,
            additionalNameSuffix = additionalNameSuffix
        )
    }

    private fun toIdentifierDto(entity: IdentifierDb) =
        BusinessPartnerIdentifierDto(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toStateDtos(entities: Collection<StateDb>, businessPartnerType: BusinessPartnerType) =
        entities.filter { it.businessPartnerTyp == businessPartnerType }.map(::toStateDto)

    private fun toStateDto(entity: StateDb) =
        BusinessPartnerStateDto(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo)

    private fun toLeRelationType(type: LegalEntityGoldenRecordRelationType) =
        when (type) {
            LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationTypeDto.IsAlternativeHeadquarterFor
            LegalEntityGoldenRecordRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationTypeDto.IsManagedBy
            LegalEntityGoldenRecordRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationTypeDto.IsOwnedBy
            LegalEntityGoldenRecordRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationTypeDto.IsReplacedBy
        }

    private fun toSiteRelationType(type: SiteGoldenRecordRelationType) =
        when (type) {
            SiteGoldenRecordRelationType.IsReplacedBy -> SiteGoldenRecordRelationTypeDto.IsReplacedBy
        }

    private fun toAddressRelationType(type: AddressGoldenRecordRelationType) =
        when (type) {
            AddressGoldenRecordRelationType.IsReplacedBy -> AddressGoldenRecordRelationTypeDto.IsReplacedBy
        }
}
