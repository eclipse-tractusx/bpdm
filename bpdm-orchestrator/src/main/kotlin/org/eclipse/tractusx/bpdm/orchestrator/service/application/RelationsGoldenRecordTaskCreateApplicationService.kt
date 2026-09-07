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

package org.eclipse.tractusx.bpdm.orchestrator.service.application

import org.eclipse.tractusx.bpdm.common.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordIdNotValid
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.mapper.RelationsGoldenRecordTaskCreateInboundMapper
import org.eclipse.tractusx.bpdm.orchestrator.model.error.RelationsGoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationsGoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.RelationsResponseMapper
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.RelationsGoldenRecordTaskCreateOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.RelationsGoldenRecordTaskCreateParser
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RelationsGoldenRecordTaskCreateApplicationService(
    private val inboundMapper: RelationsGoldenRecordTaskCreateInboundMapper,
    private val parser: RelationsGoldenRecordTaskCreateParser,
    private val operation: RelationsGoldenRecordTaskCreateOperation,
    private val responseMapper: RelationsResponseMapper,
    private val taskConfigProperties: TaskConfigProperties
) {

    @Transactional
    fun createTasks(createRequest: TaskCreateRelationsRequest): TaskCreateRelationsResponse {
        val requests = createRequest.requests.map { inboundMapper.toRequest(it) }
        val createdTasks = createTasksInternal(createRequest.mode, requests)

        return createdTasks
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateRelationsResponse(createdTasks = it) }
    }

    private fun createTasksInternal(
        mode: TaskMode,
        requests: List<RelationsGoldenRecordTaskCreateRequest>
    ): List<RelationsGoldenRecordTaskDb> =
        parseAndExecuteAllOrNone(
            requests,
            parser::parse,
            ::toValidationException,
            execute = { parsed -> operation.execute(mode, parsed) }
        )

    private fun toValidationException(errors: List<RelationsGoldenRecordTaskCreateParseError>): RuntimeException =
        when {
            errors.any { it is RelationsGoldenRecordTaskCreateParseError.RecordIdInvalid } ->
                BpdmRecordIdNotValid(errors.filterIsInstance<RelationsGoldenRecordTaskCreateParseError.RecordIdInvalid>().first().recordId)

            else ->
                BpdmRecordNotFoundException(
                    errors.filterIsInstance<RelationsGoldenRecordTaskCreateParseError.RecordNotFound>().map { UUID.fromString(it.recordId) }
                )
        }

    private fun calculateTaskRetentionTimeout(task: RelationsGoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)
}
