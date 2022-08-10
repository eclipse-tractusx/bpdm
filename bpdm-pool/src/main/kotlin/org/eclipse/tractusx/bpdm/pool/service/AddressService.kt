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

import org.eclipse.tractusx.bpdm.common.dto.response.AddressBpnResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.eclipse.tractusx.bpdm.pool.repository.AddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class AddressService(
    private val addressRepository: AddressRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val siteRepository: SiteRepository
) {
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<AddressBpnResponse> {
        if (!businessPartnerRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = addressRepository.findByPartnerBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchAddressDependencies(page.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): AddressWithReferenceResponse {
        val address = addressRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDtoWithReference()
    }

    @Transactional
    fun findByPartnerAndSiteBpns(searchRequest: AddressSearchRequest, paginationRequest: PaginationRequest): PageResponse<AddressWithReferenceResponse> {
        val partners = if (searchRequest.legalEntities.isNotEmpty()) businessPartnerRepository.findDistinctByBpnIn(searchRequest.legalEntities) else emptyList()
        val sites = if (searchRequest.sites.isNotEmpty()) siteRepository.findDistinctByBpnIn(searchRequest.sites) else emptyList()

        val addressPage = addressRepository.findByPartnerInOrSiteIn(partners, sites, PageRequest.of(paginationRequest.page, paginationRequest.size))
        fetchAddressDependencies(addressPage.toSet())
        return addressPage.toDto(addressPage.content.map { it.toDtoWithReference() })
    }

    fun fetchAddressDependencies(addresses: Set<Address>): Set<Address> {
        addressRepository.joinContexts(addresses)
        addressRepository.joinTypes(addresses)
        addressRepository.joinVersions(addresses)
        addressRepository.joinAdminAreas(addresses)
        addressRepository.joinPostCodes(addresses)
        addressRepository.joinLocalities(addresses)
        addressRepository.joinPremises(addresses)
        addressRepository.joinPostalDeliveryPoints(addresses)
        addressRepository.joinThoroughfares(addresses)

        return addresses
    }
}