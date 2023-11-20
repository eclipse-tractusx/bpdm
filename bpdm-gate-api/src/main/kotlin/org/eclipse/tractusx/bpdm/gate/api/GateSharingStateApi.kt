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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
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
        summary = "Returns sharing states of business partners, optionally filtered by a business partner type and an array of external IDs"
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
        @Parameter(description = "Business partner type") @RequestParam(required = false) businessPartnerType: BusinessPartnerType?,
        @Parameter(description = "External IDs") @RequestParam(required = false) externalIds: Collection<String>?
    ): PageDto<SharingStateDto>

    @Operation(
        summary = "Creates or updates a sharing state of a business partner",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Operation completed successfully"),
            ApiResponse(responseCode = "400", description = "Invalid data (e.g. external ID)", content = [Content()])
        ]
    )
    @PutMapping
    @PutExchange
    fun upsertSharingState(@RequestBody request: SharingStateDto)
}
