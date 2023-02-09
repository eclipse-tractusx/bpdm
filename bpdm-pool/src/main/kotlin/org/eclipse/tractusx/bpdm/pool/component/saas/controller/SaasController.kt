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

package org.eclipse.tractusx.bpdm.pool.component.saas.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.component.saas.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cdq")
class SaasController(
    val partnerImportService: ImportStarterService
) {
    @Operation(
        summary = "Import new business partner records from CDQ",
        description = "Triggers an asynchronous import of new business partner records from CDQ. " +
                "A CDQ record counts as new when it does not have a BPN and the BPDM service does not already have a record with the same CDQ ID. " +
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
    fun importBusinessPartners() : SyncResponse {
        return partnerImportService.importAsync()
    }

    @Operation(
        summary = "Fetch information about the CDQ synchronization",
        description = "Fetch information about the latest import (either ongoing or already finished)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Import information found"),
            ApiResponse(responseCode = "500", description = "Fetching failed (no connection to database)", content = [Content()])
        ]
    )
    @GetMapping("/business-partner/sync")
    fun getSyncStatus(): SyncResponse {
        return partnerImportService.getImportStatus()
    }
}