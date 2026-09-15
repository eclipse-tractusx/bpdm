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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Component
import java.time.ZoneOffset

/**
 * Maps the business partners a golden record task has written to the golden record the task reports back.
 *
 * Each partner is rendered through the mapper the Pool's own API reports it with, so a task and a read of the same
 * record state the same content.
 */
@Component
class GoldenRecordTaskResultMapper(
    private val legalEntityResponseMapper: LegalEntityResponseMapper,
    private val siteResponseMapper: SiteResponseMapper,
    private val addressResponseMapper: AddressResponseMapper
) {

    /**
     * Reports the records an upsert left behind as the task's business partner result, stated on top of the partner
     * the task carried.
     */
    fun toTaskResult(stated: BusinessPartner, written: GoldenRecordUpsertResult): BusinessPartner {
        val legalEntityResult = toTaskResult(written.legalEntity)
        val siteResult = written.site?.let { toTaskResult(it) }

        // A site whose main address is the legal address is one record standing in both roles: it is reported on the
        // legal entity, carrying the site's verdict on whether it changed, and the site states no main address of its own.
        val sharedAddress = siteResult?.siteMainAddress?.takeIf { written.isSiteMainAddressTheLegalAddress }

        return stated.copy(
            legalEntity = sharedAddress?.let { legalEntityResult.copy(legalAddress = it) } ?: legalEntityResult,
            site = sharedAddress?.let { siteResult.copy(siteMainAddress = null) } ?: siteResult,
            additionalAddress = written.additionalAddress?.let { toTaskResult(it) },
            additionalSites = written.membershipSites.map { AdditionalSite(BpnReference(it.bpn, null, BpnReferenceType.Bpn), it.name) }
        )
    }

    private fun toTaskResult(written: UpsertResult<LegalEntityDb>): LegalEntity =
        toTaskResult(legalEntityResponseMapper.toLegalEntityWithLegalAddress(written.value), written.hasChanged)

    private fun toTaskResult(written: UpsertResult<SiteDb>): Site =
        siteResponseMapper.toSiteWithMainAddress(written.value)
            .let { toTaskResult(it.site, it.mainAddress, written.hasChanged) }

    private fun toTaskResult(written: UpsertResult<LogisticAddressDb>): PostalAddressWithScriptVariants =
        addressResponseMapper.toAddress(written.value)
            .let { toTaskResult(it.address, it.scriptVariants, written.hasChanged) }

    private fun toTaskResult(legalEntity: LegalEntityWithLegalAddressVerboseDto, hasChanged: Boolean?): LegalEntity{
        return toTaskResult(legalEntity.header, legalEntity.legalAddress, hasChanged, legalEntity.scriptVariants)
    }

    // The site states its main address even where that address is the legal address: which of the two partners
    // reports it is settled once, above, for the whole business partner.
    private fun toTaskResult(site: SiteVerboseDto, siteMainAddress: LogisticAddressInvariantVerboseDto, hasChanged: Boolean?): Site{
        return with(site){
            Site(
                bpnReference = BpnReference(bpns, null, BpnReferenceType.Bpn),
                siteName = name,
                states = states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                hasChanged = hasChanged,
                siteMainAddress = toTaskResult(siteMainAddress, hasChanged),
                scriptVariants = scriptVariants.map { toTaskResult(it) },
                goldenRecordRelations = relations
                    .distinctBy { Triple(it.type, it.businessPartnerSourceBpns, it.businessPartnerTargetBpns) }
                    .map { toTaskResult(it) },
                updatedAt = updatedAt
            )
        }
    }

    private fun toTaskResult(
        postalAddress: LogisticAddressInvariantVerboseDto,
        scriptVariants: List<LogisticAddressScriptVariantDto>,
        hasChanged: Boolean?
    ): PostalAddressWithScriptVariants{
        return PostalAddressWithScriptVariants(toTaskResult(postalAddress, hasChanged), scriptVariants.map { toTaskResult(it) })
    }

    private fun toTaskResult(
        legalEntity: LegalEntityHeaderVerboseDto,
        legalAddress: LogisticAddressInvariantVerboseDto,
        hasChanged: Boolean?,
        scriptVariants: List<LegalEntityScriptVariantDto>
    ): LegalEntity{
        return with(legalEntity){
            LegalEntity(
                bpnReference = BpnReference(bpnl, null, BpnReferenceType.Bpn),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map { Identifier(it.value, it.type, it.issuingBody) },
                states = states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                isParticipantData = legalEntity.isParticipantData,
                hasChanged = hasChanged,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl,
                legalAddress = toTaskResult(legalAddress, hasChanged),
                scriptVariants = scriptVariants.map { toTaskResult(it) },
                goldenRecordRelations = legalEntity.relations
                    .distinctBy { Triple(it.type, it.businessPartnerSourceBpnl, it.businessPartnerTargetBpnl) }
                    .map { toTaskResult(it) },
                updatedAt = updatedAt
            )
        }
    }

    private fun toTaskResult(confidenceCriteria: ConfidenceCriteriaDto): ConfidenceCriteria{
        return with(confidenceCriteria){
            ConfidenceCriteria(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                nextConfidenceCheckAt = nextConfidenceCheckAt.toInstant(ZoneOffset.UTC),
                confidenceLevel = confidenceLevel
            )
        }
    }

    private fun toTaskResult(postalAddress: LogisticAddressInvariantVerboseDto, hasChanged: Boolean?): PostalAddress{
        return with(postalAddress){
            PostalAddress(
                bpnReference = BpnReference(bpna, null, BpnReferenceType.Bpn),
                addressName = name,
                identifiers = identifiers.map { Identifier(it.value, it.type, null) },
                states =  states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                physicalAddress = toTaskResult(physicalPostalAddress),
                alternativeAddress =  alternativePostalAddress?.let { toTaskResult(it) },
                hasChanged = hasChanged,
                goldenRecordRelations = relations
                    .distinctBy { Triple(it.type, it.businessPartnerSourceBpna, it.businessPartnerTargetBpna) }
                    .map { toTaskResult(it) },
                updatedAt = updatedAt
            )
        }
    }

    private fun toTaskResult(physicalAddress: PhysicalPostalAddressVerboseDto): PhysicalAddress{
        return with(physicalAddress){
            PhysicalAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = physicalAddress.country.alpha2,
                administrativeAreaLevel1 = physicalAddress.administrativeAreaLevel1,
                administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let { toTaskResult(it) } ?: Street.empty,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode

            )
        }
    }

    private fun toTaskResult(alternativeAddress: AlternativePostalAddressVerboseDto): AlternativeAddress{
        return with(alternativeAddress){
            AlternativeAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = country.alpha2,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
    }


    private fun toTaskResult(street: StreetDto): Street{
        return with(street){
            Street(
                name = name,
                houseNumber = houseNumber,
                houseNumberSupplement = houseNumberSupplement,
                milestone = milestone,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
    }

    private fun toTaskResult(legalEntityScriptVariant: LegalEntityScriptVariantDto): LegalEntityScriptVariant{
        return with(legalEntityScriptVariant){
            LegalEntityScriptVariant(
                scriptCode = scriptCode,
                legalName = legalName,
                legalShortName = shortName,
                legalAddress = toTaskResult(legalAddress)
            )
        }
    }

    private fun toTaskResult(siteScriptVariant: SiteScriptVariantDto): SiteScriptVariant{
        return with(siteScriptVariant){
            SiteScriptVariant(
                scriptCode = scriptCode,
                siteName = name,
                mainAddress = toTaskResult(mainAddress)
            )
        }
    }

    private fun toTaskResult(addressScriptVariant: LogisticAddressScriptVariantDto): PostalAddressScriptVariantWithScriptCode{
        return with(addressScriptVariant){
            PostalAddressScriptVariantWithScriptCode(addressScriptVariant.scriptCode, toTaskResult(address))
        }
    }

    private fun toTaskResult(addressScriptVariant: PostalAddressScriptVariantDto): PostalAddressScriptVariant{
        return with(addressScriptVariant){
            PostalAddressScriptVariant(
                addressName = addressName,
                physicalAddress = toTaskResult(physicalAddress),
                alternativeAddress = alternativeAddress?.let { toTaskResult(it) })
        }
    }

    private fun toTaskResult(physicalAddress: PhysicalAddressScriptVariantDto): PhysicalAddressScriptVariant{
        return with(physicalAddress){
            PhysicalAddressScriptVariant(
                city = city,
                district = district,
                street = street?.let { toTaskResult(it) } ?: StreetScriptVariant.empty,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door)
        }
    }

    private fun toTaskResult(alternativeAddressScriptVariant: AlternativeAddressScriptVariantDto): AlternativeAddressScriptVariant{
        return with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariant(
                city = city
            )
        }
    }

    private fun toTaskResult(street: StreetScriptVariantDto): StreetScriptVariant{
        return with(street){
            StreetScriptVariant(
                name = name,
                direction = direction,
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix
            )
        }
    }

    private fun toTaskResult(relation: RelationVerboseDto): LegalEntityGoldenRecordRelation{
        return LegalEntityGoldenRecordRelation(
            relationType = when (relation.type) {
                LegalEntityRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
                LegalEntityRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
                LegalEntityRelationType.IsOwnedBy ->LegalEntityGoldenRecordRelationType.IsOwnedBy
                LegalEntityRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpnl,
            targetBpn = relation.businessPartnerTargetBpnl
        )
    }

    private fun toTaskResult(relation: SiteRelationVerboseDto): SiteGoldenRecordRelation{
        return SiteGoldenRecordRelation(
            relationType = when (relation.type) {
                SiteRelationType.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpns,
            targetBpn = relation.businessPartnerTargetBpns
        )
    }

    private fun toTaskResult(relation: AddressRelationVerboseDto): AddressGoldenRecordRelation{
        return AddressGoldenRecordRelation(
            relationType = when (relation.type) {
                AddressRelationType.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpna,
            targetBpn = relation.businessPartnerTargetBpna
        )
    }
}
