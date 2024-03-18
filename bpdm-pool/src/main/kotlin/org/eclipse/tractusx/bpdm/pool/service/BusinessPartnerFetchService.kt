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
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.response.BpnIdentifierMappingDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityIdentifierDb
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching business partner records from the database
 */
@Service
class BusinessPartnerFetchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val addressService: AddressService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Search legal entities per page for [searchRequest] and [paginationRequest]
     */
    @Transactional
    fun searchLegalEntities(searchRequest: LegalEntitySearchRequest, paginationRequest: PaginationRequest): PageDto<LegalEntityWithLegalAddressVerboseDto>{
        val spec = Specification.allOf(
            LegalEntityRepository.byBpns(searchRequest.bpnLs),
            LegalEntityRepository.byLegalName(searchRequest.legalName),
            LegalEntityRepository.byIsMember(searchRequest.isCatenaXMemberData)
        )

        val legalEntityPage = legalEntityRepository.findAll(spec, PageRequest.of(paginationRequest.page, paginationRequest.size))

        return legalEntityPage.toDto(LegalEntityDb::toLegalEntityWithLegalAddress)
    }

    /**
     * Fetch a business partner by [bpn] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    fun findLegalEntityIgnoreCase(bpn: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $bpn" }
        return findLegalEntityOrThrow(bpn).toLegalEntityWithLegalAddress()
    }


    /**
     * Fetch a business partner by [identifierValue] (ignoring case) of [identifierType] and return as [LegalEntityWithLegalAddressVerboseDto]
     */
    @Transactional
    fun findLegalEntityIgnoreCase(identifierType: String, identifierValue: String): LegalEntityWithLegalAddressVerboseDto {
        logger.debug { "Executing findLegalEntityIgnoreCase() with parameters $identifierType and $identifierValue" }
        return findLegalEntityOrThrow(identifierType, identifierValue).toLegalEntityWithLegalAddress()
    }

    /**
     * Fetch business partners by BPN in [bpns]
     */
    @Transactional
    fun fetchByBpns(bpns: Collection<String>): Set<LegalEntityDb> {
        logger.debug { "Executing fetchByBpns() with parameters $bpns " }
        return fetchLegalEntityDependencies(legalEntityRepository.findDistinctByBpnIn(bpns))
    }

    /**
     * Fetch business partners by BPN in [bpns] and map to dtos
     */
    @Transactional
    fun fetchDtosByBpns(bpns: Collection<String>): Collection<LegalEntityWithLegalAddressVerboseDto> {
        logger.debug { "Executing fetchDtosByBpns() with parameters $bpns " }
        return fetchByBpns(bpns).map { it.toLegalEntityWithLegalAddress() }
    }

    /**
     * Find bpn to identifier value mappings by [idValues] of [identifierTypeKey]
     */
    @Transactional
    fun findBpnsByIdentifiers(
        identifierTypeKey: String,
        businessPartnerType: IdentifierBusinessPartnerType,
        idValues: Collection<String>
    ): Set<BpnIdentifierMappingDto> {
        logger.debug { "Executing findBpnsByIdentifiers() with parameters $identifierTypeKey // $businessPartnerType and $idValues" }
        val identifierType = findIdentifierTypeOrThrow(identifierTypeKey, businessPartnerType)
        return when (businessPartnerType) {
            IdentifierBusinessPartnerType.LEGAL_ENTITY -> legalEntityIdentifierRepository.findBpnsByIdentifierTypeAndValues(identifierType, idValues)
            IdentifierBusinessPartnerType.ADDRESS -> addressIdentifierRepository.findBpnsByIdentifierTypeAndValues(identifierType, idValues)
        }
    }

    fun fetchDependenciesWithLegalAddress(partners: Set<LegalEntityDb>): Set<LegalEntityDb> {
        fetchLegalEntityDependencies(partners)
        legalEntityRepository.joinLegalAddresses(partners)
        addressService.fetchLogisticAddressDependencies(partners.map { it.legalAddress }.toSet())
        return partners
    }

    fun fetchLegalEntityDependencies(partners: Set<LegalEntityDb>): Set<LegalEntityDb> {

        legalEntityRepository.joinIdentifiers(partners)
        legalEntityRepository.joinStates(partners)
        legalEntityRepository.joinClassifications(partners)
        legalEntityRepository.joinRelations(partners)
        legalEntityRepository.joinLegalForm(partners)

        // don't fetch sites/addresses since those are not needed when mapping to BusinessPartnerResponse

        val identifiers = partners.flatMap { it.identifiers }.toSet()
        fetchIdentifierDependencies(identifiers)

        return partners
    }

    fun fetchIdentifierDependencies(identifiers: Set<LegalEntityIdentifierDb>): Set<LegalEntityIdentifierDb> {
        legalEntityIdentifierRepository.joinType(identifiers)

        return identifiers
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


    data class LegalEntitySearchRequest(
        val bpnLs: List<String>?,
        val legalName: String?,
        val isCatenaXMemberData: Boolean?
    )

}