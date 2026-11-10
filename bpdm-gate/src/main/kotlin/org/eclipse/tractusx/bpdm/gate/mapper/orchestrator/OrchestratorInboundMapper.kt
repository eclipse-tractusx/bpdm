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
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordBpnsRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.task.FinishedGoldenRecordTaskPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskOutputRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResultRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.RelationTaskResultRaw
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelation
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.AddressGoldenRecordRelationType as OrchestratorAddressRelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.AlternativeAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartner
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessState
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.FinishedTaskEventsResponse
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GeoCoordinate as OrchestratorGeoCoordinate
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.GoldenRecordType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.Identifier
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.IsPostalAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelation
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.LegalEntityGoldenRecordRelationType as OrchestratorLegalEntityRelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PhysicalAddress
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.PostalAddressScriptVariant
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultState
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelation
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.SiteGoldenRecordRelationType as OrchestratorSiteRelationType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.Street as OrchestratorStreet
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateDto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorType
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

/**
 * Maps Orchestrator responses to their raw model.
 */
@Component
class OrchestratorInboundMapper {

    /**
     * Maps [task] onto its result, translating the error type the task failed with first into a sharing error code.
     */
    fun toRequest(task: TaskClientStateDto): GoldenRecordTaskResultRaw =
        when (task.processingState.resultState) {
            ResultState.Pending -> GoldenRecordTaskResultRaw.Pending(task.taskId)
            ResultState.Success -> GoldenRecordTaskResultRaw.Succeeded(task.taskId, toRequest(task.businessPartnerResult), toBpns(task.businessPartnerResult))
            ResultState.Error -> toFailed(task)
        }

    private fun toBpns(businessPartner: BusinessPartner): GoldenRecordBpnsRaw =
        with(businessPartner) {
            GoldenRecordBpnsRaw(
                legalEntities = setOfNotNull(legalEntity.bpnReference.referenceValue),
                sites = setOfNotNull(site?.bpnReference?.referenceValue),
                addresses = setOfNotNull(
                    legalEntity.legalAddress.bpnReference.referenceValue,
                    site?.siteMainAddress?.bpnReference?.referenceValue,
                    additionalAddress?.bpnReference?.referenceValue
                )
            )
        }

    private fun toFailed(task: TaskClientStateDto): GoldenRecordTaskResultRaw.Failed {
        val errors = task.processingState.errors
        return GoldenRecordTaskResultRaw.Failed(
            taskId = task.taskId,
            errorCode = toErrorCode(errors.firstOrNull()?.type),
            errorMessage = errors.joinToString(" // ") { it.description }.take(255).ifEmpty { null }
        )
    }

    private fun toErrorCode(errorType: TaskErrorType?): BusinessPartnerSharingErrorCode =
        when (errorType) {
            TaskErrorType.Timeout -> BusinessPartnerSharingErrorCode.SharingTimeout
            TaskErrorType.NaturalPersonError -> BusinessPartnerSharingErrorCode.NaturalPersonError
            TaskErrorType.BpnErrorNotFound -> BusinessPartnerSharingErrorCode.BpnErrorNotFound
            TaskErrorType.BpnErrorTooManyOptions -> BusinessPartnerSharingErrorCode.BpnErrorTooManyOptions
            TaskErrorType.MandatoryFieldValidationFailed -> BusinessPartnerSharingErrorCode.MandatoryFieldValidationFailed
            TaskErrorType.BlacklistCountryPresent -> BusinessPartnerSharingErrorCode.BlacklistCountryPresent
            TaskErrorType.UnknownSpecialCharacters -> BusinessPartnerSharingErrorCode.UnknownSpecialCharacters
            else -> BusinessPartnerSharingErrorCode.SharingProcessError
        }

    /**
     * Maps [businessPartner] onto its output request, taking the address the golden record type points to as the
     * business partner's address and leaving every value the result lacks empty.
     */
    fun toRequest(businessPartner: BusinessPartner): GoldenRecordTaskOutputRaw {
        val addressType = toAddressType(businessPartner)
        val address = addressType?.let { addressOf(it, businessPartner) }
        val legalEntity = businessPartner.legalEntity
        val site = businessPartner.site

        return GoldenRecordTaskOutputRaw(
            addressType = addressType,
            owningCompanyBpnl = businessPartner.owningCompany,
            nameParts = businessPartner.uncategorized.nameParts,
            identifiers = toIdentifiers(businessPartner.uncategorized.identifiers, BusinessPartnerType.GENERIC)
                    + toIdentifiers(legalEntity.identifiers, BusinessPartnerType.LEGAL_ENTITY)
                    + toIdentifiers(address?.identifiers.orEmpty(), BusinessPartnerType.ADDRESS),
            states = toStates(businessPartner.uncategorized.states, BusinessPartnerType.GENERIC)
                    + toStates(legalEntity.states, BusinessPartnerType.LEGAL_ENTITY)
                    + toStates(site?.states.orEmpty(), BusinessPartnerType.SITE)
                    + toStates(address?.states.orEmpty(), BusinessPartnerType.ADDRESS),
            legalEntityBpn = legalEntity.bpnReference.referenceValue,
            legalName = legalEntity.legalName,
            shortName = legalEntity.legalShortName,
            legalForm = legalEntity.legalForm,
            siteBpn = site?.bpnReference?.referenceValue,
            siteName = site?.siteName,
            addressBpn = address?.bpnReference?.referenceValue,
            addressName = address?.addressName,
            physicalPostalAddress = address?.physicalAddress?.let(::toPhysicalPostalAddress),
            alternativePostalAddress = address?.alternativeAddress?.let(::toAlternativePostalAddress),
            legalEntityConfidence = toConfidence(legalEntity.confidenceCriteria),
            siteConfidence = site?.let { toConfidence(it.confidenceCriteria) },
            addressConfidence = address?.let { toConfidence(it.confidenceCriteria) },
            legalEntityUpdatedAt = legalEntity.updatedAt,
            siteUpdatedAt = site?.updatedAt,
            addressUpdatedAt = address?.updatedAt,
            scriptVariants = addressType?.let { toScriptVariants(it, businessPartner) }.orEmpty(),
            legalEntityGoldenRecordRelations = legalEntity.goldenRecordRelations.map(::toRelation),
            siteGoldenRecordRelations = site?.goldenRecordRelations.orEmpty().map(::toRelation),
            addressGoldenRecordRelations = address?.goldenRecordRelations.orEmpty().map(::toRelation),
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            additionalSites = businessPartner.additionalSites.map { BusinessPartnerValuesRaw.AdditionalSite(bpn = it.bpnReference.referenceValue, name = it.siteName) }
        )
    }

    private fun toAddressType(businessPartner: BusinessPartner): AddressType? =
        when (businessPartner.type) {
            GoldenRecordType.LegalEntity -> AddressType.LegalAddress
            GoldenRecordType.Site -> businessPartner.site?.let { if (it.siteMainIsLegalAddress) AddressType.LegalAndSiteMainAddress else AddressType.SiteMainAddress }
            GoldenRecordType.Address -> AddressType.AdditionalAddress
            null -> null
        }

    private fun addressOf(addressType: AddressType, businessPartner: BusinessPartner): IsPostalAddress? =
        when (addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> businessPartner.legalEntity.legalAddress
            AddressType.SiteMainAddress -> businessPartner.site?.siteMainAddress
            AddressType.AdditionalAddress -> businessPartner.additionalAddress
        }

    private fun toIdentifiers(identifiers: List<Identifier>, businessPartnerType: BusinessPartnerType) =
        identifiers.map { GoldenRecordTaskOutputRaw.Identifier(it.type, it.value, it.issuingBody, businessPartnerType) }

    private fun toStates(states: List<BusinessState>, businessPartnerType: BusinessPartnerType) =
        states.map { GoldenRecordTaskOutputRaw.State(it.type, toLocalDateTime(it.validFrom), toLocalDateTime(it.validTo), businessPartnerType) }

    private fun toConfidence(confidence: ConfidenceCriteria) =
        with(confidence) {
            GoldenRecordTaskOutputRaw.Confidence(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = toLocalDateTime(lastConfidenceCheckAt),
                nextConfidenceCheckAt = toLocalDateTime(nextConfidenceCheckAt),
                confidenceLevel = confidenceLevel
            )
        }

    private fun toPhysicalPostalAddress(address: PhysicalAddress) =
        with(address) {
            BusinessPartnerValuesRaw.PhysicalPostalAddress(
                geographicCoordinates = toGeoCoordinate(geographicCoordinates),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = toStreet(street),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toAlternativePostalAddress(address: AlternativeAddress) =
        with(address) {
            BusinessPartnerValuesRaw.AlternativePostalAddress(
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

    private fun toStreet(street: OrchestratorStreet) =
        with(street) {
            StreetRaw(
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                name = name,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix,
                houseNumber = houseNumber,
                houseNumberSupplement = houseNumberSupplement,
                milestone = milestone,
                direction = direction
            )
        }

    private fun toGeoCoordinate(coordinate: OrchestratorGeoCoordinate): GeoCoordinateRaw? {
        val latitude = coordinate.latitude ?: return null
        val longitude = coordinate.longitude ?: return null
        return GeoCoordinateRaw(longitude = longitude, latitude = latitude, altitude = coordinate.altitude)
    }

    private fun toScriptVariants(addressType: AddressType, businessPartner: BusinessPartner): List<BusinessPartnerValuesRaw.ScriptVariant> {
        val legalEntityVariantsByCode = businessPartner.legalEntity.scriptVariants.associateBy { it.scriptCode }
        val siteVariantsByCode = businessPartner.site?.scriptVariants.orEmpty().associateBy { it.scriptCode }
        val addressVariantsByCode = when (addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> legalEntityVariantsByCode.mapValues { it.value.legalAddress }
            AddressType.SiteMainAddress -> siteVariantsByCode.mapValues { it.value.mainAddress }
            AddressType.AdditionalAddress -> businessPartner.additionalAddress?.scriptVariants.orEmpty().associate { it.scriptCode to it.postalProperties }
        }
        val uncategorizedVariantsByCode = businessPartner.uncategorized.scriptVariants.associateBy { it.scriptCode }

        val scriptCodes = addressVariantsByCode.keys + legalEntityVariantsByCode.keys + siteVariantsByCode.keys + uncategorizedVariantsByCode.keys
        return scriptCodes.map { scriptCode ->
            val addressVariant = addressVariantsByCode[scriptCode]
            BusinessPartnerValuesRaw.ScriptVariant(
                scriptCode = scriptCode,
                nameParts = uncategorizedVariantsByCode[scriptCode]?.nameParts.orEmpty(),
                legalName = legalEntityVariantsByCode[scriptCode]?.legalName,
                shortName = legalEntityVariantsByCode[scriptCode]?.legalShortName,
                siteName = siteVariantsByCode[scriptCode]?.siteName,
                addressName = addressVariant?.addressName,
                physicalAddress = toPhysicalAddressScriptVariant(addressVariant),
                alternativeAddressCity = addressVariant?.alternativeAddress?.city
            )
        }
    }

    private fun toPhysicalAddressScriptVariant(addressVariant: PostalAddressScriptVariant?): BusinessPartnerValuesRaw.PhysicalAddressScriptVariant {
        val physicalAddress = addressVariant?.physicalAddress
        val street = physicalAddress?.street
        return BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
            city = physicalAddress?.city,
            district = physicalAddress?.district,
            street = BusinessPartnerValuesRaw.StreetScriptVariant(
                name = street?.name,
                direction = street?.direction,
                namePrefix = street?.namePrefix,
                additionalNamePrefix = street?.additionalNamePrefix,
                nameSuffix = street?.nameSuffix,
                additionalNameSuffix = street?.additionalNameSuffix
            ),
            industrialZone = physicalAddress?.industrialZone,
            building = physicalAddress?.building,
            floor = physicalAddress?.floor,
            door = physicalAddress?.door
        )
    }

    private fun toRelation(relation: LegalEntityGoldenRecordRelation) =
        GoldenRecordRelationRaw(
            type = when (relation.relationType) {
                OrchestratorLegalEntityRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
                OrchestratorLegalEntityRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
                OrchestratorLegalEntityRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationType.IsOwnedBy
                OrchestratorLegalEntityRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toRelation(relation: SiteGoldenRecordRelation) =
        GoldenRecordRelationRaw(
            type = when (relation.relationType) {
                OrchestratorSiteRelationType.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toRelation(relation: AddressGoldenRecordRelation) =
        GoldenRecordRelationRaw(
            type = when (relation.relationType) {
                OrchestratorAddressRelationType.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toLocalDateTime(instant: Instant?) =
        instant?.atZone(ZoneOffset.UTC)?.toLocalDateTime()

    /**
     * Maps [task] onto its result, translating the error type the task failed with first into a sharing error code.
     */
    fun toRequest(task: TaskClientRelationsStateDto): RelationTaskResultRaw =
        when (task.processingState.resultState) {
            ResultState.Pending -> RelationTaskResultRaw.Pending(task.taskId)
            ResultState.Success -> toSucceeded(task)
            ResultState.Error -> toFailed(task)
        }

    private fun toSucceeded(task: TaskClientRelationsStateDto): RelationTaskResultRaw.Succeeded =
        with(task.businessPartnerRelationsResult) {
            RelationTaskResultRaw.Succeeded(
                taskId = task.taskId,
                relationType = toRelationType(relationType),
                sourceBpn = businessPartnerSourceBpn,
                targetBpn = businessPartnerTargetBpn,
                validityPeriods = validityPeriods.map { RelationValidityPeriodRaw(validFrom = it.validFrom, validTo = it.validTo) },
                reasonCode = reasonCode
            )
        }

    private fun toFailed(task: TaskClientRelationsStateDto): RelationTaskResultRaw.Failed {
        val errors = task.processingState.errors
        return RelationTaskResultRaw.Failed(
            taskId = task.taskId,
            errorCode = toErrorCode(errors.firstOrNull()?.type),
            errorMessage = errors.joinToString { it.description }.take(250)
        )
    }

    private fun toRelationType(relationType: RelationType): SharableRelationType =
        when (relationType) {
            RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> SharableRelationType.IsManagedBy
            RelationType.IsOwnedBy -> SharableRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> SharableRelationType.IsReplacedBy
        }

    private fun toErrorCode(errorType: TaskRelationsErrorType?): RelationSharingStateErrorCode =
        when (errorType) {
            TaskRelationsErrorType.Timeout -> RelationSharingStateErrorCode.SharingTimeout
            TaskRelationsErrorType.Unspecified, null -> RelationSharingStateErrorCode.SharingProcessError
        }

    /**
     * Maps the first page of [events] onto its finished tasks, the finish time of its last event and whether more
     * pages follow.
     */
    fun toPage(events: FinishedTaskEventsResponse): FinishedGoldenRecordTaskPageRaw =
        FinishedGoldenRecordTaskPageRaw(
            taskIds = events.content.map { it.taskId },
            lastFinishedAt = events.content.lastOrNull()?.timestamp,
            hasMore = events.totalPages > 1
        )
}
