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

package org.eclipse.tractusx.bpdm.orchestrator.service.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.model.error.RelationsGoldenRecordTaskResolveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.RelationsGoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationsGoldenRecordTaskResolveRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchRelationsData
import org.eclipse.tractusx.bpdm.orchestrator.util.toUuidOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RelationsGoldenRecordTaskResolveParser(
    private val taskRepository: RelationsGoldenRecordTaskRepository
) {

    fun parse(resolveRequest: RelationsGoldenRecordTaskResolveRequest): List<ParseResult<RelationsGoldenRecordTaskResolveParsed, RelationsGoldenRecordTaskResolveParseError>> {
        val uuids = resolveRequest.results.map { toUuidOrNull(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.filterNotNull().toSet())
            .also { taskRepository.fetchRelationsData(it) }
            .associateBy { it.uuid.toString() }

        return resolveRequest.results.zip(uuids).map { (resultEntry, uuid) ->
            when {
                uuid == null -> ParseResult.ofSingleFailure(RelationsGoldenRecordTaskResolveParseError.TaskNotFound(resultEntry.taskId))
                else -> {
                    val task = foundTasks[uuid.toString()]
                    when {
                        task == null -> ParseResult.ofSingleFailure(RelationsGoldenRecordTaskResolveParseError.TaskNotFound(resultEntry.taskId))
                        task.processingState.resultState == RelationsGoldenRecordTaskDb.ResultState.Aborted -> 
                            ParseResult.ofSingleFailure(RelationsGoldenRecordTaskResolveParseError.TaskAborted(resultEntry.taskId))
                        else -> {
                            try {
                                assertBusinessPartnerRelationsValid(resultEntry.businessPartnerRelations)
                                ParseResult.Success(RelationsGoldenRecordTaskResolveParsed(resolveRequest.step, task, resultEntry))
                            } catch (e: BpdmInvalidBusinessPartnerException) {
                                ParseResult.ofSingleFailure(
                                    RelationsGoldenRecordTaskResolveParseError.InvalidBusinessPartnerRelations(
                                        resultEntry.taskId,
                                        e.message ?: "Invalid business partner relations"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assertBusinessPartnerRelationsValid(businessPartnerRelations: org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations) {
        // Validate business partner relations
        if (businessPartnerRelations.businessPartnerSourceBpn.isBlank() || businessPartnerRelations.businessPartnerTargetBpn.isBlank()) {
            throw BpdmInvalidBusinessPartnerException(
                "Business partner source and target BPNs must not be blank"
            )
        }
    }
}
