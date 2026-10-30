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

package org.eclipse.tractusx.bpdm.gate.service.parser.task

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.PoolGoldenRecordsRequest
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskOutputError
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResolutionPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResultRaw
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord.GoldenRecordAlignmentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides how the sharing states pending on finished golden record tasks resolve.
 */
@Service
class GoldenRecordTaskResolutionParser(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val outputParser: GoldenRecordTaskOutputParser,
    private val alignmentParser: GoldenRecordAlignmentParser
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Resolves each sharing state still pending on a task in [page] into the outcome of its task, failing those whose
     * task, input or task result is unusable and leaving out those whose task is still pending.
     */
    @Transactional(readOnly = true)
    fun parse(page: GoldenRecordTaskResolutionPageRaw): List<ParseResult<GoldenRecordTaskResolutionParsed, GoldenRecordTaskResolutionError>> {
        val pendingSharingStates = sharingStateRepository.findByTaskIdIn(page.finishedTasks.taskIds.toSet())
            .filter { it.sharingStateType == SharingStateType.Pending }
        val resultsByTaskId = page.results.associateBy { it.taskId }
        val succeededSharingStates = pendingSharingStates.filter { resultsByTaskId[it.taskId] is GoldenRecordTaskResultRaw.Succeeded }
        val inputsBySharingStateId = findBySharingStates(succeededSharingStates, StageType.Input)
        val storedOutputsBySharingStateId = findBySharingStates(succeededSharingStates, StageType.Output)

        return pendingSharingStates.mapNotNull { sharingState ->
            when (val result = resultsByTaskId[sharingState.taskId]) {
                null -> ParseResult.ofSingleFailure(GoldenRecordTaskResolutionError.TaskMissing(sharingState))
                is GoldenRecordTaskResultRaw.Pending -> null
                is GoldenRecordTaskResultRaw.Failed ->
                    ParseResult.Success(GoldenRecordTaskResolutionParsed.Failed(sharingState, result.errorCode, result.errorMessage))
                is GoldenRecordTaskResultRaw.Succeeded -> parseSucceeded(
                    sharingState,
                    result,
                    inputsBySharingStateId[sharingState.id],
                    storedOutputsBySharingStateId[sharingState.id],
                    page.goldenRecords
                )
            }
        }
    }

    private fun parseSucceeded(
        sharingState: SharingStateDb,
        result: GoldenRecordTaskResultRaw.Succeeded,
        input: BusinessPartnerDb?,
        storedOutput: BusinessPartnerDb?,
        goldenRecords: PoolGoldenRecordsRequest
    ): ParseResult<GoldenRecordTaskResolutionParsed, GoldenRecordTaskResolutionError> {
        if (input == null) return ParseResult.ofSingleFailure(GoldenRecordTaskResolutionError.InputMissing(sharingState))

        val taskOutput = when (val parsedOutput = outputParser.parse(result.output, input.roles, sharingState.tenantBpnl)) {
            is ParseResult.Success -> parsedOutput.parsed
            is ParseResult.Failure -> return ParseResult.ofSingleFailure(
                GoldenRecordTaskResolutionError.ResultIncomplete(sharingState, parsedOutput.errors.map(::toMissingValue))
            )
        }

        if (!isNewerThanStored(taskOutput, storedOutput))
            return ParseResult.Success(GoldenRecordTaskResolutionParsed.Succeeded(sharingState, null))

        val output = when (val aligned = alignmentParser.parse(sharingState, storedOutput, taskOutput, goldenRecords)) {
            is ParseResult.Success -> aligned.parsed
            is ParseResult.Failure -> {
                aligned.errors.forEach(::logKeptUnaligned)
                BusinessPartnerOutputParsed(sharingState, storedOutput, taskOutput)
            }
        }
        return ParseResult.Success(GoldenRecordTaskResolutionParsed.Succeeded(sharingState, output))
    }

    private fun logKeptUnaligned(error: GoldenRecordAlignmentError) {
        val externalId = error.sharingState.externalId
        when (error) {
            is GoldenRecordAlignmentError.GoldenRecordMissing ->
                logger.debug { "Kept the task result of business partner output '$externalId' unaligned: the Pool does not return golden record '${error.bpn}'" }
            is GoldenRecordAlignmentError.AdoptedSiteMissing ->
                logger.warn { "Kept the task result of business partner output '$externalId' unaligned: its address belongs to site '${error.siteBpn}' which the Pool does not return" }
        }
    }

    private fun findBySharingStates(sharingStates: List<SharingStateDb>, stage: StageType): Map<Long, BusinessPartnerDb> =
        businessPartnerRepository.findBySharingStateInAndStage(sharingStates, stage).associateBy { it.sharingState.id }

    private fun isNewerThanStored(incoming: OutputUpsertData, stored: BusinessPartnerDb?): Boolean {
        val incomingComponentTimestamps = listOf(incoming.legalEntityUpdatedAt, incoming.siteUpdatedAt, incoming.addressUpdatedAt)
        if (incomingComponentTimestamps.all { it == null }) return true
        if (stored == null) return true

        val storedComponentTimestamps = listOf(stored.legalEntityUpdatedAt, stored.siteUpdatedAt, stored.addressUpdatedAt)
        if (storedComponentTimestamps.all { it == null }) return true

        return incomingComponentTimestamps.zip(storedComponentTimestamps)
            .filter { (incomingTimestamp, _) -> incomingTimestamp != null }
            .any { (incomingTimestamp, storedTimestamp) -> storedTimestamp == null || incomingTimestamp!!.isAfter(storedTimestamp) }
    }

    private fun toMissingValue(error: GoldenRecordTaskOutputError): String =
        when (error) {
            is GoldenRecordTaskOutputError.ValueMissing -> error.property
        }
}
