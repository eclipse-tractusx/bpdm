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
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/catena")
interface GateSiteApi {

    @Operation(
        summary = "Create or update sites.",
        description = "Create or update sites. " +
                "Updates instead of creating a new site if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of sites in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sites were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed site request", content = [Content()]),
        ]
    )
    @PutMapping("/input/sites")
    @PutExchange("/input/sites")
    fun upsertSites(@RequestBody sites: Collection<SiteGateInputRequest>): ResponseEntity<Unit>

    @Operation(
        summary = "Get site by external identifier",
        description = "Get site by external identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with external identifier"),
            ApiResponse(responseCode = "404", description = "No site found under specified external identifier", content = [Content()])
        ]
    )
    @GetMapping("/input/sites/{externalId}")
    @GetExchange("/input/sites/{externalId}")
    fun getSiteByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): SiteGateInputDto

    @Operation(
        summary = "Get page of sites filtered by a collection of externalIds",
        description = "Get page of sites filtered by a collection of externalIds."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/sites/search")
    @PostExchange("/input/sites/search")
    fun getSitesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<SiteGateInputDto>

    @Operation(
        summary = "Get page of sites",
        description = "Get page of sites."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/sites")
    @GetExchange("/input/sites")
    fun getSites(@ParameterObject @Valid paginationRequest: PaginationRequest): PageDto<SiteGateInputDto>

    @Operation(
        summary = "Get page of sites",
        description = "Get page of sites. Can optionally be filtered by external ids."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/sites/search")
    @PostExchange("/output/sites/search")
    fun getSitesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<SiteGateOutputDto>

    @Operation(
        summary = "Create or update output sites.",
        description = "Create or update sites (Output). " +
                "Updates instead of creating a new site if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of sites in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sites were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed site request", content = [Content()]),
        ]
    )
    @PutMapping("/output/sites")
    @PutExchange("/output/sites")
    fun upsertSitesOutput(@RequestBody sites: Collection<SiteGateOutputRequest>): ResponseEntity<Unit>

}