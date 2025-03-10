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

package org.eclipse.tractusx.orchestrator.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginRequest
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

const val TagOrigin = "Origin Registrar"

@RequestMapping(OriginatorRegistrarApi.PRIORITY_INDICATOR_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface OriginatorRegistrarApi {
    companion object{
        const val PRIORITY_INDICATOR_PATH = "${ApiCommons.BASE_PATH}/register/origin"
    }

    @Operation(
        summary = "Register Gate components along with their priority levels",
        description = "This endpoint allows you to register Gate components, specifying their priority and threshold values."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Return the registered gate information."
            ),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()]),
        ]
    )
    @Tag(name = TagOrigin)
    @PostMapping
    fun registerOrigin(@RequestBody request: UpsertOriginRequest): UpsertOriginResponse

    @Operation(
        summary = "Retrieve registered Gate components using the originId",
        description = "This endpoint enables fetching details of registered Gate components."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns the details of the registered Gate components."
            ),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @Tag(name = TagOrigin)
    @GetMapping("/{originId}")
    fun fetchOrigin(@PathVariable("originId") originId: String): UpsertOriginResponse

    @Operation(
        summary = "Retrieve registered Gate components using the originId",
        description = "This endpoint enables fetching details of registered Gate components."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returns the details of the registered Gate components."
            ),
        ]
    )
    @Tag(name = TagOrigin)
    @PutMapping("/{originId}")
    fun updateOrigin(@PathVariable("originId") originId: String,
                     @RequestBody request: UpsertOriginRequest): UpsertOriginResponse
}