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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalEntityPoolUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPoolUpsertResponse
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/legal-entities")
class LegalEntityController(
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val addressService: AddressService
) {

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
            ApiResponse(responseCode = "200", description = "New business partner record successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Metadata referenced by technical key not found", content = [Content()])
        ]
    )
    @PostMapping
    fun createBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityCreateRequest>
    ): Collection<LegalEntityPoolUpsertResponse> {
        return businessPartnerBuildService.createLegalEntities(businessPartners)
    }

    @Operation(
        summary = "Update existing legal entity business partners",
        description = "Update existing business partner records of type legal entity referenced via BPNL. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The successfully updated records"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
            ApiResponse(responseCode = "404", description = "Metadata referenced by technical key not found", content = [Content()])
        ]
    )
    @PutMapping
    fun updateBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPoolUpdateRequest>
    ): Collection<LegalEntityPoolUpsertResponse> {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }


}