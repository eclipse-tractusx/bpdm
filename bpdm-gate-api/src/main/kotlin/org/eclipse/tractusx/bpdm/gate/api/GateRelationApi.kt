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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.GateErrorResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.RelationPutResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateRelationApi {

    @Operation(
        summary = "Find business partner input relations",
        description = "Find paginated list of business partner relations from the input stage. " +
                "There are various filter criteria available. " +
                "All filters are 'AND' filters."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "A paginated list of business partner relations for the input stage")
        ]
    )
    @PostMapping(value = ["${ApiCommons.RELATIONS_INPUT_PATH_V7}/search"])
    fun postSearch(
        @RequestBody searchRequest: RelationSearchRequest = RelationSearchRequest(),
        @ParameterObject @Valid paginationRequest: PaginationRequest = PaginationRequest()
    ): PageDto<RelationDto>

    @Operation(
        summary = "Update a business partner input relation",
        description = "Update an existing business partner relation on the input stage. " +
                "By using a request parameter it is also possible to create a relation if the relation with the given external identifier does not exist. "
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The given business partner relation has been updated."),
            ApiResponse(responseCode = "400", description = "On wrong or insufficient user provided data like references to non-existent business partners or relations that violate the relation constraints. ",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GateErrorResponse::class)
                )])
        ]
    )
    @PutMapping(value = [ApiCommons.RELATIONS_INPUT_PATH_V7], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun put(
        @Schema(description = "If true a business partner relation will be created even if a relation could not be found under the given external identifier.")
        @RequestParam createIfNotExist: Boolean = true,
        @RequestBody requestBody: RelationPutRequest
       ): RelationPutResponse

}