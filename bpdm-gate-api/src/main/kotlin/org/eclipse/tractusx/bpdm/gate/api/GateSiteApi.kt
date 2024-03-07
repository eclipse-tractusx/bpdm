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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateSiteApi {

    @Operation(
        summary = "Creates or updates an existing site in the input stage",
        description = "Create or update sites. " +
                "Updates instead of creating a new site if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of sites in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sites were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed site request", content = [Content()]),
        ]
    )
    @PutMapping("/input/sites")
    fun upsertSites(@RequestBody sites: Collection<SiteGateInputRequest>): ResponseEntity<Unit>

    @Operation(
        summary = "Returns site by external ID from the input stage",
        description = "Returns site by external ID from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with external ID"),
            ApiResponse(responseCode = "404", description = "No site found under specified external ID", content = [Content()])
        ]
    )
    @GetMapping("/input/sites/{externalId}")
    fun getSiteByExternalId(@Parameter(description = "External ID") @PathVariable externalId: String): SiteGateInputDto

    @Operation(
        summary = "Returns sites by an array of external IDs from the input stage",
        description = "Returns page of sites from the input stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/sites/search")
    fun getSitesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<SiteGateInputDto>

    @Operation(
        summary = "Returns sites from the input stage",
        description = "Returns page of sites from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/sites")
    fun getSites(@ParameterObject @Valid paginationRequest: PaginationRequest): PageDto<SiteGateInputDto>

    @Operation(
        summary = "Returns sites by an array of external IDs from the output stage",
        description = "Get page of sites from the output stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of sites"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/sites/search")
    fun getSitesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<SiteGateOutputResponse>

    @Operation(
        summary = "Creates or updates an existing site in the output stage",
        description = "Create or update sites (Output). " +
                "Updates instead of creating a new site if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of sites in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sites were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed site request", content = [Content()]),
        ]
    )
    @PutMapping("/output/sites")
    fun upsertSitesOutput(@RequestBody sites: Collection<SiteGateOutputRequest>): ResponseEntity<Unit>
}
