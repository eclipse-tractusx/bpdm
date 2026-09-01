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
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskResolveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskResolveRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.eclipse.tractusx.bpdm.orchestrator.util.toUuidOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GoldenRecordTaskResolveParser(
    private val taskRepository: GoldenRecordTaskRepository
) {

    fun parse(resolveRequest: GoldenRecordTaskResolveRequest): List<ParseResult<GoldenRecordTaskResolveParsed, GoldenRecordTaskResolveParseError>> {
        val uuids = resolveRequest.results.map { toUuidOrNull(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.filterNotNull().toSet())
            .also { taskRepository.fetchBusinessPartnerData(it) }
            .associateBy { it.uuid.toString() }

        return resolveRequest.results.zip(uuids).map { (resultEntry, uuid) ->
            when {
                uuid == null -> ParseResult.ofSingleFailure(GoldenRecordTaskResolveParseError.TaskNotFound(resultEntry.taskId))
                else -> {
                    val task = foundTasks[resultEntry.taskId]
                    when {
                        task == null -> ParseResult.ofSingleFailure(GoldenRecordTaskResolveParseError.TaskNotFound(resultEntry.taskId))
                        task.processingState.resultState == GoldenRecordTaskDb.ResultState.Aborted -> 
                            ParseResult.ofSingleFailure(GoldenRecordTaskResolveParseError.TaskAborted(resultEntry.taskId))
                        resultEntry.errors.isEmpty() -> {
                            val validationError = validateAdditionalSitesHaveSite(resultEntry.businessPartner)
                            if (validationError != null) {
                                ParseResult.ofSingleFailure(GoldenRecordTaskResolveParseError.InvalidBusinessPartner(resultEntry.taskId, validationError))
                            } else {
                                ParseResult.Success(GoldenRecordTaskResolveParsed(resolveRequest.step, task, resultEntry))
                            }
                        }
                        else -> ParseResult.Success(GoldenRecordTaskResolveParsed(resolveRequest.step, task, resultEntry))
                    }
                }
            }
        }
    }

    private fun validateAdditionalSitesHaveSite(businessPartner: org.eclipse.tractusx.orchestrator.api.model.BusinessPartner): String? {
        return if (businessPartner.additionalSites.isNotEmpty() && businessPartner.site == null) {
            "additional sites of its address are stated but no site of its own is, which they would be additional to"
        } else {
            null
        }
    }
}
