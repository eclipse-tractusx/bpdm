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

package org.eclipse.tractusx.bpdm.orchestrator.service.application.v7

import org.eclipse.tractusx.bpdm.common.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordIdNotValid
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.mapper.v7.GoldenRecordTaskCreateInboundMapperV7
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.ResponseMapper
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.GoldenRecordTaskCreateOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskCreateParser
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * The REST-API boundary for the V7 "create golden record tasks" operation.
 */
@Service
class GoldenRecordTaskCreateApplicationV7Service(
    private val inboundMapperV7: GoldenRecordTaskCreateInboundMapperV7,
    private val parser: GoldenRecordTaskCreateParser,
    private val operation: GoldenRecordTaskCreateOperation,
    private val responseMapper: ResponseMapper,
    private val taskConfigProperties: TaskConfigProperties
) {

    @Transactional
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        val requests = createRequest.requests.map { inboundMapperV7.toRequest(it) }
        val createdTasks = createTasksInternal(createRequest.mode, requests, newGateRecordIsGoldenRecordCounted = null)

        return createdTasks
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }

    private fun createTasksInternal(
        mode: org.eclipse.tractusx.orchestrator.api.model.TaskMode,
        requests: List<GoldenRecordTaskCreateRequest>,
        newGateRecordIsGoldenRecordCounted: Boolean?
    ): List<GoldenRecordTaskDb> =
        parseAndExecuteAllOrNone(
            requests,
            parser::parse,
            ::toValidationException,
            execute = { parsed -> operation.execute(mode, parsed, newGateRecordIsGoldenRecordCounted) }
        )

    private fun toValidationException(errors: List<GoldenRecordTaskCreateParseError>): RuntimeException =
        when {
            errors.any { it is GoldenRecordTaskCreateParseError.AdditionalSitesWithoutSite } ->
                BpdmInvalidBusinessPartnerException(
                    "additional sites of its address are stated but no site of its own is, which they would be additional to"
                )

            errors.any { it is GoldenRecordTaskCreateParseError.RecordIdInvalid } ->
                BpdmRecordIdNotValid(errors.filterIsInstance<GoldenRecordTaskCreateParseError.RecordIdInvalid>().first().recordId)

            else ->
                BpdmRecordNotFoundException(
                    errors.filterIsInstance<GoldenRecordTaskCreateParseError.RecordNotFound>().map { UUID.fromString(it.recordId) }
                )
        }

    private fun calculateTaskRetentionTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)
}
