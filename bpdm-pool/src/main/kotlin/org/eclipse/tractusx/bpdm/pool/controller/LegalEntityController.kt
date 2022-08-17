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

    @PostMapping("/legal-addresses/search")
    fun searchLegalAddresses(
        @RequestBody
        bpnLs: Collection<String>
    ): Collection<LegalAddressSearchResponse> {
        return addressService.findLegalAddresses(bpnLs)
    }

    @Operation(
        summary = "Create new business partner record",
        description = "Endpoint to create new business partner records directly in the system. Currently for test purposes only."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New business partner record successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
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

    @PutMapping
    fun updateBusinessPartners(
        @RequestBody
        businessPartners: Collection<LegalEntityPoolUpdateRequest>
    ): Collection<LegalEntityPoolUpsertResponse> {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }


}