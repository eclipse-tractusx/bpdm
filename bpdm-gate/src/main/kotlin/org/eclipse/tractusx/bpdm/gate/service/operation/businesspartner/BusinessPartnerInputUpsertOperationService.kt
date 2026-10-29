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

package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.model.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.mapper.entity.BusinessPartnerInputEntityMapper
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputContentMapper
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertEntryResult.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes parsed business partners into the input stage of their tenant.
 */
@Service
class BusinessPartnerInputUpsertOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val sharingStateOperation: SharingStateOperationService,
    private val changelogCreateOperation: ChangelogCreateOperationService,
    private val contentMapper: BusinessPartnerInputContentMapper,
    private val entityMapper: BusinessPartnerInputEntityMapper,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the input of each of [requests] for [tenantBpnl] and resets its sharing state, resubmits an
     * unchanged input whose sharing failed, or skips a request that is outdated or changes nothing.
     */
    @Transactional
    fun upsert(requests: List<BusinessPartnerInputUpsertParsed>, tenantBpnl: String?): List<BusinessPartnerInputUpsertEntryResult> {
        val sharingStates = sharingStateOperation.getOrCreate(requests.map { it.externalId }, tenantBpnl)
        val existingByExternalId = businessPartnerRepository.findBySharingStateInAndStage(sharingStates, StageType.Input)
            .associateBy { it.sharingState.externalId }

        return requests.zip(sharingStates) { request, sharingState ->
            upsert(request, sharingState, existingByExternalId[request.externalId])
        }
    }

    private fun upsert(
        request: BusinessPartnerInputUpsertParsed,
        sharingState: SharingStateDb,
        existing: BusinessPartnerDb?
    ): BusinessPartnerInputUpsertEntryResult {
        val result = when {
            existing == null -> create(request, sharingState)
            isOutdated(request, existing) -> Skipped(request.externalId, SkipReason.Outdated)
            hasChanged(request, existing) -> update(request, existing)
            sharingState.sharingStateType == SharingStateType.Error -> resubmit(existing)
            else -> Skipped(request.externalId, SkipReason.Unchanged)
        }
        logDebug(result)
        return result
    }

    private fun create(request: BusinessPartnerInputUpsertParsed, sharingState: SharingStateDb): Created {
        val input = BusinessPartnerDb.createEmpty(sharingState, StageType.Input)
        write(request, input)
        persist(input, ChangelogType.CREATE)
        logChange(input, PersistedChangeType.Created)
        return Created(input)
    }

    private fun update(request: BusinessPartnerInputUpsertParsed, input: BusinessPartnerDb): Updated {
        write(request, input)
        persist(input, ChangelogType.UPDATE)
        logChange(input, PersistedChangeType.Updated)
        return Updated(input)
    }

    private fun resubmit(input: BusinessPartnerDb): Resubmitted {
        sharingStateOperation.reset(input.sharingState)
        return Resubmitted(input)
    }

    private fun isOutdated(request: BusinessPartnerInputUpsertParsed, existing: BusinessPartnerDb): Boolean {
        val requestedTimestamp = request.externalSequenceTimestamp ?: return false
        val existingTimestamp = existing.externalSequenceTimestamp ?: return false
        return !requestedTimestamp.isAfter(existingTimestamp)
    }

    private fun hasChanged(request: BusinessPartnerInputUpsertParsed, existing: BusinessPartnerDb) =
        contentMapper.toContent(existing) != request.content

    private fun persist(input: BusinessPartnerDb, changeType: ChangelogType) {
        val sharingState = input.sharingState
        businessPartnerRepository.save(input)
        changelogCreateOperation.record(
            ChangelogEntryDb(sharingState.externalId, sharingState.tenantBpnl, changeType, StageType.Input, GoldenRecordType.BusinessPartner)
        )
        sharingStateOperation.reset(sharingState)
    }

    private fun logChange(input: BusinessPartnerDb, type: PersistedChangeType) {
        val sharingState = input.sharingState
        changeLogger.log(BusinessPartnerChange(type, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.Input))
    }

    private fun logDebug(result: BusinessPartnerInputUpsertEntryResult) =
        logger.debug {
            when (result) {
                is Created -> "Created business partner input ${result.input.toInputLogIdentifier()}"
                is Updated -> "Updated business partner input ${result.input.toInputLogIdentifier()}"
                is Resubmitted -> "Resubmitted unchanged business partner input ${result.input.toInputLogIdentifier()} after a sharing error"
                is Skipped -> "Skipped business partner input ${result.externalId}: ${result.reason}"
            }
        }

    private fun write(request: BusinessPartnerInputUpsertParsed, input: BusinessPartnerDb) {
        val content = request.content
        input.externalSequenceTimestamp = request.externalSequenceTimestamp
        input.nameParts.replace(content.nameParts)
        input.roles.replace(content.roles)
        input.identifiers.replace(content.identifiers.map(entityMapper::toEntity))
        input.states.replace(content.states.map(entityMapper::toEntity))
        input.isOwnCompanyData = content.isOwnCompanyData

        input.bpnL = content.legalEntity.bpn
        input.legalName = content.legalEntity.legalName
        input.shortName = content.legalEntity.shortName
        input.legalForm = content.legalEntity.legalForm
        input.ownershipUltimate = content.legalEntity.ownershipUltimate
        input.ultimateOwnerBpnl = content.legalEntity.ultimateOwnerBpnl

        input.bpnS = content.site.bpn
        input.siteName = content.site.name
        input.additionalSites.replace(content.additionalSites.map(entityMapper::toEntity))

        input.bpnA = content.address.bpn
        input.addressName = content.address.name
        writeAddress(content.address, input.postalAddress)

        syncScriptVariants(content.scriptVariants, input)
    }

    private fun writeAddress(address: BusinessPartnerInputContent.Address, postalAddress: PostalAddressDb) {
        postalAddress.addressType = address.addressType
        postalAddress.physicalPostalAddress = address.physicalPostalAddress?.let(entityMapper::toEntity)
        postalAddress.alternativePostalAddress = address.alternativePostalAddress?.let(entityMapper::toEntity)
    }

    // Updated in place by script code: replacing the list would insert before the orphans are deleted and break the
    // unique (script_code, business_partner_id) constraint.
    private fun syncScriptVariants(scriptVariants: Collection<BusinessPartnerValues.ScriptVariant>, input: BusinessPartnerDb) {
        val existingByCode = input.scriptVariants.associateBy { it.scriptCode }
        input.scriptVariants.removeIf { existing -> scriptVariants.none { it.scriptCode == existing.scriptCode } }

        scriptVariants.forEach { scriptVariant ->
            val target = existingByCode[scriptVariant.scriptCode]
                ?: BusinessPartnerScriptVariantDb(scriptVariant.scriptCode, input).also { input.scriptVariants.add(it) }
            writeScriptVariant(scriptVariant, target)
        }
    }

    private fun writeScriptVariant(scriptVariant: BusinessPartnerValues.ScriptVariant, target: BusinessPartnerScriptVariantDb) {
        target.nameParts.replace(scriptVariant.nameParts)
        target.legalName = scriptVariant.legalName
        target.shortName = scriptVariant.shortName
        target.siteName = scriptVariant.siteName
        target.addressName = scriptVariant.addressName
        target.physicalAddress = entityMapper.toEntity(scriptVariant.physicalAddress)
        target.alternativeAddress = scriptVariant.alternativeAddressCity?.let(entityMapper::toAlternativeAddressScriptVariant)
    }
}

private fun BusinessPartnerDb.toInputLogIdentifier(): String {
    val bpns = listOfNotNull(bpnL, bpnS, bpnA)
    return if (bpns.isEmpty()) sharingState.externalId else "${sharingState.externalId} (${bpns.joinToString("/")})"
}
