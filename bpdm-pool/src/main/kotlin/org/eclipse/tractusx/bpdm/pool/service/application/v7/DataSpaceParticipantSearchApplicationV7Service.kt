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
import org.eclipse.tractusx.bpdm.pool.api.model.DataSpaceParticipantDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.DataSpaceParticipantSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.DataSpaceParticipantResponseMapper
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.DataSpaceParticipantSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.DataSpaceParticipantSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantSearchRequest as DataSpaceParticipantSearchRequestDto

/**
 * The REST-API boundary for the V7 "search data space participants" operation.
 */
@Service
class DataSpaceParticipantSearchApplicationV7Service(
    private val dataSpaceParticipantSearchParser: DataSpaceParticipantSearchParser,
    private val dataSpaceParticipantSearchService: DataSpaceParticipantSearchService,
    private val dataSpaceParticipantSearchRequestMapper: DataSpaceParticipantSearchRequestMapper,
    private val dataSpaceParticipantResponseMapper: DataSpaceParticipantResponseMapper
) {

    /**
     * Returns the requested page of data space participations matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchParticipants(
        searchRequest: DataSpaceParticipantSearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<DataSpaceParticipantDto> {
        val criteria = dataSpaceParticipantSearchParser.parse(dataSpaceParticipantSearchRequestMapper.toSearchRequest(searchRequest))

        return dataSpaceParticipantSearchService.search(criteria, paginationRequest.toPageRequest())
            .toPageDto { dataSpaceParticipantResponseMapper.toParticipant(it) }
    }
}
