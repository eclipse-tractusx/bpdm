/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangeLogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/catena")
interface GateChangelogApi {


    @Operation(
        summary = "Get business partner changelog entries for changes to the business partner input data",
        description = "Get business partner changelog entries for changes to the business partner input data. Filter by list external id, from timestamp and/or lsa type"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified parameters"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/changelog/search")
    @PostExchange("/input/changelog/search")
    fun getInputChangelog(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody searchRequest: ChangeLogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto>


    @Operation(
        summary = "Get business partner changelog entries for changes to the business partner output data",
        description = "Get business partner changelog entries for changes to the business partner output data. Filter by list external id, from timestamp and/or lsa type"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified parameters"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/changelog/search")
    @PostExchange("/output/changelog/search")
    fun getOutputChangelog(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody searchRequest: ChangeLogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto>
}