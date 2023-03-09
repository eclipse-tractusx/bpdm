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

package org.eclipse.tractusx.bpdm.pool.client.service

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.response.SuggestionResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange


@RequestMapping("/api/catena/suggestions")
@HttpExchange("/api/catena/suggestions")
interface PoolClientSuggestionInterface {
    @Operation(
        summary = "Find best matches for given text in business partner names",
        description = "Performs search on business partner names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("legal-entities/names")
    @GetExchange("legal-entities/names")
    fun getNameSuggestion(
        @Parameter(description = "Show names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in business partner legal forms",
        description = "Performs search on legal form names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible legal form names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("legal-entities/legal-forms")
    @GetExchange("legal-entities/legal-forms")
    fun getLegalFormSuggestion(
        @Parameter(description = "Show legal form names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in business partner sites",
        description = "Performs search on site names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible site names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("sites/names")
    @GetExchange("sites/names")
    fun getSiteSuggestion(
        @Parameter(description = "Show site names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in business statuses",
        description = "Performs search on business status denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible business status denotations in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("legal-entities/statuses")
    @GetExchange("legal-entities/statuses")
    fun getStatusSuggestion(
        @Parameter(description = "Show business status denotations best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in business partner classifications",
        description = "Performs search on business partner classifications in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible business partner classifications in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("legal-entities/classifications")
    @GetExchange("legal-entities/classifications")
    fun getClassificationSuggestion(
        @Parameter(description = "Show business partner classifications best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>
    @Operation(
        summary = "Find best matches for given text in administrative areas",
        description = "Performs search on administrative area names in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible administrative area names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/administrative-areas")
    @GetExchange("/addresses/administrative-areas")
    fun getAdminAreaSuggestion(
        @Parameter(description = "Show administrative area names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in postcodes",
        description = "Performs search on postcode values in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible postcode values in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/postcodes")
    @GetExchange("/addresses/postcodes")
    fun getPostcodeSuggestion(
        @Parameter(description = "Show postcodes best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in localities",
        description = "Performs search on locality denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible locality names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/localities")
    @GetExchange("/addresses/localities")
    fun getLocalitySuggestion(
        @Parameter(description = "Show locality names this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in thoroughfares",
        description = "Performs search on thoroughfare denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible thoroughfare names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/thoroughfares")
    @GetExchange("/addresses/thoroughfares")
    fun getThoroughfareSuggestion(
        @Parameter(description = "Show thoroughfare names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in premises",
        description = "Performs search on premise denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible premise names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/premises")
    @GetExchange("/addresses/premises")
    fun getPremiseSuggestion(
        @Parameter(description = "Show premise names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>

    @Operation(
        summary = "Find best matches for given text in postal delivery points",
        description = "Performs search on postal delivery point denotations in order to find the best matches for the given text. " +
                "By specifying further request parameters the set of business partners to search in can be restricted. " +
                "If no text is given, the endpoint lists possible postal delivery point names in the search set.",
        responses = [ApiResponse(responseCode = "200", description = "Best matches found, may be empty")]
    )
    @GetMapping("/addresses/postal-delivery-points")
    @GetExchange("/addresses/postal-delivery-points")
    fun getPostalDeliverPointSuggestion(
        @Parameter(description = "Show postal delivery point names best matching this text") text: String?,
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<SuggestionResponse>
}