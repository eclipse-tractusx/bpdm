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
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.exception.BpdmAlternativeProcessException
import org.eclipse.tractusx.bpdm.gate.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.gate.mapper.ParsedToDbMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.AlternativeOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputDesignationParsed
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

@Service
class AlternativeProcessOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val sharingStateOperation: SharingStateOperationService,
    private val changelogOperation: ChangelogCreateOperationService,
    private val mapper: ParsedToDbMapper,
    private val outputMapper: DbToOutputMapper,
    private val changeLogger: PersistedChangeLogger
) {
    @Transactional
    fun write(outputs: List<AlternativeOutputParsed>, tenantBpnl: String?) =
        outputs.map { it.partner }.let { partners ->
            val states = validate(partners.map { it.externalId to it.process }, tenantBpnl)
            val existing = businessPartnerRepository.findBySharingStateInAndStage(states.values, StageType.Output)
                .associateBy { it.sharingState.externalId }
            outputs.map { request ->
                val partner = request.partner
                val state = states.getValue(partner.externalId)
                val output = existing[partner.externalId] ?: BusinessPartnerDb.createEmpty(state, StageType.Output)
                val changeType = if (existing.containsKey(partner.externalId)) ChangelogType.UPDATE else ChangelogType.CREATE
                mapper.writeContent(partner.content, output)
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
                changelogOperation.record(ChangelogEntryDb(state.externalId, tenantBpnl, changeType, StageType.Output, GoldenRecordType.BusinessPartner))
                changeLogger.log(BusinessPartnerChangeRaw(
                    if (changeType == ChangelogType.CREATE) PersistedChangeType.Created else PersistedChangeType.Updated,
                    state.externalId, tenantBpnl, BusinessPartnerChangeRaw.Aspect.Output
                ))
                sharingStateOperation.succeed(listOf(state))
                outputMapper.toRecord(BusinessPartnerOutputDesignationParsed(
                    output,
                    output.identifiers.filter { it.businessPartnerType == org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType.GENERIC },
                    output.states.filter { it.businessPartnerTyp == org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType.GENERIC }
                ))
            }
        }

    @Transactional
    fun pending(externalIds: Collection<String>, process: String, tenantBpnl: String?) {
        validate(externalIds.map { it to process }, tenantBpnl).values.forEach { state ->
            state.sharingStateType = SharingStateType.Pending
            state.sharingErrorCode = null
            state.sharingErrorMessage = null
            state.taskId = null
            state.sharingProcessStarted = state.sharingProcessStarted ?: LocalDateTime.now()
            state.updatedAt = DbTimestamp.now()
            sharingStateRepository.save(state)
            changeLogger.log(BusinessPartnerChangeRaw(PersistedChangeType.Updated, state.externalId, tenantBpnl, BusinessPartnerChangeRaw.Aspect.SharingState))
        }
    }

    @Transactional
    fun error(externalIds: Collection<String>, process: String, message: String, tenantBpnl: String?) {
        validate(externalIds.map { it to process }, tenantBpnl).values.forEach {
            sharingStateOperation.fail(it, BusinessPartnerSharingErrorCode.SharingProcessError, message)
        }
    }

    private fun validate(records: List<Pair<String, String?>>, tenantBpnl: String?): Map<String, SharingStateDb> {
        if (records.map { it.first }.distinct().size != records.size)
            throw BpdmAlternativeProcessException("External IDs must be unique")
        val states = sharingStateRepository.findByExternalIdInAndTenantBpnl(records.map { it.first }, tenantBpnl).associateBy { it.externalId }
        records.forEach { (externalId, process) ->
            val state = states[externalId]
                ?: throw BpdmAlternativeProcessException("Business partner '$externalId' not found in the caller's tenant")
            if (process.isNullOrBlank() || state.process == null || state.process != process)
                throw BpdmAlternativeProcessException("Business partner '$externalId' does not belong to process '$process'")
        }
        return states
    }
}
