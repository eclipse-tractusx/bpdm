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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.inbound.SiteSearchRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntitySiteSearchParser
import org.eclipse.tractusx.bpdm.pool.service.parser.SiteSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest as SiteSearchRequestDto

/**
 * The REST-API boundary for the V7 "search sites" operation.
 */
@Service
class SiteSearchApplicationV7Service(
    private val siteSearchParser: SiteSearchParser,
    private val legalEntitySiteSearchParser: LegalEntitySiteSearchParser,
    private val siteSearchService: SiteSearchService,
    private val siteSearchRequestMapper: SiteSearchRequestMapper,
    private val siteResponseMapper: SiteResponseMapper
) {

    /**
     * Returns the requested page of sites matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchSites(searchRequest: SiteSearchRequestDto, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = null).toPageDto { siteResponseMapper.toSiteWithMainAddress(it) }

    /**
     * Returns the requested page of sites matching the given criteria, restricted to those of Catena-X members.
     */
    @Transactional(readOnly = true)
    fun searchMemberSites(searchRequest: SiteSearchRequestDto, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto> =
        search(searchRequest, paginationRequest, isDataSpaceParticipant = true).toPageDto { siteResponseMapper.toSiteWithMainAddress(it) }

    /**
     * Returns the requested page of sites of the given legal entity, without their main addresses, and fails with a
     * not-found error when no legal entity carries that BPN.
     */
    @Transactional(readOnly = true)
    fun searchLegalEntitySites(bpnl: String, paginationRequest: PaginationRequest): PageDto<SiteVerboseDto> {
        val criteria = when (val result = legalEntitySiteSearchParser.parse(siteSearchRequestMapper.toLegalEntitySitesRequest(bpnl))) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmNotFoundException("Business Partner", bpnl)
        }

        return siteSearchService.search(criteria, paginationRequest.toPageRequest()).toPageDto { siteResponseMapper.toSite(it) }
    }

    private fun search(searchRequest: SiteSearchRequestDto, paginationRequest: PaginationRequest, isDataSpaceParticipant: Boolean?) =
        siteSearchService.search(
            siteSearchParser.parse(siteSearchRequestMapper.toSearchRequest(searchRequest, isDataSpaceParticipant)),
            paginationRequest.toPageRequest()
        )
}
