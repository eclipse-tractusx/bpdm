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

import org.eclipse.tractusx.bpdm.common.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.eclipse.tractusx.bpdm.pool.entity.AddressPartner
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.AddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class AddressService(
    private val addressPartnerRepository: AddressPartnerRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
    private val addressRepository: AddressRepository
) {
    fun findByPartnerBpn(bpn: String, pageIndex: Int, pageSize: Int): PageResponse<AddressPartnerResponse> {
        if (!legalEntityRepository.existsByBpn(bpn)) {
            throw BpdmNotFoundException("Business Partner", bpn)
        }

        val page = addressPartnerRepository.findByLegalEntityBpn(bpn, PageRequest.of(pageIndex, pageSize))
        fetchPartnerAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toDto() })
    }

    fun findByBpn(bpn: String): AddressPartnerSearchResponse {
        val address = addressPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Address", bpn)
        return address.toDtoWithReference()
    }

    @Transactional
    fun findByPartnerAndSiteBpns(searchRequest: AddressPartnerSearchRequest, paginationRequest: PaginationRequest): PageResponse<AddressPartnerSearchResponse> {
        val partners = if (searchRequest.legalEntities.isNotEmpty()) legalEntityRepository.findDistinctByBpnIn(searchRequest.legalEntities) else emptyList()
        val sites = if (searchRequest.sites.isNotEmpty()) siteRepository.findDistinctByBpnIn(searchRequest.sites) else emptyList()

        val addressPage = addressPartnerRepository.findByLegalEntityInOrSiteIn(partners, sites, PageRequest.of(paginationRequest.page, paginationRequest.size))
        fetchPartnerAddressDependencies(addressPage.map { it }.toSet())
        // TODO: use [searchRequest.addresses]
        return addressPage.toDto(addressPage.content.map { it.toDtoWithReference() })
    }

    fun findLegalAddresses(bpnLs: Collection<String>): Collection<LegalAddressSearchResponse> {
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnLs)
        legalEntityRepository.joinLegalAddresses(legalEntities)
        val bpnAddressPairs = legalEntities.map { Pair(it.bpn, it.legalAddress) }
        fetchAddressDependencies(bpnAddressPairs.map { (_, legalAddress) -> legalAddress }.toSet())
        return bpnAddressPairs.map { (bpn, legalAddress) -> legalAddress.toLegalSearchResponse(bpn) }
    }

    fun findMainAddresses(bpnS: Collection<String>): Collection<MainAddressSearchResponse> {
        val sites = siteRepository.findDistinctByBpnIn(bpnS)
        siteRepository.joinAddresses(sites)
        val bpnAddressPairs = sites.map { Pair(it.bpn, it.mainAddress) }
        fetchAddressDependencies(bpnAddressPairs.map { (_, mainAddress) -> mainAddress }.toSet())
        return bpnAddressPairs.map { (bpn, legalAddress) -> legalAddress.toMainSearchResponse(bpn) }
    }

    fun fetchPartnerAddressDependencies(addressPartners: Set<AddressPartner>): Set<AddressPartner> {
        addressPartnerRepository.joinLegalEntities(addressPartners)
        addressPartnerRepository.joinSites(addressPartners)
        addressPartnerRepository.joinAddresses(addressPartners)

        fetchAddressDependencies(addressPartners.map { it.address }.toSet())

        return addressPartners
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