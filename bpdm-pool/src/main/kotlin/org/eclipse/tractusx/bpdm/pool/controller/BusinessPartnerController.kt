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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolBusinessPartnerV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchFilterTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.BusinessPartnerSearchResultV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPropertiesSearchV7Request
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7BusinessPartnerMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessPartnerSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.BusinessPartnerSearchApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class BusinessPartnerController(
    private val businessPartnerSearchApplicationService: BusinessPartnerSearchApplicationService,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper
) : PoolBusinessPartnerV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun searchBusinessPartners(
        searchRequest: LegalEntityPropertiesSearchV7Request,
        searchResultFilter: Set<BusinessPartnerSearchFilterTypeV7>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultV7Dto> {
        val request = apiV7InboundMapper.toBusinessPartnerSearchRaw(searchRequest, searchResultFilter)
        return when (val result = businessPartnerSearchApplicationService.search(request, paginationRequest.toPageRequest())) {
            is BusinessPartnerSearchResult.Found -> result.page.toPageDto(apiV7BusinessPartnerMapper::toDto)
            is BusinessPartnerSearchResult.Rejected -> throw apiV7BusinessPartnerMapper.toSearchException(result.errors)
        }
    }
}
