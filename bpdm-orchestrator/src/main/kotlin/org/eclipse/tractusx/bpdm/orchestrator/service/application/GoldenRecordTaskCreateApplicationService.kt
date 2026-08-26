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
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordIdNotValid
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.mapper.v6.GoldenRecordTaskCreateInboundMapperV6
import org.eclipse.tractusx.bpdm.orchestrator.mapper.v6.GoldenRecordTaskCreateOutboundMapperV6
import org.eclipse.tractusx.bpdm.orchestrator.mapper.v7.GoldenRecordTaskCreateInboundMapperV7
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskCreateParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.ResponseMapper
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.GoldenRecordTaskCreateOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskCreateParser
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequest as TaskCreateRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateResponse as TaskCreateResponseV6

/**
 * The entry point for the "create golden record tasks" operation: binds the [GoldenRecordTaskCreateParser] and the
 * [GoldenRecordTaskCreateOperation] into the full validate-then-execute business logic, shared by the V6 and V7 API
 * versions via their respective inbound/outbound mappers.
 */
@Service
class GoldenRecordTaskCreateApplicationService(
    private val inboundMapperV6: GoldenRecordTaskCreateInboundMapperV6,
    private val inboundMapperV7: GoldenRecordTaskCreateInboundMapperV7,
    private val outboundMapperV6: GoldenRecordTaskCreateOutboundMapperV6,
    private val parser: GoldenRecordTaskCreateParser,
    private val operation: GoldenRecordTaskCreateOperation,
    private val responseMapper: ResponseMapper,
    private val taskConfigProperties: TaskConfigProperties
) {

    @Transactional
    fun createTasksV7(createRequest: TaskCreateRequest): TaskCreateResponse {
        val requests = createRequest.requests.map { inboundMapperV7.toRequest(it) }
        val createdTasks = createTasks(createRequest.mode, requests, newGateRecordIsGoldenRecordCounted = true)

        return createdTasks
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }

    @Transactional
    fun createTasksV6(createRequest: TaskCreateRequestV6): TaskCreateResponseV6 {
        val requests = createRequest.requests.map { inboundMapperV6.toRequest(it) }
        val createdTasks = createTasks(createRequest.mode, requests, newGateRecordIsGoldenRecordCounted = null)

        return createdTasks
            .map { task -> outboundMapperV6.toClientState(responseMapper.toClientState(task, calculateTaskRetentionTimeout(task))) }
            .let { TaskCreateResponseV6(createdTasks = it) }
    }

    private fun createTasks(
        mode: TaskMode,
        requests: List<GoldenRecordTaskCreateRequest>,
        newGateRecordIsGoldenRecordCounted: Boolean?
    ): List<GoldenRecordTaskDb> =
        parseAndExecuteAllOrNone(
            requests,
            parser::parse,
            ::toValidationException,
            execute = { parsed -> operation.execute(mode, parsed, newGateRecordIsGoldenRecordCounted) }
        )

    /**
     * Reconstructs the exact exception the previous, pre-refactoring implementation threw for these problems, so
     * existing error responses stay unchanged: an entry whose additional sites have no site of their own always
     * wins (that check used to run, and could throw, before any record ID was even looked at); next, an invalid
     * record ID format (used to be looked up eagerly, in request order, before existence was checked); finally the
     * IDs of every requested record that does not exist.
     */
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
