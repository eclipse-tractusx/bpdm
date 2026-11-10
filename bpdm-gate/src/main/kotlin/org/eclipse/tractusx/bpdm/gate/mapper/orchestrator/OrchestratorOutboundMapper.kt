/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw as GateGeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw as GateStreet
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.GoldenRecordCountedUpdateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.TaskReferenceRaw
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IdentifierV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCodeV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariantsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationTypeV7 as OrchestratorRelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordUpdateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsRequestEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateRequestEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariantV7Dto
import org.springframework.stereotype.Component
import java.time.ZoneOffset

/**
 * Maps Gate models to the requests the Orchestrator accepts.
 */
@Component
class OrchestratorOutboundMapper {

    /**
     * Maps [candidates] onto one request creating a golden record task from the sharing member side for each of them.
     */
    fun toCreateRequest(candidates: List<GoldenRecordTaskCandidateParsed>): TaskCreateV7Request =
        TaskCreateV7Request(
            mode = TaskModeV7.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRequestEntryV7Dto(recordId = it.recordId, businessPartner = toBusinessPartner(it)) }
        )

    /**
     * Maps [taskIds] onto a request asking for the result state of each of these tasks.
     */
    fun toResultStateSearchRequest(taskIds: List<String>): TaskResultStateSearchV7Request =
        TaskResultStateSearchV7Request(taskIds)

    private fun toBusinessPartner(candidate: GoldenRecordTaskCandidateParsed): BusinessPartnerV7Dto {
        val input = candidate.input
        val addressType = input.address.addressType
        val postalAddress = toPostalAddress(input)
        val isLegalAddress = addressType == AddressType.LegalAddress || addressType == AddressType.LegalAndSiteMainAddress
        val isSiteMainAddress = addressType == AddressType.SiteMainAddress

        return BusinessPartnerV7Dto(
            nameParts = emptyList(),
            uncategorized = UncategorizedPropertiesV7Dto(
                nameParts = input.nameParts,
                // The Gate stores every input identifier as uncategorized, so none belongs to the legal entity or address.
                identifiers = input.identifiers.map { IdentifierV7Dto(it.value, it.type, it.issuingBody) },
                states = toStates(input, BusinessPartnerType.GENERIC),
                address = postalAddress.takeIf { addressType == null }?.let { toWithScriptVariants(it, input) },
                scriptVariants = input.scriptVariants.map { UncategorizedScriptVariantV7Dto(it.scriptCode, it.nameParts) }
            ),
            owningCompany = candidate.owningCompanyBpnl,
            legalEntity = LegalEntityV7Dto(
                bpnReference = toBpnReference(input.legalEntity.bpn),
                legalName = input.legalEntity.legalName,
                legalForm = input.legalEntity.legalForm,
                legalShortName = input.legalEntity.shortName,
                identifiers = emptyList(),
                states = toStates(input, BusinessPartnerType.LEGAL_ENTITY),
                confidenceCriteria = ConfidenceCriteriaV7Dto.empty,
                isParticipantData = null,
                hasChanged = true,
                ownershipUltimate = input.legalEntity.ownershipUltimate,
                ultimateOwnerBpnl = input.legalEntity.ultimateOwnerBpnl,
                legalAddress = postalAddress.takeIf { isLegalAddress } ?: PostalAddressV7Dto.empty,
                scriptVariants = if (isLegalAddress) input.scriptVariants.map(::toLegalEntityScriptVariant) else emptyList()
            ),
            site = SiteV7Dto(
                bpnReference = toBpnReference(input.site.bpn),
                siteName = input.site.name,
                states = toStates(input, BusinessPartnerType.SITE),
                confidenceCriteria = ConfidenceCriteriaV7Dto.empty,
                hasChanged = true,
                siteMainAddress = postalAddress.takeIf { isSiteMainAddress },
                scriptVariants = if (isSiteMainAddress) input.scriptVariants.map(::toSiteScriptVariant) else emptyList()
            ).takeIf { input.site.bpn != null || input.site.name != null },
            additionalAddress = postalAddress.takeIf { addressType == AddressType.AdditionalAddress }?.let { toWithScriptVariants(it, input) },
            additionalSites = input.additionalSites.map { AdditionalSiteV7Dto(bpnReference = toBpnReference(it.bpn), siteName = it.name) }
        )
    }

    private fun toPostalAddress(input: BusinessPartnerInputContentParsed): PostalAddressV7Dto =
        PostalAddressV7Dto(
            bpnReference = toBpnReference(input.address.bpn),
            addressName = input.address.name,
            identifiers = emptyList(),
            states = toStates(input, BusinessPartnerType.ADDRESS),
            confidenceCriteria = ConfidenceCriteriaV7Dto.empty,
            physicalAddress = input.address.physicalPostalAddress?.let(::toPhysicalAddress) ?: PhysicalAddressV7Dto.empty,
            alternativeAddress = input.address.alternativePostalAddress?.let(::toAlternativeAddress),
            hasChanged = null
        )

    private fun toWithScriptVariants(postalAddress: PostalAddressV7Dto, input: BusinessPartnerInputContentParsed) =
        PostalAddressWithScriptVariantsV7Dto(
            postalAddress,
            input.scriptVariants.map { PostalAddressScriptVariantWithScriptCodeV7Dto(it.scriptCode, toPostalAddressScriptVariant(it)) }
        )

    private fun toPhysicalAddress(address: BusinessPartnerValuesRaw.PhysicalPostalAddress) =
        with(address) {
            PhysicalAddressV7Dto(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let(::toStreet) ?: StreetV7Dto.empty,
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativeAddress(address: BusinessPartnerValuesRaw.AlternativePostalAddress) =
        with(address) {
            AlternativeAddressV7Dto(
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

    private fun toStreet(street: GateStreet) =
        with(street) {
            StreetV7Dto(
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

    private fun toGeoCoordinate(coordinate: GateGeoCoordinate?) =
        coordinate?.let { GeoCoordinateV7Dto(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinateV7Dto.empty

    private fun toStates(input: BusinessPartnerInputContentParsed, businessPartnerType: BusinessPartnerType) =
        input.states.filter { it.businessPartnerType == businessPartnerType }.map {
            BusinessStateV7Dto(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type)
        }

    private fun toBpnReference(bpn: String?) =
        bpn?.let { BpnReferenceV7Dto(referenceValue = it, desiredBpn = null, referenceType = BpnReferenceTypeV7.Bpn) } ?: BpnReferenceV7Dto.empty

    private fun toLegalEntityScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        LegalEntityScriptVariantV7Dto(scriptVariant.scriptCode, scriptVariant.legalName, scriptVariant.shortName, toPostalAddressScriptVariant(scriptVariant))

    private fun toSiteScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        SiteScriptVariantV7Dto(scriptVariant.scriptCode, scriptVariant.siteName ?: "", toPostalAddressScriptVariant(scriptVariant))

    private fun toPostalAddressScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        PostalAddressScriptVariantV7Dto(
            addressName = scriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariantV7Dto(city = it) }
        )

    private fun toPhysicalAddressScriptVariant(address: BusinessPartnerValuesRaw.PhysicalAddressScriptVariant) =
        with(address) {
            PhysicalAddressScriptVariantV7Dto(
                city = city,
                district = district,
                street = with(street) { StreetScriptVariantV7Dto(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix) },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }

    /**
     * Maps [candidates] onto one request creating a relation golden record task from the sharing member side for each
     * of them.
     */
    fun toCreateRequest(candidates: List<RelationTaskCandidateParsed>): TaskCreateRelationsV7Request =
        TaskCreateRelationsV7Request(
            mode = TaskModeV7.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRelationsRequestEntryV7Dto(recordId = it.recordId, businessPartnerRelations = toRelations(it)) }
        )

    private fun toRelations(candidate: RelationTaskCandidateParsed): BusinessPartnerRelationsV7Dto =
        BusinessPartnerRelationsV7Dto(
            relationType = toOrchestratorType(candidate.relationType),
            businessPartnerSourceBpn = candidate.sourceBpn,
            businessPartnerTargetBpn = candidate.targetBpn,
            validityPeriods = candidate.validityPeriods.map { RelationValidityPeriodV7Dto(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = candidate.reasonCode
        )

    private fun toOrchestratorType(relationType: RelationType): OrchestratorRelationType =
        when (relationType) {
            RelationType.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }

    /**
     * Maps [update] onto a request setting whether its record counts towards the sharing member count of its golden
     * record.
     */
    fun toRequest(update: GoldenRecordCountedUpdateRaw): SharingMemberRecordUpdateV7Request =
        SharingMemberRecordUpdateV7Request(update.recordId, update.isGoldenRecordCounted)

    /**
     * Maps [tasks] onto a request asking for the current state of each of these tasks.
     */
    fun toRequest(tasks: List<TaskReferenceRaw>): TaskStateV7Request =
        TaskStateV7Request(tasks.map { TaskStateV7Request.Entry(taskId = it.taskId, recordId = it.recordId) })
}
