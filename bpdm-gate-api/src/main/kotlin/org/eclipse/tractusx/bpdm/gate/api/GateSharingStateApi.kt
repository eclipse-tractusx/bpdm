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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PutExchange

@RequestMapping("/api/catena/sharing-state", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/catena/sharing-state")
interface GateSharingStateApi {

    @Operation(
        summary = "Get sharing states (including error info and BPN) for business partners, optionally filtered by LSA type and external ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of sharing states")
        ]
    )
    @GetMapping
    @GetExchange
    fun getSharingStates(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @Parameter(description = "LSA Type") @RequestParam(required = false) lsaType: LsaType?,
        @Parameter(description = "External identifiers") @RequestParam(required = false) externalIds: Collection<String>?
    ): PageResponse<SharingStateDto>

    @Operation(
        summary = "Insert/update sharing state (including error info and BPN) for business partner with LSA type and external ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Operation completed successfully"),
            ApiResponse(responseCode = "400", description = "Invalid data (e.g. externalId)", content = [Content()])
        ]
    )
    @PutMapping
    @PutExchange
    fun upsertSharingState(@RequestBody request: SharingStateDto)

}
