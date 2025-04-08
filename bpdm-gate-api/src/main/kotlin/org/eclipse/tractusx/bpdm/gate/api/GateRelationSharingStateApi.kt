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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateRelationSharingStateApi {

    @Operation(
        summary = "Returns sharing states of shared business partner relations which can be optionally filtered"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of sharing states")
        ]
    )
    @Tag(name = "sharing-state-controller")
    @GetMapping(value = [ApiCommons.RELATION_SHARING_STATE_PATH_V7])
    fun get(
        @Parameter(description = "Only show sharing states of given external IDs")
        @RequestParam(required = false) externalIds: Collection<String>? = null,
        @Parameter(description = "Only show sharing states of given types")
        @RequestParam(required = false) sharingStateTypes: Collection<RelationSharingStateType>? = null,
        @Parameter(description = "Only show sharing states updated after given time")
        @RequestParam(required = false) updatedAfter: Instant? = null,
        @ParameterObject @Valid paginationRequest: PaginationRequest = PaginationRequest()
    ): PageDto<RelationSharingStateDto>
}