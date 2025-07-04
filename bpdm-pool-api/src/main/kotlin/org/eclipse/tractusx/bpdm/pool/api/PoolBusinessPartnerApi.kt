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
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSearchResultDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping(PoolBusinessPartnerApi.BUSINESS_PARTNERS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolBusinessPartnerApi {

    companion object{
        const val BUSINESS_PARTNERS_PATH = "${ApiCommons.BASE_PATH_V7}/business-partners"
    }

    @Operation(
        summary = "Return business partners look-up result",
        description = "Look-up the business partner data by parameters"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Matching business partners"),
            ApiResponse(responseCode = "400", description = "Invalid request (e.g., missing both ID and legalName)"),
        ]
    )
    @Tag(name = ApiCommons.BUSINESS_PARTNERS_NAME, description = ApiCommons.BUSINESS_PARTNERS_DESCRIPTION)
    @PostMapping(CommonApiPathNames.SUBPATH_SEARCH)
    fun searchBusinessPartners(
        @RequestBody searchRequest: LegalEntityPropertiesSearchRequest,
        @RequestParam searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultDto>
}