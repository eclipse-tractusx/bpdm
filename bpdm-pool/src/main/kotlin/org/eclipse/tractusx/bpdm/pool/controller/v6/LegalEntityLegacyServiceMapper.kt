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
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.toV6Dto
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LegalEntityLegacyServiceMapper(
    private val legalEntityRepository: LegalEntityRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val siteRepository: SiteRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val addressService: AddressService
) {

    companion object {
        // Retained for the v6 legacy tests that build the expected "too many identifiers" message; the live limit now
        // lives in org.eclipse.tractusx.bpdm.pool.util.ValidationLimits.
        const val IDENTIFIER_AMOUNT_LIMIT = 100
    }

    private val logger = KotlinLogging.logger { }

    /**
     * Search legal entities per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchLegalEntities(searchRequest: LegalEntitySearchRequest, paginationRequest: PaginationRequest): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        val spec = Specification.allOf(
            LegalEntityRepository.byBpns(searchRequest.bpnLs),
            LegalEntityRepository.byLegalName(searchRequest.legalName),
            LegalEntityRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val legalEntityPage = legalEntityRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toDto(::toLegalEntityWithLegalAddress)
    }

    /**
     * Fetch a business partner by [bpn] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    fun findLegalEntityIgnoreCase(bpn: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $bpn" }
        val legalEntity = findLegalEntityOrThrow(bpn)
        return toLegalEntityWithLegalAddress(legalEntity)
    }

    /**
     * Fetch a business partner by [identifierValue] (ignoring case) of [identifierType] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    @Transactional
    fun findLegalEntityIgnoreCase(identifierType: String, identifierValue: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $identifierType and $identifierValue" }
        val legalEntity = findLegalEntityOrThrow(identifierType, identifierValue)
        return toLegalEntityWithLegalAddress(legalEntity)
    }

    private fun findLegalEntityOrThrow(bpn: String): LegalEntityDb {
        return legalEntityRepository.findByBpnIgnoreCase(bpn) ?: throw BpdmNotFoundException(LegalEntityDb::class.simpleName!!, bpn)
    }

    fun findLegalEntityOrThrow(identifierTypeKey: String, identifierValue: String): LegalEntityDb {
        val identifierType = findIdentifierTypeOrThrow(identifierTypeKey, IdentifierBusinessPartnerType.LEGAL_ENTITY)
        return legalEntityRepository.findByIdentifierTypeAndValueIgnoreCase(identifierType, identifierValue)
            ?: throw BpdmNotFoundException("Identifier Value", identifierValue)
    }

    private fun findIdentifierTypeOrThrow(identifierTypeKey: String, businessPartnerType: IdentifierBusinessPartnerType) =
        identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKey(businessPartnerType, identifierTypeKey)
            ?: throw BpdmNotFoundException(IdentifierTypeDb::class, "$identifierTypeKey/$businessPartnerType")


    fun toLegalEntityWithLegalAddress(entity: LegalEntityDb): LegalEntityWithLegalAddressVerboseDto {
        return LegalEntityWithLegalAddressVerboseDto(
            legalAddress = entity.legalAddress.toV6Dto(),
            legalEntity = entity.toV6Dto()
        )
    }

    data class LegalEntitySearchRequest(
        val bpnLs: List<String>?,
        val legalName: String?,
        val isCatenaXMemberData: Boolean?
    )

    fun findByParentBpn(bpn: String, pageIndex: Int, pageSize: Int): PageDto<SiteVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpn // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)

        val page = siteRepository.findByLegalEntity(legalEntity, PageRequest.of(pageIndex, pageSize))
        fetchSiteDependencies(page.toSet())
        return page.toDto(page.content.map { it.toV6Dto() })
    }

    private fun fetchSiteDependencies(sites: Set<SiteDb>) {
        siteRepository.joinAddresses(sites)
        siteRepository.joinStates(sites)
        val addresses = sites.flatMap { it.addresses }.toSet()
        addressService.fetchLogisticAddressDependencies(addresses)
    }

    /**
     * Find Addresses which directly belong to a Legal Entity
     */
    fun findNonSiteAddressesOfLegalEntity(bpnl: String, pageIndex: Int, pageSize: Int): PageDto<LogisticAddressVerboseDto> {
        logger.debug { "Executing findByPartnerBpn() with parameters $bpnl // $pageIndex // $pageSize" }
        val legalEntity = legalEntityRepository.findByBpnIgnoreCase(bpnl) ?:  throw BpdmNotFoundException("Business Partner", bpnl)

        val page = logisticAddressRepository.findByLegalEntityAndSitesIsEmpty(legalEntity, PageRequest.of(pageIndex, pageSize))
        addressService.fetchLogisticAddressDependencies(page.map { it }.toSet())
        return page.toDto(page.content.map { it.toV6Dto() })
    }
}
