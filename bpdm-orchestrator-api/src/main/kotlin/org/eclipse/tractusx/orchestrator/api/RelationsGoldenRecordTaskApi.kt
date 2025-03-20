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
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.eclipse.tractusx.orchestrator.api.RelationsGoldenRecordTaskApi.Companion.RELATIONS_TASKS_PATH
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsResponse


@RequestMapping(RELATIONS_TASKS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface RelationsGoldenRecordTaskApi {

    companion object{
        const val RELATIONS_TASKS_PATH = "${ApiCommons.BASE_PATH}/relations/golden-record-tasks"
    }

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
    @Tag(name = TagClient)
    @PostMapping
    fun createTasks(@RequestBody createRequest: TaskCreateRelationsRequest): TaskCreateRelationsResponse

}