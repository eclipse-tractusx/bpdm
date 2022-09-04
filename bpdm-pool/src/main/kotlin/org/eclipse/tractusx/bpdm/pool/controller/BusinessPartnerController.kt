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

package org.eclipse.tractusx.bpdm.pool.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.*
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.service.*
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/business-partner")
class BusinessPartnerController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val businessPartnerBuildService: BusinessPartnerBuildService,
    val searchService: SearchService,
    val bpnConfigProperties: BpnConfigProperties,
    val partnerChangelogService: PartnerChangelogService,
    val siteService: SiteService,
    val addressService: AddressService
) {

    @Operation(
        summary = "Get page of business partners matching the search criteria",
        description = "This endpoint tries to find matches among all existing business partners, " +
                "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
                "The match of a partner is better the higher its relevancy score."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @GetMapping
    fun getBusinessPartners(
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {
        return searchService.searchBusinessPartners(
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            paginationRequest
        )
    }

    @Operation(
        summary = "Get business partner by identifier",
        description = "This endpoint tries to find a business partner by the specified identifier. " +
                "The identifier value is case insensitively compared but needs to be given exactly. " +
                "By default the value given is interpreted as a BPN. " +
                "By specifying the technical key of another identifier type" +
                "the value is matched against the identifiers of that given type."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found business partner with specified identifier"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(
            responseCode = "404",
            description = "No business partner found under specified identifier or specified identifier type not found",
            content = [Content()]
        )
    ]
    )
    @GetMapping("/{idValue}")
    fun getBusinessPartner(
        @Parameter(description = "Identifier value") @PathVariable idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam
        idType: String?
    ): LegalEntityPartnerResponse {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findPartner(idValue)
        else businessPartnerFetchService.findPartnerByIdentifier(actualType, idValue)
    }

    @Operation(
        summary = "Confirms that the data of a business partner is still up to date.",
        description = "Confirms that the data of a business partner is still up to date " +
                "by saving the current timestamp at the time this POST-request is made as this business partner's \"currentness\"."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Business partner's \"currentness\" successfully updated"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @PostMapping("/{bpn}/confirm-up-to-date")
    fun setBusinessPartnerCurrentness(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ) {
        businessPartnerBuildService.setBusinessPartnerCurrentness(bpn)
    }

    @Operation(
        summary = "Get business partner changelog entries by bpn",
        description = "Get business partner changelog entries by bpn."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}/changelog")
    fun getChangelogEntries(
        @Parameter(description = "Bpn value") @PathVariable bpn: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<ChangelogEntryResponse> {
        return partnerChangelogService.getChangelogEntriesByBpn(bpn, paginationRequest.page, paginationRequest.size)
    }

    @Operation(
        summary = "Get sites of a business partner",
        description = "Get sites for a business partner, identified by the business partner's bpn."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The sites for the specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}/sites")
    fun getSites(
        @Parameter(description = "Bpn value") @PathVariable bpn: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<SitePartnerResponse> {
        return siteService.findByPartnerBpn(bpn, paginationRequest.page, paginationRequest.size)
    }

    @Operation(
        summary = "Get addresses of a business partner",
        description = "Get addresses for a business partner, identified by the business partner's bpn."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The addresses for the specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}/addresses")
    fun getAddresses(
        @Parameter(description = "Bpn value") @PathVariable bpn: String,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<AddressPartnerResponse> {
        return addressService.findByPartnerBpn(bpn, paginationRequest.page, paginationRequest.size)
    }

}