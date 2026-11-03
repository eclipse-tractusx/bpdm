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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class TaskResolutionMapper {

    fun toTaskResult(legalEntity: LegalEntityWithLegalAddressVerboseV7Dto, hasChanged: Boolean?): LegalEntity{
        return toTaskResult(legalEntity.header, legalEntity.legalAddress, hasChanged, legalEntity.scriptVariants)
    }

    fun toTaskResult(legalEntity: LegalEntityHeaderVerboseV7Dto, legalAddress: LogisticAddressInvariantVerboseV7Dto, hasChanged: Boolean?, scriptVariants: List<LegalEntityScriptVariantV7Dto>): LegalEntity{
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

    fun toTaskResult(site: SiteVerboseV7Dto, siteMainAddress: LogisticAddressInvariantVerboseV7Dto, hasChanged: Boolean?): Site{
        return with(site){
            Site(
                bpnReference = BpnReference(bpns, null, BpnReferenceType.Bpn),
                siteName = name,
                states = states.map { BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type) },
                confidenceCriteria = toTaskResult(confidenceCriteria),
                hasChanged = hasChanged,
                //Normally this should be null if the site main address is also the legal address
                //However, due to synchronization issues we will pass the address here
                // and perform that last step to set this to null later on after we use this site main address to override the legal entities legal address
                siteMainAddress = toTaskResult(siteMainAddress, hasChanged),
                scriptVariants = scriptVariants.map { toTaskResult(it) },
                goldenRecordRelations = relations
                    .distinctBy { Triple(it.type, it.businessPartnerSourceBpns, it.businessPartnerTargetBpns) }
                    .map { toTaskResult(it) },
                updatedAt = updatedAt
            )
        }
    }

    fun toTaskResult(confidenceCriteria: ConfidenceCriteriaV7Dto): ConfidenceCriteria{
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

    fun toTaskResult(postalAddress: LogisticAddressInvariantVerboseV7Dto, scriptVariants: List<LogisticAddressScriptVariantV7Dto>, hasChanged: Boolean?): PostalAddressWithScriptVariants{
        return PostalAddressWithScriptVariants(toTaskResult(postalAddress, hasChanged), scriptVariants.map { toTaskResult(it) })
    }

    fun toTaskResult(postalAddress: LogisticAddressInvariantVerboseV7Dto, hasChanged: Boolean?): PostalAddress{
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

    fun toTaskResult(physicalAddress: PhysicalPostalAddressVerboseV7Dto): PhysicalAddress{
        return with(physicalAddress){
            PhysicalAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = physicalAddress.country,
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

    fun toTaskResult(alternativeAddress: AlternativePostalAddressVerboseV7Dto): AlternativeAddress{
        return with(alternativeAddress){
            AlternativeAddress(
                geographicCoordinates = geographicCoordinates?.let { with(it){ GeoCoordinate(longitude, latitude, altitude) } } ?: GeoCoordinate.empty,
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
    }


    fun toTaskResult(street: StreetV7Dto): Street{
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

    fun toTaskResult(legalEntityScriptVariant: LegalEntityScriptVariantV7Dto): LegalEntityScriptVariant{
        return with(legalEntityScriptVariant){
            LegalEntityScriptVariant(
                scriptCode = scriptCode,
                legalName = legalName,
                legalShortName = shortName,
                legalAddress = toTaskResult(legalAddress)
            )
        }
    }

    fun toTaskResult(siteScriptVariant: SiteScriptVariantV7Dto): SiteScriptVariant{
        return with(siteScriptVariant){
            SiteScriptVariant(
                scriptCode = scriptCode,
                siteName = name,
                mainAddress = toTaskResult(mainAddress)
            )
        }
    }

    fun toTaskResult(addressScriptVariant: LogisticAddressScriptVariantV7Dto): PostalAddressScriptVariantWithScriptCode{
        return with(addressScriptVariant){
            PostalAddressScriptVariantWithScriptCode(addressScriptVariant.scriptCode, toTaskResult(address))
        }
    }

    fun toTaskResult(addressScriptVariant: PostalAddressScriptVariantV7Dto): PostalAddressScriptVariant{
        return with(addressScriptVariant){
            PostalAddressScriptVariant(
                addressName = addressName,
                physicalAddress = toTaskResult(physicalAddress),
                alternativeAddress = alternativeAddress?.let { toTaskResult(it) })
        }
    }

    fun toTaskResult(physicalAddress: PhysicalAddressScriptVariantV7Dto): PhysicalAddressScriptVariant{
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

    fun toTaskResult(alternativeAddressScriptVariant: AlternativeAddressScriptVariantV7Dto): AlternativeAddressScriptVariant{
        return with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariant(
                city = city
            )
        }
    }

    fun toTaskResult(street: StreetScriptVariantV7Dto): StreetScriptVariant{
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

    fun toTaskResult(relation: RelationVerboseV7Dto): LegalEntityGoldenRecordRelation{
        return LegalEntityGoldenRecordRelation(
            relationType = when (relation.type) {
                LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
                LegalEntityRelationTypeV7.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
                LegalEntityRelationTypeV7.IsOwnedBy ->LegalEntityGoldenRecordRelationType.IsOwnedBy
                LegalEntityRelationTypeV7.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpnl,
            targetBpn = relation.businessPartnerTargetBpnl
        )
    }

    fun toTaskResult(relation: SiteRelationVerboseV7Dto): SiteGoldenRecordRelation{
        return SiteGoldenRecordRelation(
            relationType = when (relation.type) {
                SiteRelationTypeV7.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpns,
            targetBpn = relation.businessPartnerTargetBpns
        )
    }

    fun toTaskResult(relation: AddressRelationVerboseV7Dto): AddressGoldenRecordRelation{
        return AddressGoldenRecordRelation(
            relationType = when (relation.type) {
                AddressRelationTypeV7.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.businessPartnerSourceBpna,
            targetBpn = relation.businessPartnerTargetBpna
        )
    }


}