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
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolChangelogApi.Companion.CHANGELOG_PATH
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(CHANGELOG_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolChangelogApi {
    companion object {
        const val CHANGELOG_PATH = "business-partners/changelog"
        const val SUBPATH_SEARCH = "/search"
    }

    @Operation(
        summary = "Returns changelog entries as of a specified timestamp, optionally filtered by a list of BPNL/S/A, or business partner types"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The specified changelog entries"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No business partner found for specified bpn", content = [Content()])
        ]
    )
    @Tag(name = ApiTags.CHANGELOG_NAME, description = ApiTags.CHANGELOG_DESCRIPTION)
    @PostMapping(SUBPATH_SEARCH)
    fun getChangelogEntries(
        @RequestBody changelogSearchRequest: ChangelogSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto>
}