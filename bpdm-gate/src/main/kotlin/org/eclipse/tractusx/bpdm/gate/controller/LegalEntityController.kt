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
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.eclipse.tractusx.bpdm.gate.service.ValidationService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/catena")
class LegalEntityController(
    val legalEntityService: LegalEntityService,
    val apiConfigProperties: ApiConfigProperties,
    val validationService: ValidationService
) {

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
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityGateInput>): ResponseEntity<Any> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || legalEntities.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntities(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

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
    fun getLegalEntityByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): LegalEntityGateInput {
        return legalEntityService.getLegalEntityByExternalId(externalId)
    }

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
    fun getLegalEntities(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<LegalEntityGateInput> {
        return legalEntityService.getLegalEntities(paginationRequest.limit, paginationRequest.startAfter)
    }

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
    fun getLegalEntitiesOutput(
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageStartAfterResponse<LegalEntityGateOutput> {
        return legalEntityService.getLegalEntitiesOutput(externalIds, paginationRequest.limit, paginationRequest.startAfter)
    }

    @Operation(
        summary = "Validate a legal entity",
        description = "Determines errors in a legal entity record which keep it from entering the sharing process"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "A validation response with possible errors"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity requests", content = [Content()]),
        ]
    )
    @PostMapping("/input/legal-entities/validation")
    fun validateLegalEntity(
        @RequestBody legalEntityInput: LegalEntityGateInput
    ): ValidationResponse {
        return validationService.validate(legalEntityInput)
    }

}