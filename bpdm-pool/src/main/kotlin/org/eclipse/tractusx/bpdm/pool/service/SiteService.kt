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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteService(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressService: AddressService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Search sites per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchSites(searchRequest: SiteSearchRequest, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto>{
        logger.debug { "Executing site search with request: $searchRequest" }
        val spec = Specification.allOf(
            SiteRepository.byBpns(searchRequest.siteBpns),
            SiteRepository.byParentBpns(searchRequest.legalEntityBpns),
            SiteRepository.byName(searchRequest.name),
            SiteRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val sitePage = siteRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        fetchSiteDependencies(sitePage.toSet())

        return sitePage.toDto(SiteDb::toPoolDto)
    }

    fun findByParentBpn(bpn: String, pageIndex: Int, pageSize: Int): PageDto<SiteVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpn // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)

        val page = siteRepository.findByLegalEntity(legalEntity, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): SiteWithMainAddressVerboseDto {
        logger.debug { "Executing findByBpn() with parameters $bpn " }
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return site.toPoolDto()
    }

    private fun fetchSiteDependencies(sites: Set<SiteDb>) {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)
    }

    fun fetchSiteDependenciesPage(sites: Set<SiteDb>): Set<SiteDb> {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)

        return sites
    }

    data class SiteSearchRequest(
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )

}