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

package org.eclipse.tractusx.bpdm.orchestrator.api.v7

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.orchestrator.api.ApiCommons
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStateV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskResultStateSearchV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface OrchestratorRelationsGoldenRecordTaskV7Api {

    @Operation(
        summary = "Create new golden record relations tasks for given business partner relationship data",
        description = "Create golden record tasks for given business partner relationship data in given mode. " +
                "The mode decides through which processing steps the given business partner relationship data will go through. " +
                "The response contains the states of the created tasks in the order of given business partner relationship data." +
                "If there is an error in the request no tasks are created (all or nothing). " +
                "For a single request, the maximum number of business partners in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The states of successfully created tasks including the task identifier for tracking purposes."
            ),
            ApiResponse(responseCode = "400", description = "On malformed task create requests or reaching upsert limit", content = [Content()]),
        ]
    )
    @Tag(name = ApiCommons.TAG_CLIENT)
    @PostMapping(value = [ApiCommons.BASE_PATH_V7_RELATIONS])
    fun createTasks(@RequestBody createRequest: TaskCreateRelationsV7Request): TaskCreateRelationsV7Response

    @Operation(
        summary = "Search for the state of relations golden record tasks by task identifiers",
        description = "Returns the state of relations golden record tasks based on the provided task identifiers. Unknown task identifiers are ignored."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The state of the tasks for the provided task identifiers."
            ),
            ApiResponse(responseCode = "400", description = "On malformed task search requests", content = [Content()]),
        ]
    )
    @Tag(name = ApiCommons.TAG_CLIENT)
    @PostMapping(value = ["${ApiCommons.BASE_PATH_V7_RELATIONS}/state/search"])
    fun searchTaskStates(@RequestBody stateRequest: TaskStateV7Request): TaskRelationsStateV7Response

    @Operation(
        summary = "Search for result states by giving a list of task IDs",
        description = ""
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The list of corresponding result states in the same order as has been received. A null indicates that a task could not be found."
            ),
            ApiResponse(responseCode = "400", description = "On malformed task search requests", content = [Content()]),
        ]
    )
    @Tag(name = ApiCommons.TAG_CLIENT)
    @PostMapping(value = ["${ApiCommons.BASE_PATH_V7_RELATIONS}/result-state/search"])
    fun searchTaskResultStates(@RequestBody stateRequest: TaskResultStateSearchV7Request): TaskResultStateSearchV7Response

    @Operation(
        summary = "Reserve the next relations golden record tasks waiting in the given step queue",
        description = "Reserve up to a given number of relations golden record tasks in the given step queue. " +
                "The response entries contain the business partner relations data. " +
                "The reservation has a time limit which is returned. " +
                "For a single request, the maximum number of reservable tasks is limited to \${bpdm.api.upsert-limit}."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The reserved tasks with their business partner relations data to process."
            ),
            ApiResponse(responseCode = "400", description = "On malformed task create requests or reaching upsert limit", content = [Content()]),
        ]
    )
    @Tag(name = ApiCommons.TAG_WORKER)
    @PostMapping(value = ["${ApiCommons.BASE_PATH_V7_RELATIONS}/step-reservations"])
    fun reserveTasksForStep(@RequestBody reservationRequest: TaskStepReservationV7Request): TaskRelationsStepReservationV7Response

    @Operation(
        summary = "Post step results for reserved relations golden record tasks in the given step queue",
        description = "Post business partner relations step results for the given tasks in the given step queue. " +
                "In order to post a result for a task it needs to be reserved first, has to currently be in the given step queue and the time limit is not exceeded. " +
                "The number of results you can post at a time does not need to match the original number of reserved tasks. " +
                "Results are accepted via strategy 'all or nothing'. " +
                "For a single request, the maximum number of postable results is limited to \${bpdm.api.upsert-limit}."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "If the results could be processed"
            ),
            ApiResponse(
                responseCode = "400",
                description = "On malformed requests, reaching upsert limit or posting results for tasks which are missing or in the wrong step queue",
                content = [Content()]
            ),
        ]
    )
    @Tag(name = ApiCommons.TAG_WORKER)
    @PostMapping(value = ["${ApiCommons.BASE_PATH_V7_RELATIONS}/step-results"])
    fun resolveStepResults(@RequestBody resultRequest: TaskRelationsStepResultV7Request)
}
