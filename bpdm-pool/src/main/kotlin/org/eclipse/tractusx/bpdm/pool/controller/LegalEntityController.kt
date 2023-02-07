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

package org.eclipse.tractusx.bpdm.pool.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.*
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/legal-entities")
class LegalEntityController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val businessPartnerBuildService: BusinessPartnerBuildService,
    val searchService: SearchService,
    val bpnConfigProperties: BpnConfigProperties,
    val siteService: SiteService,
    val addressService: AddressService
) {

    @Operation(
        summary = "Get page of legal entity business partners matching the search criteria",
        description = "This endpoint tries to find matches among all existing business partners of type legal entity, " +
                "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
                "The match of a partner is better the higher its relevancy score. " +
                "Note that when using search parameters the max page is \${bpdm.opensearch.max-page}."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @GetMapping
    fun getLegalEntities(
        @ParameterObject bpSearchRequest: LegalEntityPropertiesSearchRequest,
        @ParameterObject addressSearchRequest: AddressPropertiesSearchRequest,
        @ParameterObject siteSearchRequest: SitePropertiesSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {
        return searchService.searchLegalEntities(
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            paginationRequest
        )
    }

    @Operation(
        summary = "Get legal entity business partner by identifier",
        description = "This endpoint tries to find a business partner by the specified identifier. " +
                "The identifier value is case insensitively compared but needs to be given exactly. " +
                "By default the value given is interpreted as a BPN. " +
                "By specifying the technical key of another identifier type" +
                "the value is matched against the identifiers of that given type."
    )
    @ApiResponses(
        value = [
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
    fun getLegalEntity(
        @Parameter(description = "Identifier value") @PathVariable idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam
        idType: String?
    ): LegalEntityPartnerResponse {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findLegalEntityIgnoreCase(idValue.uppercase())
        else businessPartnerFetchService.findLegalEntityIgnoreCase(actualType, idValue)
    }

    @Operation(
        summary = "Confirms that the data of a legal entity business partner is still up to date.",
        description = "Confirms that the data of a business partner is still up to date " +
                "by saving the current timestamp at the time this POST-request is made as this business partner's \"currentness\". Ignores case of bpn."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Business partner's \"currentness\" successfully updated"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @PostMapping("/{bpn}/confirm-up-to-date")
    fun setLegalEntityCurrentness(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ) {
        businessPartnerBuildService.setBusinessPartnerCurrentness(bpn.uppercase())
    }

    @Operation(
        summary = "Search legal entity partners by BPNLs",
        description = "Search legal entity partners by their BPNLs. " +
                "The response can contain less results than the number of BPNLs that were requested, if some of the BPNLs did not exist. " +
                "For a single request, the maximum number of BPNLs to search for is limited to \${bpdm.bpn.search-request-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found legal entites"),
            ApiResponse(
                responseCode = "400",
                description = "On malformed request parameters or if number of requested bpns exceeds limit",
                content = [Content()]
            )
        ]
    )
    @PostMapping("/search")
    fun searchSites(
        @RequestBody bpnLs: Collection<String>
    ): ResponseEntity<Collection<LegalEntityPartnerResponse>> {
        if (bpnLs.size > bpnConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity(businessPartnerFetchService.fetchDtosByBpns(bpnLs), HttpStatus.OK)
    }

    @Operation(
        summary = "Get site partners of a legal entity",
        description = "Get business partners of type site belonging to a business partner of type legal entity, identified by the business partner's bpn ignoring case."
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
        return siteService.findByPartnerBpn(bpn.uppercase(), paginationRequest.page, paginationRequest.size)
    }

    @Operation(
        summary = "Get address partners of a legal entity",
        description = "Get business partners of type address belonging to a business partner of type legal entity, identified by the business partner's bpn ignoring case."
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
        return addressService.findByPartnerBpn(bpn.uppercase(), paginationRequest.page, paginationRequest.size)
    }

    @Operation(
        summary = "Search Legal Addresses",
        description = "Search legal addresses of legal entities by BPNL"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The found legal addresses"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ]
    )
    @PostMapping("/legal-addresses/search")
    fun searchLegalAddresses(
        @RequestBody
        bpnLs: Collection<String>
    ): Collection<LegalAddressSearchResponse> {
        return addressService.findLegalAddresses(bpnLs)
    }

    @Operation(
        summary = "Create new legal entity business partners",
        description = "Create new business partners of type legal entity. " +
                "The given additional identifiers of a record need to be unique, otherwise they are ignored. " +
                "For matching purposes, on each record you can specify your own index value which will reappear in the corresponding record of the response."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New legal entities request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
        ]
    )
    @PostMapping
    fun createBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPartnerCreateRequest>
    ): LegalEntityPartnerCreateResponseWrapper {
        return businessPartnerBuildService.createLegalEntities(businessPartners)
    }

    @Operation(
        summary = "Update existing legal entity business partners",
        description = "Update existing business partner records of type legal entity referenced via BPNL. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Update legal entities request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
        ]
    )
    @PutMapping
    fun updateBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPartnerUpdateRequest>
    ): LegalEntityPartnerUpdateResponseWrapper {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }

}