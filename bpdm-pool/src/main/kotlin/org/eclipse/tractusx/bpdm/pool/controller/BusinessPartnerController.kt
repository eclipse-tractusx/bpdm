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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/catena/business-partners")
class BusinessPartnerController(
    private val partnerChangelogService: PartnerChangelogService,
    private val controllerConfigProperties: ControllerConfigProperties,
) {
    @Operation(
        summary = "Get business partner changelog entries",
        description = "Get business partner changelog entries by BPNs ignoring case and/or modification timestamp. " +
                "For a single request, the maximum number of BPN values to search for is limited to \${bpdm.controller.search-request-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The changelog entries for the specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed request", content = [Content()]),
        ]
    )
    @GetMapping("/changelog")
    fun getChangelogEntries(
        @Parameter(description = "BPN values (ignored if missing)") bpn: Array<String>?,
        @Parameter(description = "Modified after (ignored if missing)") modifiedAfter: Instant?,
        @ParameterObject paginationRequest: PaginationRequest
    ): ResponseEntity<PageResponse<ChangelogEntryResponse>> {
        if (bpn != null && bpn.size > controllerConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val response = partnerChangelogService.getChangelogEntriesByBpn(bpn, modifiedAfter, paginationRequest.page, paginationRequest.size)
        return ResponseEntity(response, HttpStatus.OK)
    }
}