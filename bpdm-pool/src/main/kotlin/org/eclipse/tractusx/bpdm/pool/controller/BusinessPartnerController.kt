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
import org.eclipse.tractusx.bpdm.pool.api.PoolBusinessPartnerApi
import org.eclipse.tractusx.bpdm.pool.api.model.BusinessPartnerSearchFilterType
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerSearchResultDto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerSearchService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

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
@RestController
class BusinessPartnerController(
    private val businessPartnerSearchService: BusinessPartnerSearchService
): PoolBusinessPartnerApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.Companion.READ_PARTNER})")
    override fun searchBusinessPartners(
        searchRequest: LegalEntityPropertiesSearchRequest,
        searchResultFilter: Set<BusinessPartnerSearchFilterType>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerSearchResultDto> {

        fun String?.startsWithWhitespace(): Boolean = this?.firstOrNull()?.isWhitespace() == true

        val isAllSearchParamsEmpty = with(searchRequest) {
            listOf(id, legalName, street, city, postcode, country).all { it.isNullOrBlank() }
        }

        val isBpnAndLegalNameBothBlank = searchRequest.id.isNullOrBlank() && searchRequest.legalName.isNullOrBlank()
        val isFilterBlank = searchResultFilter.isNullOrEmpty()

        if (isAllSearchParamsEmpty || isBpnAndLegalNameBothBlank || isFilterBlank) {
            return PageDto(
                totalElements = 0,
                totalPages = 0,
                page = 0,
                contentSize = 0,
                content = emptyList()
            )
        }

        if (searchRequest.id.startsWithWhitespace() || searchRequest.legalName.startsWithWhitespace()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id and legal name cannot start with a space character.")
        }

        searchRequest.legalName
            ?.takeIf { it.length < 2 }
            ?.let {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "legal name length must be longer than 1 character.")
            }

        return businessPartnerSearchService.searchBusinessPartner(searchRequest, searchResultFilter, paginationRequest);
    }

}