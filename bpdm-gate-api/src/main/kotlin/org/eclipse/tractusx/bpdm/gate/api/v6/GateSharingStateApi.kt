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

package org.eclipse.tractusx.bpdm.gate.api.v6

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateSharingStateApi {

    @Operation(
        summary = "Returns sharing states of business partners, optionally filtered by a business partner type and an array of external IDs"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of sharing states")
        ]
    )
    @GetMapping(value = [ApiCommons.SHARING_STATE_PATH_V6])
    fun getSharingStates(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @Parameter(description = "External IDs")
        @RequestParam(required = false) externalIds: Collection<String>?
    ): PageDto<SharingStateDto>
}