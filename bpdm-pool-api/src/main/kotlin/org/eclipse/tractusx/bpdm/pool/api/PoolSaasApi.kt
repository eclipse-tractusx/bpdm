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


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ImportIdEntry
import org.eclipse.tractusx.bpdm.pool.api.model.request.ImportIdFilterRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ImportIdMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SyncDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping


@RequestMapping("/api/saas", produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolSaasApi {

    @Operation(
        summary = "Import new business partner records from SaaS",
        description = "Triggers an asynchronous import of new business partner records from SaaS. " +
                "A SaaS record counts as new when it does not have a BPN and the BPDM service does not already have a record with the same SaaS ID. " +
                "This import only regards records with a modifiedAfter timestamp since the last import."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Import successfully started"),
            ApiResponse(responseCode = "409", description = "Import already running"),
            ApiResponse(responseCode = "500", description = "Import couldn't start to unexpected error", content = [Content()])
        ]
    )
    @PostMapping("/business-partner/sync")
    fun importBusinessPartners(): SyncDto

    @Operation(
        summary = "Fetch information about the SaaS synchronization",
        description = "Fetch information about the latest import (either ongoing or already finished)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Import information found"),
            ApiResponse(responseCode = "500", description = "Fetching failed (no connection to database)", content = [Content()])
        ]
    )
    @GetMapping("/business-partner/sync")
    fun getSyncStatus(): SyncDto

    @Operation(
        summary = "Filter Identifier Mappings by CX-Pool Identifiers",
        description = "Specify a range of CX-Pool Identifiers to get the corresponding mapping to their Business Partner Numbers"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The found import identifier mappings"),
            ApiResponse(responseCode = "400", description = "On malformed requests or exceeding the request size of \${bpdm.saas.request-size-limit}"),
        ]
    )
    @PostMapping("/identifier-mappings/filter")
    fun getImportEntries(@RequestBody importIdFilterRequest: ImportIdFilterRequest): ImportIdMappingDto

    @Operation(
        summary = "Paginate Identifier Mappings by CX-Pool Identifiers",
        description = "Paginate through all CX-Pool Identifier and Business Partner Number mappings."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The found import identifier mappings"),
            ApiResponse(responseCode = "400", description = "On malformed requests or exceeding the request size of \${bpdm.saas.request-size-limit}"),
        ]
    )
    @GetMapping("/identifier-mappings")
    fun getImportEntries(@ParameterObject paginationRequest: PaginationRequest): PageDto<ImportIdEntry>
}