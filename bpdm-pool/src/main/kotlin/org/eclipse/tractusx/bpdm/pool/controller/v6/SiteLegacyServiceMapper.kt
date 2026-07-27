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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6Dto
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.toV6
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteLegacyServiceMapper(
    private val siteRepository: SiteRepository,
    private val addressService: AddressService
) {

    private val logger = KotlinLogging.logger { }

    fun findByBpn(bpn: String): SiteWithMainAddressVerboseDtoV6 {
        logger.debug { "Executing findByBpn() with parameters $bpn " }
        val site = siteRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Site", bpn)
        return toPoolDto(site)
    }

    fun toPoolDto(entity: SiteDb): SiteWithMainAddressVerboseDtoV6 {
        return SiteWithMainAddressVerboseDtoV6(

            site = SiteVerboseDtoV6(
                entity.bpn,
                entity.name,
                states = entity.states.map { it.toDto().toV6() },
                bpnLegalEntity = entity.legalEntity.bpn,
                confidenceCriteria = entity.confidenceCriteria.toDto().toV6(),
                isCatenaXMemberData = entity.legalEntity.isCatenaXMemberData,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            ),
            mainAddress = entity.mainAddress.toV6Dto()
        )
    }

    /**
     * Search sites per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchSites(searchRequest: SiteSearchRequest, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDtoV6> {
        logger.debug { "Executing site search with request: $searchRequest" }
        val spec = Specification.allOf(
            SiteRepository.byBpns(searchRequest.siteBpns),
            SiteRepository.byParentBpns(searchRequest.legalEntityBpns),
            SiteRepository.byName(searchRequest.name),
            SiteRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val sitePage = siteRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        fetchSiteDependencies(sitePage.toSet())

        return sitePage.toDto(::toPoolDto)
    }

    private fun fetchSiteDependencies(sites: Set<SiteDb>) {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)
    }

    data class SiteSearchRequest(
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )
}
