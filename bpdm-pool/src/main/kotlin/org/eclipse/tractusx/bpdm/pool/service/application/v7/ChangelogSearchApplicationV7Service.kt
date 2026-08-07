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

package org.eclipse.tractusx.bpdm.pool.service.application.v7

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.ChangelogSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.ChangelogParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.ChangelogResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.ChangelogSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.ChangelogSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest as ChangelogSearchRequestDto

/**
 * The REST-API boundary for the V7 "search changelog entries" operation.
 */
@Service
class ChangelogSearchApplicationV7Service(
    private val changelogSearchParser: ChangelogSearchParser,
    private val changelogSearchService: ChangelogSearchService,
    private val changelogSearchRequestMapper: ChangelogSearchRequestMapper,
    private val changelogParseErrorMapper: ChangelogParseErrorMapper,
    private val changelogResponseMapper: ChangelogResponseMapper
) {

    /**
     * Returns the requested page of changelog entries matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchChangelogEntries(
        searchRequest: ChangelogSearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of changelog entries matching the given criteria, restricted to Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberChangelogEntries(
        searchRequest: ChangelogSearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true)

    private fun search(searchRequest: ChangelogSearchRequestDto, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        when (val criteria = changelogSearchParser.parse(changelogSearchRequestMapper.toSearchRequest(searchRequest, isDataSpaceParticipant))) {
            is ParseResult.Failure -> throw changelogParseErrorMapper.toSearchException(criteria.errors)
            is ParseResult.Success -> changelogSearchService.search(criteria.parsed, paginationRequest.toPageRequest())
                .toPageDto { changelogResponseMapper.toChangelogEntry(it) }
        }
}
