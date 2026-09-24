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
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoverningRelationUpsertRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoverningRelationParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoverningRelationResultMapper
import org.eclipse.tractusx.bpdm.pool.model.error.OwnershipUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.OwnershipUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.OwnershipUpsertService
import org.eclipse.tractusx.bpdm.pool.service.parser.relation.OwnershipUpsertParser
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Answers a reserved golden record relation task that states an ownership with the ownership as it now stands.
 */
@Service
class OwnershipUpsertApplicationService(
    private val upsertRequestMapper: GoverningRelationUpsertRequestMapper,
    private val upsertParser: OwnershipUpsertParser,
    private val ownershipUpsertService: OwnershipUpsertService,
    private val parseErrorMapper: GoverningRelationParseErrorMapper,
    private val resultMapper: GoverningRelationResultMapper
) {

    /**
     * Writes the ownership the task states and reports it back, or reports that task's reasons for not writing it.
     */
    @Transactional
    fun upsert(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto =
        when (val result = upsertParser.parse(upsertRequestMapper.toOwnershipRequest(taskEntry.businessPartnerRelations))) {
            is ParseResult.Failure -> toErrorReply(taskEntry, result.errors)
            is ParseResult.Success -> toSuccessReply(taskEntry, result.parsed)
        }

    private fun toErrorReply(
        taskEntry: TaskRelationsStepReservationEntryDto,
        errors: List<OwnershipUpsertParseError>
    ): TaskRelationsStepResultEntryDto =
        TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartnerRelations = taskEntry.businessPartnerRelations,
            errors = errors.map { TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, parseErrorMapper.toUpsertDescription(it)) }
        )

    private fun toSuccessReply(
        taskEntry: TaskRelationsStepReservationEntryDto,
        parsed: OwnershipUpsertParsed
    ): TaskRelationsStepResultEntryDto =
        TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartnerRelations = resultMapper.toTaskResult(ownershipUpsertService.upsert(parsed).value),
            errors = emptyList()
        )
}
