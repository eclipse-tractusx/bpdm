/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.raw.participation.SharingMemberRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.AddressUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BusinessStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordPassThroughRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordTaskRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRelationTaskRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.LegalEntityUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.NamePartRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertHeaderRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressRelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAlternativeAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedAlternativeAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedGeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedPhysicalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedPhysicalAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedStreetRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UncategorizedStreetScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressStateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AlternativePostalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LogisticAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteScriptVariantRaw
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress
import org.eclipse.tractusx.orchestrator.api.model.AlternativeAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.BusinessState
import org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria
import org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate
import org.eclipse.tractusx.orchestrator.api.model.Identifier
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.model.NamePartType
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress
import org.eclipse.tractusx.orchestrator.api.model.PhysicalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddress
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressWithScriptVariants
import org.eclipse.tractusx.orchestrator.api.model.Site
import org.eclipse.tractusx.orchestrator.api.model.Street
import org.eclipse.tractusx.orchestrator.api.model.StreetScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.pool.model.Street as PoolStreet
import org.eclipse.tractusx.bpdm.pool.model.StreetScriptVariant as PoolStreetScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AlternativeAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PhysicalAddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.PostalAddressScriptVariantRaw
/**
 * Translates the golden record tasks, golden record relation tasks and sharing member records the Orchestrator hands to
 * the Pool into the Pool's raw model.
 */
@Component
class OrchestratorInboundMapper {

    /**
     * Returns the raw golden record task [task] carries.
     */
    fun toRaw(task: TaskStepReservationEntryDto): GoldenRecordTaskRaw =
        GoldenRecordTaskRaw(
            taskId = task.taskId,
            sharingMemberRecordId = task.recordId,
            goldenRecord = toGoldenRecord(task.businessPartner),
            passThrough = toPassThrough(task.businessPartner)
        )

    /**
     * Returns the raw golden record relation task [task] carries.
     */
    fun toRaw(task: TaskRelationsStepReservationEntryDto): GoldenRecordRelationTaskRaw =
        GoldenRecordRelationTaskRaw(
            taskId = task.taskId,
            relation = toRelation(task.businessPartnerRelations)
        )

    /**
     * Returns the raw sharing member record [record] carries.
     */
    fun toRaw(record: SharingMemberRecord): SharingMemberRecordRaw =
        SharingMemberRecordRaw(
            recordId = record.recordId,
            isGoldenRecordCounted = record.isGoldenRecordCounted
        )

    private fun toRelation(relation: BusinessPartnerRelations): RelationRaw =
        RelationRaw(
            type = toRelationType(relation.relationType),
            sourceBpn = relation.businessPartnerSourceBpn,
            targetBpn = relation.businessPartnerTargetBpn,
            validityPeriods = relation.validityPeriods.map { RelationValidityPeriodRaw(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = relation.reasonCode
        )

    private fun toRelationType(type: OrchestratorRelationType): RelationType =
        when (type) {
            OrchestratorRelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            OrchestratorRelationType.IsManagedBy -> RelationType.IsManagedBy
            OrchestratorRelationType.IsOwnedBy -> RelationType.IsOwnedBy
            OrchestratorRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }

    private fun toGoldenRecord(businessPartner: BusinessPartner): GoldenRecordRaw =
        GoldenRecordRaw(
            legalEntity = toLegalEntity(businessPartner.legalEntity),
            site = businessPartner.site?.let { toSite(it) },
            additionalAddress = businessPartner.additionalAddress?.let { toAdditionalAddress(it) },
            additionalSites = businessPartner.additionalSites.map { AdditionalSiteRaw(toReference(it.bpnReference), it.siteName) }
        )

    private fun toLegalEntity(legalEntity: LegalEntity): LegalEntityUpsertRaw =
        LegalEntityUpsertRaw(
            reference = toReference(legalEntity.bpnReference),
            header = LegalEntityUpsertHeaderRaw(
                legalName = legalEntity.legalName,
                legalShortName = legalEntity.legalShortName,
                legalForm = legalEntity.legalForm,
                identifiers = legalEntity.identifiers.map { LegalEntityIdentifierRaw(it.value, it.type, it.issuingBody) },
                states = legalEntity.states.map { toState(it) },
                confidenceCriteria = toConfidenceRequest(legalEntity.confidenceCriteria),
                isDataSpaceParticipant = legalEntity.isParticipantData,
                ownershipUltimate = legalEntity.ownershipUltimate,
                scriptVariants = legalEntity.scriptVariants.map { LegalEntityScriptVariantRaw(it.scriptCode, it.legalName, it.legalShortName) }
            ),
            legalAddress = AddressUpsertRaw(
                reference = toReference(legalEntity.legalAddress.bpnReference),
                content = toContentRequest(
                    legalEntity.legalAddress,
                    legalEntity.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.legalAddress) }
                )
            ),
            intent = toIntent(legalEntity.hasChanged)
        )

    private fun toSite(site: Site): SiteUpsertRaw {
        val reference = toReference(site.bpnReference)
        val header = SiteUpsertHeaderRaw(
            name = site.siteName,
            states = site.states.map { toState(it) },
            confidenceCriteria = toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = site.scriptVariants.map { SiteScriptVariantRaw(it.scriptCode, it.siteName) }
        )
        val mainAddressScriptVariants = site.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.mainAddress) }
        val intent = toIntent(site.hasChanged)

        return when (val mainAddress = site.siteMainAddress) {
            null -> SiteUpsertRaw.WithLegalAddressAsMain(
                reference = reference,
                header = header,
                intent = intent,
                mainAddressScriptVariants = mainAddressScriptVariants.map { toScriptVariant(it) }
            )
            else -> SiteUpsertRaw.WithOwnMainAddress(
                reference = reference,
                header = header,
                intent = intent,
                mainAddress = AddressUpsertRaw(toReference(mainAddress.bpnReference), toContentRequest(mainAddress, mainAddressScriptVariants))
            )
        }
    }

    private fun toAdditionalAddress(address: PostalAddressWithScriptVariants): AddressUpsertRaw =
        AddressUpsertRaw(toReference(address.bpnReference), toContentRequest(address))

    private fun toContentRequest(address: PostalAddressWithScriptVariants): LogisticAddressRaw =
        toContentRequest(address.postalProperties, address.scriptVariants)

    private fun toContentRequest(address: PostalAddress, scriptVariants: List<PostalAddressScriptVariantWithScriptCode>): LogisticAddressRaw =
        LogisticAddressRaw(
            name = address.addressName,
            states = address.states.map { toStateRequest(it) },
            identifiers = address.identifiers.map { toIdentifierRequest(it) },
            physicalPostalAddress = toPhysicalRequest(address.physicalAddress),
            alternativePostalAddress = address.alternativeAddress?.let { toAlternativeRequest(it) },
            confidenceCriteria = toConfidenceRequest(address.confidenceCriteria),
            scriptVariants = scriptVariants.map { toScriptVariant(it) }
        )

    private fun toConfidenceRequest(confidence: ConfidenceCriteria): ConfidenceCriteriaRaw =
        ConfidenceCriteriaRaw(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt
        )

    private fun toPhysicalRequest(physical: PhysicalAddress): PhysicalPostalAddressRaw =
        PhysicalPostalAddressRaw(
            geographicCoordinates = toGeoRequest(physical.geographicCoordinates),
            country = physical.country,
            administrativeAreaLevel1 = physical.administrativeAreaLevel1,
            administrativeAreaLevel2 = physical.administrativeAreaLevel2,
            administrativeAreaLevel3 = physical.administrativeAreaLevel3,
            postalCode = physical.postalCode,
            city = physical.city,
            district = physical.district,
            street = toStreet(physical.street),
            companyPostalCode = physical.companyPostalCode,
            industrialZone = physical.industrialZone,
            building = physical.building,
            floor = physical.floor,
            door = physical.door,
            taxJurisdictionCode = physical.taxJurisdictionCode
        )

    private fun toAlternativeRequest(alternative: AlternativeAddress): AlternativePostalAddressRaw =
        AlternativePostalAddressRaw(
            geographicCoordinates = toGeoRequest(alternative.geographicCoordinates),
            country = alternative.country,
            administrativeAreaLevel1 = alternative.administrativeAreaLevel1,
            postalCode = alternative.postalCode,
            city = alternative.city,
            deliveryServiceType = alternative.deliveryServiceType,
            deliveryServiceQualifier = alternative.deliveryServiceQualifier,
            deliveryServiceNumber = alternative.deliveryServiceNumber
        )

    private fun toStateRequest(state: BusinessState): AddressStateRaw =
        AddressStateRaw(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toIdentifierRequest(identifier: Identifier): AddressIdentifierRaw =
        AddressIdentifierRaw(value = identifier.value, type = identifier.type)

    private fun toGeoRequest(geo: GeoCoordinate): GeoCoordinateRaw =
        GeoCoordinateRaw(longitude = geo.longitude, latitude = geo.latitude, altitude = geo.altitude)

    private fun toScriptVariant(variant: PostalAddressScriptVariantWithScriptCode): AddressScriptVariantRaw =
        AddressScriptVariantRaw(
            scriptCode = variant.scriptCode,
            address = toPostalScriptVariant(variant.postalProperties)
        )

    private fun toPostalScriptVariant(variant: PostalAddressScriptVariant): PostalAddressScriptVariantRaw =
        PostalAddressScriptVariantRaw(
            addressName = variant.addressName,
            physicalAddress = toPhysicalScriptVariant(variant.physicalAddress),
            alternativeAddress = variant.alternativeAddress?.let { toAlternativeScriptVariant(it) }
        )

    private fun toPhysicalScriptVariant(variant: PhysicalAddressScriptVariant): PhysicalAddressScriptVariantRaw =
        PhysicalAddressScriptVariantRaw(
            city = variant.city,
            district = variant.district,
            street = toScriptVariantStreet(variant.street),
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door
        )

    private fun toAlternativeScriptVariant(variant: AlternativeAddressScriptVariant): AlternativeAddressScriptVariantRaw =
        AlternativeAddressScriptVariantRaw(
            city = variant.city
        )

    private fun toScriptVariantStreet(street: StreetScriptVariant): PoolStreetScriptVariant =
        PoolStreetScriptVariant(
            name = street.name,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun toStreet(street: Street): PoolStreet =
        PoolStreet(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun toIntent(hasChanged: Boolean?): UpsertIntent =
        if (hasChanged == false) UpsertIntent.WriteOnlyIfAbsent else UpsertIntent.AlwaysWrite

    private fun toReference(reference: BpnReference): BpnReferenceRaw =
        BpnReferenceRaw(
            value = reference.referenceValue,
            kind = reference.referenceType?.let {
                when (it) {
                    BpnReferenceType.Bpn -> BpnReferenceKind.Bpn
                    BpnReferenceType.BpnRequestIdentifier -> BpnReferenceKind.RequestIdentifier
                }
            }
        )

    private fun toState(state: BusinessState): BusinessStateRaw =
        BusinessStateRaw(validFrom = state.validFrom, validTo = state.validTo, type = state.type)

    private fun toPassThrough(businessPartner: BusinessPartner): GoldenRecordPassThroughRaw =
        GoldenRecordPassThroughRaw(
            nameParts = businessPartner.nameParts.map { NamePartRaw(it.name, toNamePartKind(it.type)) },
            owningCompany = businessPartner.owningCompany,
            uncategorized = with(businessPartner.uncategorized) {
                UncategorizedRaw(
                    nameParts = nameParts,
                    identifiers = identifiers.map { toIdentifier(it) },
                    states = states.map { toState(it) },
                    address = address?.let { toUncategorizedAddress(it) }
                )
            }
        )

    private fun toNamePartKind(type: NamePartType): NamePartKind =
        when (type) {
            NamePartType.LegalName -> NamePartKind.LegalName
            NamePartType.ShortName -> NamePartKind.ShortName
            NamePartType.LegalForm -> NamePartKind.LegalForm
            NamePartType.SiteName -> NamePartKind.SiteName
            NamePartType.AddressName -> NamePartKind.AddressName
        }

    private fun toIdentifier(identifier: Identifier): UncategorizedIdentifierRaw =
        UncategorizedIdentifierRaw(identifier.value, identifier.type, identifier.issuingBody)

    private fun toUncategorizedAddress(address: PostalAddressWithScriptVariants): UncategorizedAddressRaw =
        with(address.postalProperties) {
            UncategorizedAddressRaw(
                bpnReferenceValue = bpnReference.referenceValue,
                bpnReferenceDesiredBpn = bpnReference.desiredBpn,
                bpnReferenceKind = toReference(bpnReference).kind,
                addressName = addressName,
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                confidenceCriteria = with(confidenceCriteria) {
                    UncategorizedConfidenceCriteriaRaw(
                        sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel
                    )
                },
                physicalAddress = toUncategorizedPhysicalAddress(physicalAddress),
                alternativeAddress = alternativeAddress?.let { toUncategorizedAlternativeAddress(it) },
                hasChanged = hasChanged,
                relations = goldenRecordRelations.map {
                    val type = when (it.relationType) {
                        AddressGoldenRecordRelationType.IsReplacedBy -> UncategorizedAddressRelationKind.IsReplacedBy
                    }
                    UncategorizedAddressRelationRaw(type, it.sourceBpn, it.targetBpn)
                },
                updatedAt = updatedAt,
                scriptVariants = address.scriptVariants.map { toUncategorizedScriptVariant(it) }
            )
        }

    private fun toUncategorizedPhysicalAddress(address: PhysicalAddress): UncategorizedPhysicalAddressRaw =
        with(address) {
            UncategorizedPhysicalAddressRaw(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = with(street) {
                    UncategorizedStreetRaw(
                        name, houseNumber, houseNumberSupplement, milestone, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix
                    )
                },
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toUncategorizedAlternativeAddress(address: AlternativeAddress): UncategorizedAlternativeAddressRaw =
        with(address) {
            UncategorizedAlternativeAddressRaw(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun toGeoCoordinate(coordinate: GeoCoordinate): UncategorizedGeoCoordinateRaw =
        UncategorizedGeoCoordinateRaw(coordinate.longitude, coordinate.latitude, coordinate.altitude)

    private fun toUncategorizedScriptVariant(variant: PostalAddressScriptVariantWithScriptCode): UncategorizedAddressScriptVariantRaw =
        with(variant.postalProperties) {
            UncategorizedAddressScriptVariantRaw(
                scriptCode = variant.scriptCode,
                addressName = addressName,
                physicalAddress = with(physicalAddress) {
                    UncategorizedPhysicalAddressScriptVariantRaw(
                        city = city,
                        district = district,
                        street = with(street) {
                            UncategorizedStreetScriptVariantRaw(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                        },
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                },
                alternativeAddress = alternativeAddress?.let { UncategorizedAlternativeAddressScriptVariantRaw(it.city) }
            )
        }
}
