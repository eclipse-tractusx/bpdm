/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.entity.Site
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val siteRepository: SiteRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val addressService: AddressService
) {
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<SiteResponse> {
        if (!businessPartnerRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = siteRepository.findByPartnerBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByPartnerBpns(siteSearchRequest: SiteSearchRequest, paginationRequest: PaginationRequest): PageResponse<SiteWithReferenceResponse> {
        val partners =
            if (siteSearchRequest.legalEntities.isNotEmpty()) businessPartnerRepository.findDistinctByBpnIn(siteSearchRequest.legalEntities) else emptyList()
        val sitePage = siteRepository.findByPartnerIn(partners, PageRequest.of(paginationRequest.page, paginationRequest.size))
        fetchSiteDependencies(sitePage.toSet())
        return sitePage.toDto(sitePage.content.map { it.toDtoWithReference() })
    }

    fun findByBpn(bpn: String): SiteWithReferenceResponse {
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return site.toDtoWithReference()
    }

    private fun fetchSiteDependencies(sites: Set<Site>) {
        siteRepository.joinAddresses(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchAddressDependencies(addresses)
    }
}