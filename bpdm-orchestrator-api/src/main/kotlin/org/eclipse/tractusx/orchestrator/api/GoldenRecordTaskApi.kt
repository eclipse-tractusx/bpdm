/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.orchestrator.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

const val TagClient = "Task Client"
const val TagWorker = "Task Worker"

@RequestMapping("/api/golden-record-tasks", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/golden-record-tasks")
interface GoldenRecordTaskApi {

    @Operation(
        summary = "Create new golden record tasks for given business partner data",
        description = "Create golden record tasks for given business partner data in given mode. " +
                "The mode decides through which processing steps the given business partner data will go through. " +
                "The response contains the states of the created tasks in the order of given business partner data." +
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
    @Tag(name = TagClient)
    @PostMapping
    @PostExchange
    fun createTasks(@RequestBody createRequest: TaskCreateRequest): TaskCreateResponse

    @Operation(
        summary = "Search for the state of golden record tasks by task identifiers",
        description = "Returns the state of golden record tasks based on the provided task identifiers. Unknown task identifiers are ignored."
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
    @Tag(name = TagClient)
    @PostMapping("/state/search")
    @PostExchange("/state/search")
    fun searchTaskStates(@RequestBody stateRequest: TaskStateRequest): TaskStateResponse

    @Operation(
        summary = "Reserve the next golden record tasks waiting in the given step queue",
        description = "Reserve up to a given number of golden record tasks in the given step queue. " +
                "The response entries contain the business partner data to process which consists of the generic and L/S/A data. " +
                "The reservation has a time limit which is returned. " +
                "For a single request, the maximum number of reservable tasks is limited to \${bpdm.api.upsert-limit}."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The reserved tasks with their business partner data to process."
            ),
            ApiResponse(responseCode = "400", description = "On malformed task create requests or reaching upsert limit", content = [Content()]),
        ]
    )
    @Tag(name = TagWorker)
    @PostMapping("/step-reservations")
    @PostExchange("/step-reservations")
    fun reserveTasksForStep(@RequestBody reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse

    @Operation(
        summary = "Post step results for reserved golden record tasks in the given step queue",
        description = "Post business partner step results for the given tasks in the given step queue. " +
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
    @Tag(name = TagWorker)
    @PostMapping("/step-results")
    @PostExchange("/step-results")
    fun resolveStepResults(@RequestBody resultRequest: TaskStepResultRequest)
}
