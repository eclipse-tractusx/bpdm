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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Search addresses per page for [searchRequest] and [paginationRequest]
     */
    @org.springframework.transaction.annotation.Transactional
    fun searchAddresses(searchRequest: AddressSearchRequest, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto>{

        val spec = Specification.allOf(
            LogisticAddressRepository.byBpns(searchRequest.addressBpns),
            LogisticAddressRepository.bySiteBpns(searchRequest.siteBpns),
            LogisticAddressRepository.byLegalEntityBpns(searchRequest.legalEntityBpns),
            LogisticAddressRepository.byName(searchRequest.name),
            LogisticAddressRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )
        val addressPage = logisticAddressRepository.findAll(spec, paginationRequest.toPageRequest())

        return addressPage.toDto { it.toDto() }
    }

    /**
     * Find Addresses which directly belong to a Legal Entity
     */
    fun findNonSiteAddressesOfLegalEntity(bpnl: String, pageIndex: Int, pageSize: Int): PageDto<LogisticAddressVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpnl // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpnl) ?:  throw BpdmNotFoundException("Business Partner", bpnl)

        val page = logisticAddressRepository.findByLegalEntityAndSiteIsNull(legalEntity, PageRequest.of(pageIndex, pageSize))
        fetchLogisticAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): LogisticAddressVerboseDto {
        val address = findAddressByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDto()
    }

    fun fetchLogisticAddressDependencies(addresses: Set<LogisticAddressDb>): Set<LogisticAddressDb> {
        logisticAddressRepository.joinLegalEntities(addresses)
        logisticAddressRepository.joinSites(addresses)
        logisticAddressRepository.joinRegions(addresses)
        logisticAddressRepository.joinIdentifiers(addresses)
        logisticAddressRepository.joinStates(addresses)

        return addresses
    }

    data class AddressSearchRequest(
        val addressBpns: List<String>?,
        val siteBpns: List<String>?,
        val legalEntityBpns: List<String>?,
        val name: String?,
        val isCatenaXMemberData: Boolean?
    )

    fun findAddressByBpn(bpn: String): LogisticAddressDb? {
        logger.debug { "Executing findAddressByBpn() with parameters $bpn" }
        return logisticAddressRepository.findByBpn(bpn)
    }
}