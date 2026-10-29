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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.PoolGoldenRecordsRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskOutputError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResultRequest
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

    /**
     * Resolves each sharing state still pending on a task in [page] into the outcome of its task, failing those whose
     * task, input or task result is unusable and leaving out those whose task is still pending.
     */
    @Transactional(readOnly = true)
    fun parse(page: GoldenRecordTaskResolutionPageRequest): List<ParseResult<GoldenRecordTaskResolutionParsed, GoldenRecordTaskResolutionError>> {
        val pendingSharingStates = sharingStateRepository.findByTaskIdIn(page.finishedTasks.taskIds.toSet())
            .filter { it.sharingStateType == SharingStateType.Pending }
        val resultsByTaskId = page.results.associateBy { it.taskId }
        val succeededSharingStates = pendingSharingStates.filter { resultsByTaskId[it.taskId] is GoldenRecordTaskResultRequest.Succeeded }
        val inputsBySharingStateId = findBySharingStates(succeededSharingStates, StageType.Input)
        val storedOutputsBySharingStateId = findBySharingStates(succeededSharingStates, StageType.Output)

        return pendingSharingStates.mapNotNull { sharingState ->
            when (val result = resultsByTaskId[sharingState.taskId]) {
                null -> ParseResult.ofSingleFailure(GoldenRecordTaskResolutionError.TaskMissing(sharingState))
                is GoldenRecordTaskResultRequest.Pending -> null
                is GoldenRecordTaskResultRequest.Failed ->
                    ParseResult.Success(GoldenRecordTaskResolutionParsed.Failed(sharingState, result.errorCode, result.errorMessage))
                is GoldenRecordTaskResultRequest.Succeeded -> parseSucceeded(
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
        result: GoldenRecordTaskResultRequest.Succeeded,
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
            return ParseResult.Success(GoldenRecordTaskResolutionParsed.Succeeded(sharingState, null, emptyList()))

        val succeeded = when (val aligned = alignmentParser.parse(sharingState, storedOutput, taskOutput, goldenRecords)) {
            is ParseResult.Success -> GoldenRecordTaskResolutionParsed.Succeeded(sharingState, aligned.parsed, emptyList())
            is ParseResult.Failure -> GoldenRecordTaskResolutionParsed.Succeeded(
                sharingState,
                BusinessPartnerOutputParsed(sharingState, storedOutput, taskOutput),
                aligned.errors
            )
        }
        return ParseResult.Success(succeeded)
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
