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

import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskSearchResultStatesParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskSearchResultStatesParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskSearchResultStatesRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.ParseResult
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GoldenRecordTaskSearchResultStatesParser(
    private val taskRepository: GoldenRecordTaskRepository
) {
    fun parse(
        requests: List<GoldenRecordTaskSearchResultStatesRequest>
    ): List<ParseResult<GoldenRecordTaskSearchResultStatesParsed, GoldenRecordTaskSearchResultStatesParseError>> {
        val parsedUuids = requests.map { request ->
            request.taskId.let { taskId ->
                try {
                    UUID.fromString(taskId)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
        val existingTasks = taskRepository.findByUuidIn(parsedUuids.filterNotNull().toSet())
            .associateBy { it.uuid }

        return requests.zip(parsedUuids).map { (request, uuid) ->
            val requestPath = "taskIds[${request.index}]"
            val errors = mutableListOf<GoldenRecordTaskSearchResultStatesParseError>()

            if (uuid == null) {
                errors.add(GoldenRecordTaskSearchResultStatesParseError.InvalidTaskId("$requestPath", request.taskId))
            }

            val resolvedTask = if (uuid != null) existingTasks[uuid] else null

            if (errors.isEmpty()) {
                ParseResult.Success(
                    GoldenRecordTaskSearchResultStatesParsed(
                        uuid = uuid ?: UUID.randomUUID(),
                        task = resolvedTask
                    )
                )
            } else {
                ParseResult.Failure(errors)
            }
        }
    }
}
