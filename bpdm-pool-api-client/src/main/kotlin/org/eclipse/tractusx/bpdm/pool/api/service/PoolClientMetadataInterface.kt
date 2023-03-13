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

package org.eclipse.tractusx.bpdm.pool.api.service

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.response.LegalFormResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.api.dto.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.api.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.dto.response.CountryIdentifierTypeResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange


@RequestMapping("/api/catena")
@HttpExchange("/api/catena")
interface PoolClientMetadataInterface {

    companion object DescriptionObject {
        const val technicalKeyDisclaimer =
            "The technical key can be freely chosen but needs to be unique as it is used as reference by the business partner records. " +
                    "A recommendation for technical keys: They should be short, descriptive and " +
                    "use a restricted common character set in order to ensure compatibility with older systems."
    }

    @Operation(
        summary = "Create new identifier type",
        description = "Create a new identifier type which can be referenced by business partner records. " +
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
    @PostExchange("/identifier-types")
    fun createIdentifierType(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String>

    @Operation(
        summary = "Get page of identifier types",
        description = "Lists all currently known identifier types in a paginated result"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing identifier types, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/identifier-types")
    @GetExchange("/identifier-types")
    fun getIdentifierTypes(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>>

    @Operation(
        summary = "Get valid identifier types for a country",
        description = "Returns which identifier types are valid and which ones are mandatory for a country specified by its ISO 3166-1 alpha-2 country code."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The identifier types"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/identifier-types/for-country")
    @GetExchange("/identifier-types/for-country")
    fun getValidIdentifierTypesForCountry(@Parameter @RequestParam country: CountryCode): Collection<CountryIdentifierTypeResponse>

    @Operation(
        summary = "Create new identifier status",
        description = "Create a new identifier status which can be referenced by business partner records. " +
                "A status further distinguishes an identifier by adding current status information such as active or revoked." +
                "The actual name of the identifier status is free to choose and doesn't need to be unique. $technicalKeyDisclaimer"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New identifier status successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "409", description = "Identifier status with specified technical key already exists", content = [Content()])
        ])
    @PostMapping("/identifier-status")
    @PostExchange("/identifier-status")
    fun createIdentifierStatus(@RequestBody status: TypeKeyNameDto<String>): TypeKeyNameDto<String>

    @Operation(summary = "Get page of identifier statuses",
        description = "Lists all currently known identifier statuses in a paginated result")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of existing identifier statuses, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
    ])
    @GetMapping("/identifier-status")
    @GetExchange("/identifier-status")
    fun getIdentifierStati(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>>
    @Operation(
        summary = "Create new issuing body",
        description = "Create a new issuing body which can be referenced by business partner records. " +
                "An issuing body should be an entity which the Catena organisation trusts to issue identifiers." +
                "The actual name of the issuing body is free to choose and doesn't need to be unique. $technicalKeyDisclaimer"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New issuing body successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "409", description = "Issuing body with specified technical key already exists", content = [Content()])
        ]
    )
    @PostMapping("/issuing-bodies")
    @PostExchange("/issuing-bodies")
    fun createIssuingBody(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String>

    @Operation(summary = "Get page of issuing bodies",
        description = "Lists all currently known issuing bodies in a paginated result")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing issuing bodies, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/issuing-bodies")
    @GetExchange("/issuing-bodies")
    fun getIssuingBodies(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>>

    @Operation(
        summary = "Create new legal form",
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
    @PostExchange("/legal-forms")
    fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormResponse


    @Operation(summary = "Get page of legal forms",
        description = "Lists all currently known legal forms in a paginated result")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of existing legal forms, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @GetMapping("/legal-forms")
    @GetExchange("/legal-forms")
    fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageResponse<LegalFormResponse>
}