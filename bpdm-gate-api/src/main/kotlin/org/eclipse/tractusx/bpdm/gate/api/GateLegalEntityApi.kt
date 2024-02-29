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
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateOutputResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateLegalEntityApi {

    @Operation(
        summary = "Creates or updates an existing legal entity in the input stage",
        description = "Create or update legal entities. " +
                "Updates instead of creating a new legal entity if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of legal entities in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping("/input/legal-entities")
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit>

    @Operation(
        summary = "Returns legal entity by external ID from the input stage",
        description = "Returns legal entity by external ID from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found legal entity with external ID"),
            ApiResponse(responseCode = "404", description = "No legal entity found under specified external ID", content = [Content()])
        ]
    )
    @GetMapping("/input/legal-entities/{externalId}")
    fun getLegalEntityByExternalId(@Parameter(description = "External ID") @PathVariable externalId: String): LegalEntityGateInputResponse

    @Operation(
        summary = "Returns legal entities by an array of external IDs from the input stage",
        description = "Returns page of legal entities from the input stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal-entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/legal-entities/search")
    fun getLegalEntitiesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<LegalEntityGateInputResponse>

    @Operation(
        summary = "Returns legal entities from the input stage",
        description = "Returns page of legal entities from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/legal-entities")
    fun getLegalEntities(@ParameterObject @Valid paginationRequest: PaginationRequest): PageDto<LegalEntityGateInputResponse>

    @Operation(
        summary = "Returns legal entities by an array of external IDs from the output stage",
        description = "Get page of legal entities from the output stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/legal-entities/search")
    fun getLegalEntitiesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<LegalEntityGateOutputResponse>

    @Operation(
        summary = "Creates or updates an existing legal entity in the output stage",
        description = "Create or update legal entities (Output). " +
                "Updates instead of creating a new legal entity if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of legal entities in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping("/output/legal-entities")
    fun upsertLegalEntitiesOutput(@RequestBody legalEntities: Collection<LegalEntityGateOutputRequest>): ResponseEntity<Unit>
}
