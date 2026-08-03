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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.ChangelogSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ChangelogEntryVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service

@Service
class ChangelogLegacyServiceMapper(
    private val partnerChangelogService: PartnerChangelogService,
    private val controllerConfigProperties: ControllerConfigProperties
) {

    fun getChangelogEntries(
        changelogSearchRequest: ChangelogSearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDtoV6> {

        changelogSearchRequest.bpns?.let { bpns ->
            if (bpns.size > controllerConfigProperties.searchRequestLimit) {
                throw BpdmRequestSizeException(bpns.size, controllerConfigProperties.searchRequestLimit)
            }
        }

        val page = partnerChangelogService.getChangeLogEntries(
            bpns = changelogSearchRequest.bpns,
            businessPartnerTypes = changelogSearchRequest.businessPartnerTypes,
            fromTime = changelogSearchRequest.timestampAfter,
            isCatenaXMemberData = null,
            pageIndex = paginationRequest.page,
            pageSize = paginationRequest.size
        )

        return PageDto(
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.page,
            contentSize = page.contentSize,
            content = page.content.map { it.toV6() }
        )
    }
}
