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
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutputResponse
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
interface GateLegalEntityApi {

    @Operation(
        summary = "Create or update legal entities.",
        description = "Create or update legal entities. " +
                "Updates instead of creating a new legal entity if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of legal entities in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping("/input/legal-entities")
    @PutExchange("/input/legal-entities")
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit>

    @Operation(
        summary = "Get legal entity by external identifier",
        description = "Get legal entity by external identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found legal entity with external identifier"),
            ApiResponse(responseCode = "404", description = "No legal entity found under specified external identifier", content = [Content()])
        ]
    )
    @GetMapping("/input/legal-entities/{externalId}")
    @GetExchange("/input/legal-entities/{externalId}")
    fun getLegalEntityByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): LegalEntityGateInputResponse

    @Operation(
        summary = "Get page of legal-entities filtered by a collection of externalIds",
        description = "Get page of legal-entities filtered by a collection of externalIds."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal-entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/legal-entities/search")
    @PostExchange("/input/legal-entities/search")
    fun getLegalEntitiesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageResponse<LegalEntityGateInputResponse>

    @Operation(
        summary = "Get page of legal entities",
        description = "Get page of legal entities."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/legal-entities")
    @GetExchange("/input/legal-entities")
    fun getLegalEntities(@ParameterObject @Valid paginationRequest: PaginationRequest): PageResponse<LegalEntityGateInputResponse>

    @Operation(
        summary = "Get page of legal entities",
        description = "Get page of legal entities. Can optionally be filtered by external ids."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/legal-entities/search")
    @PostExchange("/output/legal-entities/search")
    fun getLegalEntitiesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageResponse<LegalEntityGateOutputResponse>

    @Operation(
        summary = "Create or update legal entities output.",
        description = "Create or update legal entities. " //TODO need better description
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping("/output/legal-entities")
    @PutExchange("/output/legal-entities")
    fun upsertLegalEntitiesOutput(@RequestBody legalEntities: Collection<LegalEntityGateInputRequest>): ResponseEntity<Unit>

}