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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordRelation
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.GoldenRecordType
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.AdditionalSite as OutputAdditionalSite

@Service
class OrchestratorMappings {

    fun toOutputUpsertData(dto: BusinessPartner, roles: List<BusinessPartnerRole>, tenantBpnl: String?): OutputUpsertData {
        val addressType = when(dto.type){
            GoldenRecordType.LegalEntity -> AddressType.LegalAddress
            GoldenRecordType.Site -> if(dto.site!!.siteMainIsLegalAddress) AddressType.LegalAndSiteMainAddress else AddressType.SiteMainAddress
            GoldenRecordType.Address -> AddressType.AdditionalAddress
            null -> throw BpdmNullMappingException(BusinessPartner::class, BusinessPartnerDb::class, BusinessPartner::type)
        }

        val postalAddress = when(addressType){
            AddressType.LegalAndSiteMainAddress -> dto.legalEntity.legalAddress
            AddressType.LegalAddress -> dto.legalEntity.legalAddress
            AddressType.SiteMainAddress -> dto.site!!.siteMainAddress!!
            AddressType.AdditionalAddress -> dto.additionalAddress!!
        }

        return with(dto) {
            OutputUpsertData(
                nameParts = uncategorized.nameParts.toMutableList(),
                shortName = legalEntity.legalShortName,
                identifiers = uncategorized.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(postalAddress.identifiers.mapNotNull { toIdentifier(it, BusinessPartnerType.ADDRESS) }),
                legalName = legalEntity.legalName,
                siteName = site?.siteName,
                addressName = postalAddress.addressName,
                legalForm = legalEntity.legalForm,
                states = uncategorized.states.mapNotNull { toState(it, BusinessPartnerType.GENERIC) }
                    .plus(legalEntity.states.mapNotNull{ toState(it, BusinessPartnerType.LEGAL_ENTITY) })
                    .plus(site?.states?.mapNotNull{ toState(it, BusinessPartnerType.SITE) } ?: emptyList())
                    .plus(postalAddress.states.mapNotNull { toState(it, BusinessPartnerType.ADDRESS) }),
                roles = roles.toSortedSet(),
                addressType = addressType,
                physicalPostalAddress = toPhysicalPostalAddress(postalAddress.physicalAddress),
                alternativePostalAddress = postalAddress.alternativeAddress?.let(::toAlternativePostalAddress),
                legalEntityBpn = legalEntity.bpnReference.referenceValue!!,
                siteBpn = site?.bpnReference?.referenceValue,
                addressBpn = postalAddress.bpnReference.referenceValue!!,
                legalEntityConfidence = toConfidenceCriteria(legalEntity.confidenceCriteria),
                siteConfidence = site?.let { toConfidenceCriteria(it.confidenceCriteria) },
                addressConfidence = toConfidenceCriteria(postalAddress.confidenceCriteria),
                legalEntityUpdatedAt = legalEntity.updatedAt,
                siteUpdatedAt = site?.updatedAt,
                addressUpdatedAt = postalAddress.updatedAt,
                isOwnCompanyData = if (tenantBpnl != null && owningCompany != null) tenantBpnl == owningCompany else false,
                scriptVariants = toScriptVariants(addressType, dto),
                legalEntityGoldenRecordRelations = legalEntity.goldenRecordRelations.map(::toRelation),
                siteGoldenRecordRelations = site?.goldenRecordRelations.orEmpty().map(::toRelation),
                addressGoldenRecordRelations = toAddressGoldenRecordRelations(addressType, dto).map(::toRelation),
                ownershipUltimate = legalEntity.ownershipUltimate,
                ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
                additionalSites = toOutputAdditionalSites(dto)
            )
        }
    }

    private fun toOutputAdditionalSites(dto: BusinessPartner): List<OutputAdditionalSite> =
        dto.additionalSites.mapNotNull { additionalSite ->
            additionalSite.bpnReference.referenceValue?.let { OutputAdditionalSite(siteBpn = it, name = additionalSite.siteName) }
        }

    private fun toRelation(relation: LegalEntityGoldenRecordRelation) =
        GoldenRecordRelation(LegalEntityGoldenRecordRelationType.valueOf(relation.relationType.name), relation.sourceBpn, relation.targetBpn)

    private fun toRelation(relation: SiteGoldenRecordRelation) =
        GoldenRecordRelation(SiteGoldenRecordRelationType.valueOf(relation.relationType.name), relation.sourceBpn, relation.targetBpn)

    private fun toRelation(relation: AddressGoldenRecordRelation) =
        GoldenRecordRelation(AddressGoldenRecordRelationType.valueOf(relation.relationType.name), relation.sourceBpn, relation.targetBpn)

    private fun toAddressGoldenRecordRelations(addressType: AddressType, dto: BusinessPartner): List<AddressGoldenRecordRelation> =
        when (addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> dto.legalEntity.legalAddress.goldenRecordRelations
            AddressType.SiteMainAddress -> dto.site!!.siteMainAddress!!.goldenRecordRelations
            AddressType.AdditionalAddress -> dto.additionalAddress!!.postalProperties.goldenRecordRelations
        }

    private fun toScriptVariants(addressType: AddressType, dto: BusinessPartner): List<BusinessPartnerScriptVariantDto>{
        val addressScriptVariants = when(addressType){
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress  -> dto.legalEntity.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.legalAddress) }
            AddressType.SiteMainAddress -> dto.site!!.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.mainAddress) }
            AddressType.AdditionalAddress -> dto.additionalAddress!!.scriptVariants.map { it }
        }

        val addressVariantByCode = addressScriptVariants.associateBy { it.scriptCode }
        val legalEntityVariantsByCode = dto.legalEntity.scriptVariants.associateBy { it.scriptCode }
        val siteScriptVariantsByCode = dto.site?.scriptVariants?.associateBy { it.scriptCode } ?: emptyMap()

        val allScriptCodes = addressVariantByCode.keys.plus(legalEntityVariantsByCode.keys).plus(siteScriptVariantsByCode.keys)

        return allScriptCodes.map { scriptCode ->
            val legalEntityVariant = toLegalEntityScriptVariant(legalEntityVariantsByCode[scriptCode]) ?: LegalEntityScriptVariantDto()
            val siteScriptVariant = toSiteScriptVariant(siteScriptVariantsByCode[scriptCode]) ?: SiteScriptVariantDto()
            val addressScriptVariant = addressVariantByCode[scriptCode]?.let { toAddressScriptVariant(it.postalProperties)  }?: AddressScriptVariantDto()

            BusinessPartnerScriptVariantDto(scriptCode, legalEntity = legalEntityVariant, site = siteScriptVariant, address = addressScriptVariant)
        }
    }

    private fun toLegalEntityScriptVariant(dto: LegalEntityScriptVariant?) =
        dto?.let { LegalEntityScriptVariantDto(it.legalName, it.legalShortName) }

    private fun toSiteScriptVariant(dto: SiteScriptVariant?) =
        dto?.let { SiteScriptVariantDto(it.siteName) }

    private fun toAddressScriptVariant(dto: PostalAddressScriptVariant) =
        AddressScriptVariantDto(
            name = dto.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(dto.physicalAddress),
            alternativeAddress = toAlternativeAddressScriptVariant(dto.alternativeAddress)
        )

    private fun toPhysicalAddressScriptVariant(dto: PhysicalAddressScriptVariant) =
        with(dto){
            PhysicalAddressScriptVariantDto(
                city = city,
                district = district,
                street = toScriptVariantStreet(street),
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }

    private fun toAlternativeAddressScriptVariant(dto: AlternativeAddressScriptVariant?) =
        dto?.let { with(it){
            AlternativeAddressScriptVariantDto(
                city = city
            )
        }
        }

    private fun toIdentifier(dto: Identifier, businessPartnerType: BusinessPartnerType) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                org.eclipse.tractusx.bpdm.gate.model.upsert.output.Identifier(
                    type = type,
                    value = value,
                    issuingBody = dto.issuingBody,
                    businessPartnerType = businessPartnerType
                )
            }
        }

    private fun toState(dto: BusinessState, businessPartnerType: BusinessPartnerType) =
        dto.type?.let {
            State(
            type = it,
            validFrom = dto.validFrom?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
            validTo = dto.validTo?.atZone(ZoneOffset.UTC)?.toLocalDateTime(),
                businessPartnerType = businessPartnerType
            )
        }

    private fun toPhysicalPostalAddress(dto: PhysicalAddress) =
        PhysicalPostalAddress(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = dto.country ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                PhysicalAddress::country
            ),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city ?: throw BpdmNullMappingException(BusinessPartner::class, OutputUpsertData::class, PhysicalAddress::city),
            district = dto.district,
            street = toStreet(dto.street),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door,
            taxJurisdictionCode = dto.taxJurisdictionCode
        )

    private fun toAlternativePostalAddress(dto: AlternativeAddress) =
        org.eclipse.tractusx.bpdm.gate.model.upsert.output.AlternativeAddress(
            geographicCoordinates = toGeographicCoordinate(dto.geographicCoordinates),
            country = dto.country ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                AlternativeAddress::country
            ),
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city ?: throw BpdmNullMappingException(BusinessPartner::class, OutputUpsertData::class, AlternativeAddress::city),
            deliveryServiceType = dto.deliveryServiceType ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                AlternativeAddress::deliveryServiceType
            ),
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber ?: throw BpdmNullMappingException(
                BusinessPartner::class,
                OutputUpsertData::class,
                AlternativeAddress::deliveryServiceNumber
            )
        )

    private fun toStreet(dto: Street) =
        org.eclipse.tractusx.bpdm.gate.model.value.Street(
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

    private fun toScriptVariantStreet(dto: StreetScriptVariant) =
        StreetScriptVariantDto(
            name = dto.name,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )

    private fun toGeographicCoordinate(dto: GeoCoordinate) =
        dto.latitude?.let { lat ->
            dto.longitude?.let {  lon ->
                org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate(latitude = lat, longitude = lon, altitude = dto.altitude)
            }
        }

    private fun toConfidenceCriteria(dto: ConfidenceCriteria) =
        org.eclipse.tractusx.bpdm.gate.model.upsert.output.ConfidenceCriteria(
            sharedByOwner = dto.sharedByOwner!!,
            checkedByExternalDataSource = dto.checkedByExternalDataSource!!,
            numberOfSharingMembers = dto.numberOfSharingMembers!!,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt!!.atZone(ZoneOffset.UTC).toLocalDateTime(),
            confidenceLevel = dto.confidenceLevel!!
        )
}
