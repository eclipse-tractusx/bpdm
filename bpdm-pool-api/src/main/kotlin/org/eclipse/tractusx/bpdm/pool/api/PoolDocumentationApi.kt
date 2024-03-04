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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolDocumentationApi {
    companion object {
        const val SUBPATH_MERMAID = "/mermaid/"

    }

    @Operation(
        summary = "Get mermaid class diagramm for the pool JPA model",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Entity model as mermaid diagramm"),
        ]
    )
    @GetMapping(SUBPATH_MERMAID, produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getMermaidPoolPersistence(): ResponseEntity<String>
}