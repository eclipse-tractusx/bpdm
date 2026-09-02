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
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskResolveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskResolveRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
                            try {
                                assertAdditionalSitesHaveSite(resultEntry.businessPartner)
                                ParseResult.Success(GoldenRecordTaskResolveParsed(resolveRequest.step, task, resultEntry))
                            } catch (e: BpdmInvalidBusinessPartnerException) {
                                ParseResult.ofSingleFailure(GoldenRecordTaskResolveParseError.InvalidBusinessPartner(resultEntry.taskId, e.message ?: "Invalid business partner"))
                            }
                        }
                        else -> ParseResult.Success(GoldenRecordTaskResolveParsed(resolveRequest.step, task, resultEntry))
                    }
                }
            }
        }
    }

    private fun assertAdditionalSitesHaveSite(businessPartner: org.eclipse.tractusx.orchestrator.api.model.BusinessPartner) {
        if (businessPartner.additionalSites.isNotEmpty() && businessPartner.site == null) {
            throw BpdmInvalidBusinessPartnerException(
                "additional sites of its address are stated but no site of its own is, which they would be additional to"
            )
        }
    }

    private fun toUuidOrNull(uuidString: String): UUID? =
        try {
            UUID.fromString(uuidString)
        } catch (_: IllegalArgumentException) {
            null
        }
}
