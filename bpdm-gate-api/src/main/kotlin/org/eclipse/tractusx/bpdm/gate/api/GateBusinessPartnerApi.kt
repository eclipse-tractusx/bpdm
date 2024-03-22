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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateBusinessPartnerApi.Companion.BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(BUSINESS_PARTNER_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateBusinessPartnerApi {

    companion object{
        const val BUSINESS_PARTNER_PATH = ApiCommons.BASE_PATH
    }

    @Operation(
        summary = "Create or update business partner with given external ID",
        description = "Create or update generic business partner. " +
                "Updates instead of creating a new business partner if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of business partners in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Business partner were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping("/input/business-partners")
    fun upsertBusinessPartnersInput(@RequestBody businessPartners: Collection<BusinessPartnerInputRequest>): ResponseEntity<Collection<BusinessPartnerInputDto>>

    @Operation(
        summary = "Search business partner by external ID. An empty external ID list returns a paginated list of all business partners.",
        description = "Get page of business partners filtered by a collection of external IDs."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of busines partners"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/business-partners/search")
    fun getBusinessPartnersInput(
        @RequestBody externalIds: Collection<String>? = null,
        @ParameterObject @Valid paginationRequest: PaginationRequest = PaginationRequest()
    ): PageDto<BusinessPartnerInputDto>


    @Operation(
        summary = "Search business partners by an array of external IDs from the output stage",
        description = "Get page of business partners output data filtered by a collection of external IDs. " +
                "An empty external ID list will return a paginated list of all business partners."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of business partners"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/business-partners/search")
    fun getBusinessPartnersOutput(
        @RequestBody externalIds: Collection<String>? = null,
        @ParameterObject @Valid paginationRequest: PaginationRequest = PaginationRequest()
    ): PageDto<BusinessPartnerOutputDto>

}