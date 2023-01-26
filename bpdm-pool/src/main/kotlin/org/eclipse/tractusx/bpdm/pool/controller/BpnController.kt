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
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/bpn")
class BpnController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val controllerConfigProperties: ControllerConfigProperties
) {

    @Operation(
        summary = "Find business partner numbers by identifiers",
        description = "Find business partner numbers by identifiers. " +
                "The response can contain less results than the number of identifier values that were requested, if some of the identifiers did not exist. " +
                "For a single request, the maximum number of identifier values to search for is limited to \${bpdm.controller.search-request-limit} entries."
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
    @PostMapping("/search")
    fun findBpnsByIdentifiers(@RequestBody identifiersSearchRequest: IdentifiersSearchRequest): ResponseEntity<Set<BpnIdentifierMappingResponse>> {
        if (identifiersSearchRequest.idValues.size > controllerConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val bpnIdentifierMappings = businessPartnerFetchService.findBpnsByIdentifiers(identifiersSearchRequest.idType, identifiersSearchRequest.idValues)
        return ResponseEntity(bpnIdentifierMappings, HttpStatus.OK)
    }
}