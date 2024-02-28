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

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.MainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
) {
    private val logger = KotlinLogging.logger { }

    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageDto<LogisticAddressVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpn // $pageIndex // $pageSize" }
        if (!legalEntityRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = logisticAddressRepository.findByLegalEntityBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchLogisticAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): LogisticAddressVerboseDto {
        logger.debug { "Executing findByBpn() with parameters $bpn" }
        val address = logisticAddressRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDto()
    }

    @Transactional
    fun findByPartnerAndSiteBpns(
        searchRequest: AddressPartnerBpnSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        logger.debug { "Executing findByPartnerAndSiteBpns() with parameters $searchRequest and $paginationRequest" }

        val partners = if (searchRequest.legalEntities.isNotEmpty()) legalEntityRepository.findDistinctByBpnIn(searchRequest.legalEntities) else emptyList()
        val sites = if (searchRequest.sites.isNotEmpty()) siteRepository.findDistinctByBpnIn(searchRequest.sites) else emptyList()

        val addressPage = logisticAddressRepository.findByLegalEntityInOrSiteInOrBpnIn(
            legalEntities = partners,
            sites = sites,
            bpns = searchRequest.addresses,
            pageable = PageRequest.of(paginationRequest.page, paginationRequest.size)
        )
        fetchLogisticAddressDependencies(addressPage.map { it }.toSet())
        return addressPage.toDto(addressPage.content.map { it.toDto() })
    }

    fun findLegalAddresses(bpnLs: Collection<String>): Collection<LegalAddressVerboseDto> {
        logger.debug { "Executing findLegalAddresses() with parameters $bpnLs" }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnLs)
        legalEntityRepository.joinLegalAddresses(legalEntities)
        val addresses = legalEntities.map { it.legalAddress }
        fetchLogisticAddressDependencies(addresses.toSet())
        return addresses.map { it.toLegalAddressResponse() }
    }

    fun findMainAddresses(bpnS: Collection<String>): Collection<MainAddressVerboseDto> {
        logger.debug { "Executing findMainAddresses() with parameters $bpnS" }
        val sites = siteRepository.findDistinctByBpnIn(bpnS)
        siteRepository.joinAddresses(sites)
        val addresses = sites.map { it.mainAddress }
        fetchLogisticAddressDependencies(addresses.toSet())
        return addresses.map { it.toMainAddressResponse() }
    }

    fun fetchLogisticAddressDependencies(addresses: Set<LogisticAddressDb>): Set<LogisticAddressDb> {
        logisticAddressRepository.joinLegalEntities(addresses)
        logisticAddressRepository.joinSites(addresses)
        logisticAddressRepository.joinRegions(addresses)
        logisticAddressRepository.joinIdentifiers(addresses)
        logisticAddressRepository.joinStates(addresses)

        return addresses
    }
}