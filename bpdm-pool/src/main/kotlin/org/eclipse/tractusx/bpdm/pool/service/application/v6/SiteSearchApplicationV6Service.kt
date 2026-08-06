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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6Dto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6PoolDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.SiteSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntitySiteSearchParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteSearchParser
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search sites" operation, using the v6 request/response shapes.
 */
@Service
class SiteSearchApplicationV6Service(
    private val siteSearchParser: SiteSearchParser,
    private val legalEntitySiteSearchParser: LegalEntitySiteSearchParser,
    private val siteSearchService: SiteSearchService,
    private val siteSearchRequestMapper: SiteSearchRequestMapper
) {

    /**
     * Returns the requested page of sites matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchSites(searchRequest: SiteSearchRequestV6, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDtoV6> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null).toDto { it.toV6PoolDto() }

    /**
     * Returns the requested page of sites matching the given criteria, restricted to those of Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberSites(searchRequest: SiteSearchRequestV6, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDtoV6> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true).toDto { it.toV6PoolDto() }

    /**
     * Returns the requested page of sites of the given legal entity, without their main addresses, and fails with a
     * not-found error when no legal entity carries that BPN.
     */
    @Transactional(readOnly = true)
    fun searchLegalEntitySites(bpnl: String, paginationRequest: PaginationRequest): PageDto<SiteVerboseDtoV6> {
        val criteria = when (val result = legalEntitySiteSearchParser.parse(siteSearchRequestMapper.toLegalEntitySitesRequest(bpnl))) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmNotFoundException("Business Partner", bpnl)
        }

        return siteSearchService.search(criteria, paginationRequest.toPageRequest()).toDto { it.toV6Dto() }
    }

    private fun search(searchRequest: SiteSearchRequestV6, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        siteSearchService.search(
            siteSearchParser.parse(siteSearchRequestMapper.toSearchRequest(searchRequest.toV7(), isDataSpaceParticipant)),
            paginationRequest.toPageRequest()
        )
}
