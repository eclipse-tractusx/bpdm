/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/catena")
class SiteController {
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
    fun upsertSites(@RequestBody sites: Collection<SiteGateInput>): ResponseEntity<Any> {
        TODO()
    }

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
    fun getSiteByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): SiteGateInput {
        TODO()
    }

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
    fun getSites(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<SiteGateInput> {
        TODO()
    }

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
    @GetMapping("/output/sites")
    fun getSitesOutput(
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @Parameter(description = "Only show sites that were updated after the specified ISO-8601 timestamp") from: Instant?
    ): PageStartAfterResponse<SiteGateOutput> {
        TODO()
    }

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
    @GetMapping("/output/sites/{externalId}")
    fun getSiteByExternalIdOutput(@Parameter(description = "External identifier") @PathVariable externalId: String): SiteGateOutput {
        TODO()
    }
}