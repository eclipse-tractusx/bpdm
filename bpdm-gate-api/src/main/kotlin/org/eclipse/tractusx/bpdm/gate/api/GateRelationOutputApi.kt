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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateRelationOutputApi.Companion.RELATIONS_OUTPUT_PATH
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(RELATIONS_OUTPUT_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateRelationOutputApi {

    companion object{
        const val RELATIONS_OUTPUT_PATH =  "${ApiCommons.BASE_PATH}/output/business-partner-relations"
    }

    @Operation(
        summary = "Find business partner output relations",
        description = "Find paginated list of business partner relations from the output stage. " +
                "There are various filter criteria available. " +
                "All filters are 'AND' filters."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "A paginated list of business partner relations for the input stage")
        ]
    )
    @Tag(name = "relation-controller")
    @PostMapping("/search")
    fun postSearch(
        @RequestBody searchRequest: RelationOutputSearchRequest = RelationOutputSearchRequest(),
        @ParameterObject @Valid paginationRequest: PaginationRequest = PaginationRequest()
    ): PageDto<RelationOutputDto>
}