/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.api.v6

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnIdentifierMappingDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.BpnRequestIdentifierMappingDtoV6
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolBpnV6Api {

    @Operation(
        deprecated = true,
        summary = "Returns a list of identifier mappings of an identifier to a BPNL/A/S, specified by a business partner type, identifier type and identifier values",
        description = "Find business partner numbers by identifiers. " +
                "The response can contain less results than the number of identifier values that were requested, if some of the identifiers did not exist. " +
                "For a single request, the maximum number of identifier values to search for is limited to \${bpdm.bpn.search-request-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found bpn to identifier value mappings"),
            ApiResponse(
                responseCode = "400",
                description = "On malformed request parameters or if number of requested bpns exceeds limit",
                content = [Content()]
            ),
            ApiResponse(responseCode = "404", description = "Specified identifier type not found", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.BPN_NAME, description = ApiCommons.BPN_DESCRIPTION)
    @PostMapping(value = ["${ApiCommons.BPN_BASE_PATH_V6}${CommonApiPathNames.SUBPATH_SEARCH}"])
    fun findBpnsByIdentifiers(@RequestBody request: IdentifiersSearchRequestV6): ResponseEntity<Set<BpnIdentifierMappingDtoV6>>

    @Operation(
        deprecated = true,
        summary = "Return BPNL/S/A based on the requested identifiers",
        description = "Find business partner numbers by requested-identifiers."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found bpn to based on the requested identifiers"),
        ]
    )
    @Tag(name = ApiCommons.BPN_NAME, description = ApiCommons.BPN_DESCRIPTION)
    @PostMapping(value = ["${ApiCommons.BPN_BASE_PATH_V6}/request-ids/search"])
    fun findBpnByRequestedIdentifiers(@RequestBody request: BpnRequestIdentifierSearchRequestV6): ResponseEntity<Set<BpnRequestIdentifierMappingDtoV6>>
}
