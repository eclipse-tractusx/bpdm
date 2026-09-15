/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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


package org.eclipse.tractusx.bpdm.pool.service.application.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskUpsertRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskResultMapper
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.GoldenRecordUpsertResult
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordUpsertService
import org.eclipse.tractusx.bpdm.pool.service.parser.task.BpnReferenceParser
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordUpsertParser
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Answers a batch of reserved golden record tasks with the state of the records they asked for.
 *
 * Entries are handled one after another rather than as one batch: two entries may name the same BPN request
 * identifier and must then reach the same record, which is only settled once the earlier entry has been written.
 */
@Service
class GoldenRecordTaskApplicationService(
    private val upsertRequestMapper: GoldenRecordTaskUpsertRequestMapper,
    private val bpnReferenceParser: BpnReferenceParser,
    private val upsertParser: GoldenRecordUpsertParser,
    private val upsertService: GoldenRecordUpsertService,
    private val parseErrorMapper: GoldenRecordTaskParseErrorMapper,
    private val taskResultMapper: GoldenRecordTaskResultMapper
) {

    /**
     * Writes what each task asks for and reports the resulting records, or that task's reasons for not being written.
     */
    @Transactional
    fun upsert(taskEntries: List<TaskStepReservationEntryDto>): List<TaskStepResultEntryDto> {
        val requests = taskEntries.map { upsertRequestMapper.toRequest(it) }
        val bpnReferences = BpnReferenceAllocation(bpnReferenceParser.parse(requests))

        return taskEntries.zip(requests) { taskEntry, request ->
            when (val result = upsertParser.parse(request, bpnReferences)) {
                is ParseResult.Failure -> toErrorReply(taskEntry, result.errors)
                is ParseResult.Success -> toSuccessReply(taskEntry, upsertService.upsert(result.parsed, bpnReferences))
            }
        }
    }

    private fun toErrorReply(taskEntry: TaskStepReservationEntryDto, errors: List<GoldenRecordUpsertParseError>): TaskStepResultEntryDto =
        TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = taskEntry.businessPartner,
            errors = errors.map { TaskErrorDto(TaskErrorType.Unspecified, parseErrorMapper.toUpsertDescription(it)) }
        )

    private fun toSuccessReply(taskEntry: TaskStepReservationEntryDto, written: GoldenRecordUpsertResult): TaskStepResultEntryDto =
        TaskStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartner = taskResultMapper.toTaskResult(taskEntry.businessPartner, written),
            errors = emptyList()
        )
}
