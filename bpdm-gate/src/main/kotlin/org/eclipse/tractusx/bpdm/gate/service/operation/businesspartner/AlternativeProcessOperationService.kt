/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.mapper.ParsedToDbMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.AlternativeOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerChangeRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Persists validated provider outputs and sharing-state transitions; reference resolution belongs to the parser. */
@Service
class AlternativeProcessOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val sharingStateOperation: SharingStateOperationService,
    private val changelogOperation: ChangelogCreateOperationService,
    private val mapper: ParsedToDbMapper,
    private val changeLogger: PersistedChangeLogger
) {
    /** Replaces every parsed output and marks its sharing state successful in the caller's transaction. */
    @Transactional
    fun write(outputs: List<AlternativeOutputParsed>): List<BusinessPartnerDb> =
            outputs.map { request ->
                val partner = request.partner
                val state = request.sharingState
                val tenantBpnl = state.tenantBpnl
                val output = request.storedOutput ?: BusinessPartnerDb.createEmpty(state, StageType.Output)
                val changeType = if (request.storedOutput != null) ChangelogType.UPDATE else ChangelogType.CREATE
                mapper.writeContent(partner.content, output)
                output.identifiers.addAll(request.addressIdentifiers.map {
                    IdentifierDb(it.type, it.value, it.issuingBody, BusinessPartnerType.ADDRESS)
                })
                output.process = partner.process
                output.externalSequenceTimestamp = partner.externalSequenceTimestamp
                output.legalEntityConfidence = request.legalEntityConfidence?.let(mapper::toEntity)
                output.siteConfidence = request.siteConfidence?.let(mapper::toEntity)
                output.addressConfidence = request.addressConfidence?.let(mapper::toEntity)
                output.legalEntityUpdatedAt = request.legalEntityUpdatedAt
                output.siteUpdatedAt = request.siteUpdatedAt
                output.addressUpdatedAt = request.addressUpdatedAt
                output.legalEntityGoldenRecordRelations.clear()
                output.siteGoldenRecordRelations.clear()
                output.addressGoldenRecordRelations.clear()
                output.updatedAt = DbTimestamp.now()
                businessPartnerRepository.save(output)
                changelogOperation.record(ChangelogEntryDb(state.externalId, tenantBpnl, changeType, StageType.Output, GoldenRecordType.BusinessPartner, output.process))
                changeLogger.log(BusinessPartnerChangeRaw(
                    if (changeType == ChangelogType.CREATE) PersistedChangeType.Created else PersistedChangeType.Updated,
                    state.externalId, tenantBpnl, BusinessPartnerChangeRaw.Aspect.Output
                ))
                sharingStateOperation.succeed(listOf(state))
                output
            }

    /** Marks resolved records as picked up without assigning an Orchestrator task. */
    @Transactional
    fun pending(states: List<SharingStateDb>) {
        states.forEach { state ->
            state.sharingStateType = SharingStateType.Pending
            state.sharingErrorCode = null
            state.sharingErrorMessage = null
            state.taskId = null
            state.sharingProcessStarted = state.sharingProcessStarted ?: LocalDateTime.now()
            state.updatedAt = DbTimestamp.now()
            sharingStateRepository.save(state)
            changeLogger.log(BusinessPartnerChangeRaw(PersistedChangeType.Updated, state.externalId, state.tenantBpnl, BusinessPartnerChangeRaw.Aspect.SharingState))
        }
    }

    /** Fails resolved records with the provider's original error message. */
    @Transactional
    fun error(states: List<SharingStateDb>, message: String) {
        states.forEach {
            sharingStateOperation.fail(it, BusinessPartnerSharingErrorCode.SharingProcessError, message)
        }
    }

}
