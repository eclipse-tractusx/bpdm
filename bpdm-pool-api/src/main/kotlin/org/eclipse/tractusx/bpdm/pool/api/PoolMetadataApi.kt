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

package org.eclipse.tractusx.bpdm.pool.api

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolMetadataApi {

    companion object DescriptionObject {
        const val technicalKeyDisclaimer =
            "The technical key can be freely chosen but needs to be unique for the businessPartnerType as it is used as reference by the business partner records. " +
                    "A recommendation for technical keys: They should be short, descriptive and " +
                    "use a restricted common character set in order to ensure compatibility with older systems."
    }

    @Operation(
        summary = "Creates a new identifier type",
        description = "Create a new identifier type (including validity details) which can be referenced by business partner records. " +
                "Identifier types such as BPN or VAT determine with which kind of values a business partner can be identified with. " +
                "The actual name of the identifier type is free to choose and doesn't need to be unique. $technicalKeyDisclaimer"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New identifier type successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "409", description = "Identifier type with specified technical key already exists", content = [Content()])
        ]
    )
    @PostMapping("/identifier-types")
    fun createIdentifierType(@RequestBody identifierType: IdentifierTypeDto): IdentifierTypeDto

    @Operation(
        summary = "Returns all identifier types filtered by business partner type and country.",
        description = "Lists all matching identifier types including validity details in a paginated result"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing identifier types, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/identifier-types")
    fun getIdentifierTypes(
        @ParameterObject paginationRequest: PaginationRequest,
        @Parameter businessPartnerType: IdentifierBusinessPartnerType,
        @Parameter country: CountryCode?
    ):
            PageDto<IdentifierTypeDto>


    @Operation(
        summary = "Creates a new legal form",
        description = "Create a new legal form which can be referenced by business partner records. " +
                "The actual name of the legal form is free to choose and doesn't need to be unique. " + technicalKeyDisclaimer
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New legal form successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "409", description = "Legal form with specified technical key already exists", content = [Content()])
        ]
    )
    @PostMapping("/legal-forms")
    fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormDto

    @Operation(
        summary = "Returns all legal forms",
        description = "Lists all currently known legal forms in a paginated result"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing legal forms, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/legal-forms")
    fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageDto<LegalFormDto>


    @Operation(
        summary = "Get all field quality rules filtered by country (specified by its ISO 3166-1 alpha-2 country code)",
        description = "List the country specific data rules for entity fields." +
                "All fields that are not in this list are considered to be forbidden."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of the existing rules for the given country"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/field-quality-rules/")
    fun getFieldQualityRules(@Parameter(description = "ISO 3166-1 alpha-2 country code") @RequestParam country: CountryCode): ResponseEntity<Collection<FieldQualityRuleDto>>

    @Operation(
        summary = "Create new Region",
        description = "Create a new region which can be referenced by business partner records.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New region successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),

        ]
    )
    @PostMapping("/regions")
    fun createRegion(@RequestBody type: RegionDto): RegionDto

    @Operation(
        summary = "Get page of regions",
        description = "Lists all currently known regions in a paginated result",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing regions, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/regions")
    fun getRegions(@ParameterObject paginationRequest: PaginationRequest): PageDto<RegionDto>

    @Operation(
        summary = "Get page of country subdivisions suitable for the administrativeAreaLevel1 address property",
        description = "Lists all currently known country subdivisions according to ISO 3166-2 in a paginated result"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing country subdivisions, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/administrative-areas-level1")
    fun getAdminAreasLevel1(@ParameterObject paginationRequest: PaginationRequest): PageDto<CountrySubdivisionDto>

}