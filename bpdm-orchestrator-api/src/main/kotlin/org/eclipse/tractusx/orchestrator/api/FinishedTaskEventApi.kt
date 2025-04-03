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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.orchestrator.api.model.FinishedTaskEventsResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant


@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface FinishedTaskEventApi {

    @Operation(
        summary = "Get event log of golden record tasks that have finished processing",
        description = "The event log contains all events of when golden record tasks finish processing. " +
                "These events are helpful for the task creator to check whether created tasks have finished processing. " +
                "A paginated list of events that happened after a given time is returned. "
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The paginated events after the given timestamp"
            ),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
        ]
    )
    @Tag(name = TagClient)
    @GetMapping(value = ["${ApiCommons.BASE_PATH_V6}/finished-events", "${ApiCommons.BASE_PATH_V7_BUSINESS_PARTNERS}/finished-events"])
    fun getEvents(@RequestParam timestamp: Instant,
                  @ParameterObject paginationRequest: PaginationRequest
    ): FinishedTaskEventsResponse

}