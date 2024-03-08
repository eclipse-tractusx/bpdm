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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressService: AddressService
) {
    private val logger = KotlinLogging.logger { }

    fun findByParentBpn(bpn: String, pageIndex: Int, pageSize: Int): PageDto<SiteVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpn // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)

        val page = siteRepository.findByLegalEntity(legalEntity, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByPartnerBpns(siteSearchRequest: SiteBpnSearchRequest, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto> {
        logger.debug { "Executing findByPartnerBpns() with parameters $siteSearchRequest // $paginationRequest" }
        val parents =
            if (siteSearchRequest.legalEntities.isNotEmpty()) legalEntityRepository.findDistinctByBpnIn(siteSearchRequest.legalEntities) else emptyList()
        val sitePage =
            siteRepository.findByLegalEntityInOrBpnIn(parents, siteSearchRequest.sites, PageRequest.of(paginationRequest.page, paginationRequest.size))
        fetchSiteDependencies(sitePage.toSet())

        return sitePage.toDto(sitePage.content.map { it.toPoolDto() })
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

}