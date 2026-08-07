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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.ChangelogSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ChangelogEntryVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.ChangelogSearchRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.ChangelogResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound.ChangelogParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.ChangelogSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.ChangelogSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search changelog entries" operation, using the v6 request/response shapes.
 */
@Service
class ChangelogSearchApplicationV6Service(
    private val changelogSearchParser: ChangelogSearchParser,
    private val changelogSearchService: ChangelogSearchService,
    private val changelogSearchRequestMapperV6: ChangelogSearchRequestMapperV6,
    private val changelogParseErrorMapper: ChangelogParseErrorMapper,
    private val changelogResponseMapperV6: ChangelogResponseMapperV6
) {

    /**
     * Returns the requested page of changelog entries matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchChangelogEntries(
        searchRequest: ChangelogSearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDtoV6> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of changelog entries matching the given criteria, restricted to Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberChangelogEntries(
        searchRequest: ChangelogSearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDtoV6> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true)

    private fun search(searchRequest: ChangelogSearchRequestV6, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        when (val criteria = changelogSearchParser.parse(changelogSearchRequestMapperV6.toSearchRequest(searchRequest, isDataSpaceParticipant))) {
            is ParseResult.Failure -> throw changelogParseErrorMapper.toSearchException(criteria.errors)
            is ParseResult.Success -> changelogSearchService.search(criteria.parsed, paginationRequest.toPageRequest())
                .toPageDto { changelogResponseMapperV6.toChangelogEntry(it) }
        }
}
