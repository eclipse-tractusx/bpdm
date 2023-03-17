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

package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.LsaType
import org.eclipse.tractusx.bpdm.gate.dto.response.PageChangeLogResponse
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntity
import org.eclipse.tractusx.bpdm.gate.service.ChangelogService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/catena/business-partners/changelog")
class ChangelogController (
    private val  changelogService: ChangelogService
    ) {

    @Operation(
        summary = "Get business partner changelog entries by list external id, from timestamp",
        description = "Get business partner changelog entries by list external id, from timestamp"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified parameters"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @PostMapping("/search")
    fun getChangelogEntriesExternalId(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @Parameter(description = "From Time", example = "2023-03-20T10:23:28.194Z") @RequestParam fromTime: Instant? ,
        @RequestBody externalIds: Collection<String>? = emptyList()
    ): PageChangeLogResponse<ChangelogEntity> {
        return changelogService.getChangeLogByExternalId(externalIds!!,fromTime,paginationRequest.page,paginationRequest.size)
    }

    @Operation(
        summary = "Get business partner changelog entries by timestamp or LSA type",
        description = "Get business partner changelog entries by from timestamp or LSA type"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified parameters"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @PostMapping("/filter")
    fun getChangelogEntriesLsaType(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @Parameter(description = "From Time", example = "2023-03-20T10:23:28.194Z") @RequestParam fromTime: Instant?,
        @Parameter(description = "LSA Type") @RequestParam lsaType: LsaType?
    ): PageResponse<ChangelogEntity> {
        return changelogService.getChangeLogByLsaType(lsaType,fromTime,paginationRequest.page,paginationRequest.size)
    }

    @PostMapping
    fun createChangelogEntries(
        @Parameter(description = "externalId") @RequestParam externalId: String
    ) {
        return changelogService.createChangelog(externalId)
    }
}