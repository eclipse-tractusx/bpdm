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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.controller


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/opensearch")
class OpenSearchController(
    val openSearchSyncService: OpenSearchSyncStarterService
) {

    @Operation(
        summary = "Index new business partner records on OpenSearch",
        description = "Triggers an asynchronous export of business partner records from BPDM to OpenSearch. " +
                "Only exports records which have been updated since the last export. "
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export of records successfully"),
            ApiResponse(responseCode = "500", description = "Export failed (no connection to OpenSearch or database)", content = [Content()])
        ]
    )
    @PostMapping("/business-partner")
    fun export(): SyncResponse {
        return openSearchSyncService.exportAsync()
    }

    @Operation(
        summary = "Fetch information about the latest OpenSearch export",
        description = "Fetch information about the latest export (either ongoing or already finished)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export information found"),
            ApiResponse(responseCode = "500", description = "Fetching failed (no connection to database)", content = [Content()])
        ]
    )
    @GetMapping("/business-partner")
    fun getBusinessPartners(): SyncResponse {
        return openSearchSyncService.getExportStatus()
    }


    @Operation(
        summary = "Clear business partner index on OpenSearch",
        description = "Deletes all business partner records in the OpenSearch index. " +
                "Also resets the timestamp from the last export."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Index successfully cleared"),
            ApiResponse(responseCode = "500", description = "Clearing failed (no connection to OpenSearch or database)", content = [Content()])
        ]
    )
    @DeleteMapping("/business-partner")
    fun clear() {
        openSearchSyncService.clearOpenSearch()
    }
}