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
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.LegalEntitySearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.LegalEntityResponseMapper
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntitySearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntitySearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest as LegalEntitySearchRequestDto

/**
 * The REST-API boundary for the V7 "search legal entities" operation.
 */
@Service
class LegalEntitySearchApplicationV7Service(
    private val legalEntitySearchParser: LegalEntitySearchParser,
    private val legalEntitySearchService: LegalEntitySearchService,
    private val legalEntitySearchRequestMapper: LegalEntitySearchRequestMapper,
    private val legalEntityResponseMapper: LegalEntityResponseMapper
) {

    /**
     * Returns the requested page of legal entities matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchLegalEntities(
        searchRequest: LegalEntitySearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null)

    /**
     * Returns the requested page of legal entities matching the given criteria, restricted to Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberLegalEntities(
        searchRequest: LegalEntitySearchRequestDto,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true)

    private fun search(searchRequest: LegalEntitySearchRequestDto, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        legalEntitySearchService.search(
            legalEntitySearchParser.parse(legalEntitySearchRequestMapper.toSearchRequest(searchRequest, isDataSpaceParticipant)),
            paginationRequest.toPageRequest()
        ).toPageDto { legalEntityResponseMapper.toLegalEntityWithLegalAddress(it) }
}
