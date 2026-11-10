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
 * SPDX-License-Identifier: Apache-2.0
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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AdditionalSite
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddressScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReference
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartner
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelations
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessState
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinate
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.Identifier
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntity
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddressScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariantWithScriptCode
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressWithScriptVariants
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationType as OrchestratorRelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecordUpdateRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.Site
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.SiteScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.Street
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.StreetScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsRequestEntry
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateRequestEntry
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskMode
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateRequest
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedProperties
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedScriptVariant
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
    fun toCreateRequest(candidates: List<GoldenRecordTaskCandidateParsed>): TaskCreateRequest =
        TaskCreateRequest(
            mode = TaskMode.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRequestEntry(recordId = it.recordId, businessPartner = toBusinessPartner(it)) }
        )

    /**
     * Maps [taskIds] onto a request asking for the result state of each of these tasks.
     */
    fun toResultStateSearchRequest(taskIds: List<String>): TaskResultStateSearchRequest =
        TaskResultStateSearchRequest(taskIds)

    private fun toBusinessPartner(candidate: GoldenRecordTaskCandidateParsed): BusinessPartner {
        val input = candidate.input
        val addressType = input.address.addressType
        val postalAddress = toPostalAddress(input)
        val isLegalAddress = addressType == AddressType.LegalAddress || addressType == AddressType.LegalAndSiteMainAddress
        val isSiteMainAddress = addressType == AddressType.SiteMainAddress

        return BusinessPartner(
            nameParts = emptyList(),
            uncategorized = UncategorizedProperties(
                nameParts = input.nameParts,
                // The Gate stores every input identifier as uncategorized, so none belongs to the legal entity or address.
                identifiers = input.identifiers.map { Identifier(it.value, it.type, it.issuingBody) },
                states = toStates(input, BusinessPartnerType.GENERIC),
                address = postalAddress.takeIf { addressType == null }?.let { toWithScriptVariants(it, input) },
                scriptVariants = input.scriptVariants.map { UncategorizedScriptVariant(it.scriptCode, it.nameParts) }
            ),
            owningCompany = candidate.owningCompanyBpnl,
            legalEntity = LegalEntity(
                bpnReference = toBpnReference(input.legalEntity.bpn),
                legalName = input.legalEntity.legalName,
                legalForm = input.legalEntity.legalForm,
                legalShortName = input.legalEntity.shortName,
                identifiers = emptyList(),
                states = toStates(input, BusinessPartnerType.LEGAL_ENTITY),
                confidenceCriteria = ConfidenceCriteria.empty,
                isParticipantData = null,
                hasChanged = true,
                ownershipUltimate = input.legalEntity.ownershipUltimate,
                ultimateOwnerBpnl = input.legalEntity.ultimateOwnerBpnl,
                legalAddress = postalAddress.takeIf { isLegalAddress } ?: PostalAddress.empty,
                scriptVariants = if (isLegalAddress) input.scriptVariants.map(::toLegalEntityScriptVariant) else emptyList()
            ),
            site = Site(
                bpnReference = toBpnReference(input.site.bpn),
                siteName = input.site.name,
                states = toStates(input, BusinessPartnerType.SITE),
                confidenceCriteria = ConfidenceCriteria.empty,
                hasChanged = true,
                siteMainAddress = postalAddress.takeIf { isSiteMainAddress },
                scriptVariants = if (isSiteMainAddress) input.scriptVariants.map(::toSiteScriptVariant) else emptyList()
            ).takeIf { input.site.bpn != null || input.site.name != null },
            additionalAddress = postalAddress.takeIf { addressType == AddressType.AdditionalAddress }?.let { toWithScriptVariants(it, input) },
            additionalSites = input.additionalSites.map { AdditionalSite(bpnReference = toBpnReference(it.bpn), siteName = it.name) }
        )
    }

    private fun toPostalAddress(input: BusinessPartnerInputContentParsed): PostalAddress =
        PostalAddress(
            bpnReference = toBpnReference(input.address.bpn),
            addressName = input.address.name,
            identifiers = emptyList(),
            states = toStates(input, BusinessPartnerType.ADDRESS),
            confidenceCriteria = ConfidenceCriteria.empty,
            physicalAddress = input.address.physicalPostalAddress?.let(::toPhysicalAddress) ?: PhysicalAddress.empty,
            alternativeAddress = input.address.alternativePostalAddress?.let(::toAlternativeAddress),
            hasChanged = null
        )

    private fun toWithScriptVariants(postalAddress: PostalAddress, input: BusinessPartnerInputContentParsed) =
        PostalAddressWithScriptVariants(
            postalAddress,
            input.scriptVariants.map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, toPostalAddressScriptVariant(it)) }
        )

    private fun toPhysicalAddress(address: BusinessPartnerValuesRaw.PhysicalPostalAddress) =
        with(address) {
            PhysicalAddress(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let(::toStreet) ?: Street.empty,
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
            AlternativeAddress(
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

    private fun toGeoCoordinate(coordinate: GateGeoCoordinate?) =
        coordinate?.let { GeoCoordinate(it.longitude, it.latitude, it.altitude) } ?: GeoCoordinate.empty

    private fun toStates(input: BusinessPartnerInputContentParsed, businessPartnerType: BusinessPartnerType) =
        input.states.filter { it.businessPartnerType == businessPartnerType }.map {
            BusinessState(it.validFrom?.toInstant(ZoneOffset.UTC), it.validTo?.toInstant(ZoneOffset.UTC), it.type)
        }

    private fun toBpnReference(bpn: String?) =
        bpn?.let { BpnReference(referenceValue = it, desiredBpn = null, referenceType = BpnReferenceType.Bpn) } ?: BpnReference.empty

    private fun toLegalEntityScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        LegalEntityScriptVariant(scriptVariant.scriptCode, scriptVariant.legalName, scriptVariant.shortName, toPostalAddressScriptVariant(scriptVariant))

    private fun toSiteScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        SiteScriptVariant(scriptVariant.scriptCode, scriptVariant.siteName ?: "", toPostalAddressScriptVariant(scriptVariant))

    private fun toPostalAddressScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        PostalAddressScriptVariant(
            addressName = scriptVariant.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariant(city = it) }
        )

    private fun toPhysicalAddressScriptVariant(address: BusinessPartnerValuesRaw.PhysicalAddressScriptVariant) =
        with(address) {
            PhysicalAddressScriptVariant(
                city = city,
                district = district,
                street = with(street) { StreetScriptVariant(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix) },
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
    fun toCreateRequest(candidates: List<RelationTaskCandidateParsed>): TaskCreateRelationsRequest =
        TaskCreateRelationsRequest(
            mode = TaskMode.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRelationsRequestEntry(recordId = it.recordId, businessPartnerRelations = toRelations(it)) }
        )

    private fun toRelations(candidate: RelationTaskCandidateParsed): BusinessPartnerRelations =
        BusinessPartnerRelations(
            relationType = toOrchestratorType(candidate.relationType),
            businessPartnerSourceBpn = candidate.sourceBpn,
            businessPartnerTargetBpn = candidate.targetBpn,
            validityPeriods = candidate.validityPeriods.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) },
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
    fun toRequest(update: GoldenRecordCountedUpdateRaw): SharingMemberRecordUpdateRequest =
        SharingMemberRecordUpdateRequest(update.recordId, update.isGoldenRecordCounted)

    /**
     * Maps [tasks] onto a request asking for the current state of each of these tasks.
     */
    fun toRequest(tasks: List<TaskReferenceRaw>): TaskStateRequest =
        TaskStateRequest(tasks.map { TaskStateRequest.Entry(taskId = it.taskId, recordId = it.recordId) })
}
