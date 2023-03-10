/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.entity.Site
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
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<SitePartnerResponse> {
        if (!legalEntityRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = siteRepository.findByLegalEntityBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByPartnerBpns(siteSearchRequest: SiteBpnSearchRequest, paginationRequest: PaginationRequest): PageResponse<SitePartnerSearchResponse> {
        val partners =
            if (siteSearchRequest.legalEntities.isNotEmpty()) legalEntityRepository.findDistinctByBpnIn(siteSearchRequest.legalEntities) else emptyList()
        val sitePage =
            siteRepository.findByLegalEntityInOrBpnIn(partners, siteSearchRequest.sites, PageRequest.of(paginationRequest.page, paginationRequest.size))
        fetchSiteDependencies(sitePage.toSet())
        return sitePage.toDto(sitePage.content.map { it.toWithReferenceDto() })
    }

    fun findByBpn(bpn: String): SitePartnerSearchResponse {
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return site.toWithReferenceDto()
    }

    private fun fetchSiteDependencies(sites: Set<Site>) {
        siteRepository.joinAddresses(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchPartnerAddressDependencies(addresses)
    }
}