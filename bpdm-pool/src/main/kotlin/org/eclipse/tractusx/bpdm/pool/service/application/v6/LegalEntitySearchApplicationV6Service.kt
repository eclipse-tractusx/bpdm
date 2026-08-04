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
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntitySearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6PoolDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.LegalEntitySearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntitySearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntitySearchParser
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search legal entities" operation, using the v6 request/response shapes.
 */
@Service
class LegalEntitySearchApplicationV6Service(
    private val legalEntitySearchParser: LegalEntitySearchParser,
    private val legalEntitySearchService: LegalEntitySearchService,
    private val legalEntitySearchRequestMapper: LegalEntitySearchRequestMapper
) {

    /**
     * Returns the requested page of legal entities matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchLegalEntities(
        searchRequest: LegalEntitySearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDtoV6> =
        search(searchRequest, paginationRequest, isCatenaXMemberData = null)

    /**
     * Returns the requested page of legal entities matching the given criteria, restricted to Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberLegalEntities(
        searchRequest: LegalEntitySearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDtoV6> =
        search(searchRequest, paginationRequest, isCatenaXMemberData = true)

    private fun search(searchRequest: LegalEntitySearchRequestV6, paginationRequest: PaginationRequest, isCatenaXMemberData: Boolean?) =
        legalEntitySearchService.search(
            legalEntitySearchParser.parse(legalEntitySearchRequestMapper.toSearchRequest(searchRequest.toV7(), isCatenaXMemberData)),
            paginationRequest.toPageRequest()
        ).toDto { it.toV6PoolDto() }
}
