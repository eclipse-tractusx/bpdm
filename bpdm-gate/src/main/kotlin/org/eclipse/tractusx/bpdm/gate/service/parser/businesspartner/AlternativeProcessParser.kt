/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.AlternativeOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.AlternativeProcessError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeOutputRaw
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.stereotype.Service

/** Validates provider requests and resolves their tenant-scoped input and output references before any write. */
@Service
class AlternativeProcessParser(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val inputParser: BusinessPartnerInputUpsertParser
) {
    /** Resolves every record to the requested alternative process, accumulating errors for the whole batch. */
    fun parseStates(records: List<Pair<String, String?>>, tenantBpnl: String?): ParseResult<List<SharingStateDb>, AlternativeProcessError> {
        val states = sharingStateRepository.findByExternalIdInAndTenantBpnl(records.map { it.first }, tenantBpnl).associateBy { it.externalId }
        val errors = mutableListOf<AlternativeProcessError>()
        val duplicates = records.map { it.first }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) errors.add(AlternativeProcessError.DuplicateExternalIds(duplicates))
        records.forEach { (externalId, process) ->
            val state = states[externalId]
            when {
                state == null -> errors.add(AlternativeProcessError.BusinessPartnerNotFound(externalId))
                process.isNullOrBlank() || state.process == null || state.process != process ->
                    errors.add(AlternativeProcessError.ProcessMismatch(externalId, process))
            }
        }
        return if (errors.isNotEmpty()) ParseResult.Failure(errors)
        else ParseResult.Success(records.map { states.getValue(it.first) })
    }

    /** Parses a complete output batch and rejects unnamed sites instead of dropping their states and confidence. */
    fun parseOutputs(requests: List<AlternativeOutputRaw>, tenantBpnl: String?): ParseResult<List<AlternativeOutputParsed>, AlternativeProcessError> {
        val siteErrors = requests.filter { it.hasSite && it.partner.site.name.isNullOrBlank() }
            .map { AlternativeProcessError.SiteNameMissing(it.partner.externalId) }
        if (siteErrors.isNotEmpty()) return ParseResult.Failure(siteErrors)
        val partners = when (val parsed = inputParser.parse(requests.map { it.partner })) {
            is ParseResult.Failure -> return ParseResult.Failure(parsed.errors.map(AlternativeProcessError::InvalidInput))
            is ParseResult.Success -> parsed.parsed.entries
        }
        val states = when (val parsed = parseStates(partners.map { it.externalId to it.process }, tenantBpnl)) {
            is ParseResult.Failure -> return parsed
            is ParseResult.Success -> parsed.parsed
        }
        val existing = businessPartnerRepository.findBySharingStateInAndStage(states, StageType.Output).associateBy { it.sharingState.id }
        return ParseResult.Success(requests.indices.map { index ->
            val request = requests[index]
            val state = states[index]
            AlternativeOutputParsed(
                partners[index], state, existing[state.id],
                request.addressIdentifiers,
                request.legalEntityConfidence, request.siteConfidence, request.addressConfidence,
                request.legalEntityUpdatedAt, request.siteUpdatedAt, request.addressUpdatedAt
            )
        })
    }
}
