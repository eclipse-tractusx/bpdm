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

import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.entity.Identifier
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching business partner records from the database
 */
@Service
class BusinessPartnerFetchService(
    private val legalEntityRepository: LegalEntityRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val identifierRepository: IdentifierRepository,
    private val legalFormRepository: LegalFormRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val addressService: AddressService
) {

    /**
     * Fetch a business partner by [bpn] and return as [LegalEntityPartnerResponse]
     */
    fun findLegalEntityIgnoreCase(bpn: String): LegalEntityPartnerResponse {
        return findOrThrow(bpn).toPoolDto()
    }

    fun findBusinessPartnerIgnoreCase(bpn: String): BusinessPartnerResponse {
        return findOrThrow(bpn).toBusinessPartnerDto()
    }

    /**
     * Fetch a business partner by [identifierValue] (ignoring case) of [identifierType] and return as [LegalEntityPartnerResponse]
     */
    @Transactional
    fun findLegalEntityIgnoreCase(identifierType: String, identifierValue: String): LegalEntityPartnerResponse {
        return findOrThrow(identifierType, identifierValue).toPoolDto()
    }

    @Transactional
    fun findBusinessPartnerIgnoreCase(identifierType: String, identifierValue: String): BusinessPartnerResponse {
        return findOrThrow(identifierType, identifierValue).toBusinessPartnerDto()
    }

    /**
     * Fetch business partners by BPN in [bpns]
     */
    @Transactional
    fun fetchByBpns(bpns: Collection<String>): Set<LegalEntity> {
        return fetchLegalEntityDependencies(legalEntityRepository.findDistinctByBpnIn(bpns))
    }

    /**
     * Fetch business partners by BPN in [bpns] and map to dtos
     */
    @Transactional
    fun fetchDtosByBpns(bpns: Collection<String>): Collection<LegalEntityPartnerResponse> {
        return fetchByBpns(bpns).map { it.toPoolDto() }
    }

    /**
     * Find bpn to identifier value mappings by [idValues] of [identifierType]
     */
    @Transactional
    fun findBpnsByIdentifiers(identifierType: String, idValues: Collection<String>): Set<BpnIdentifierMappingResponse> {
        val type = identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class, identifierType)
        return identifierRepository.findBpnsByIdentifierTypeAndValues(type, idValues)
    }

    fun fetchDependenciesWithLegalAddress(partners: Set<LegalEntity>): Set<LegalEntity> {
        fetchLegalEntityDependencies(partners)
        legalEntityRepository.joinLegalAddresses(partners)
        addressService.fetchAddressDependencies(partners.map { it.legalAddress }.toSet())
        return partners
    }

    fun fetchLegalEntityDependencies(partners: Set<LegalEntity>): Set<LegalEntity> {

        legalEntityRepository.joinIdentifiers(partners)
        legalEntityRepository.joinNames(partners)
        legalEntityRepository.joinStatuses(partners)
        legalEntityRepository.joinClassifications(partners)
        legalEntityRepository.joinBankAccounts(partners)
        legalEntityRepository.joinRelations(partners)
        legalEntityRepository.joinTypes(partners)
        legalEntityRepository.joinRoles(partners)
        legalEntityRepository.joinLegalForm(partners)

        // don't fetch sites/addresses since those are not needed when mapping to BusinessPartnerResponse

        val identifiers = partners.flatMap { it.identifiers }.toSet()
        fetchIdentifierDependencies(identifiers)

        val legalForms = partners.mapNotNull { it.legalForm }.toSet()
        legalFormRepository.joinCategories(legalForms)

        val bankAccounts = partners.flatMap { it.bankAccounts }.toSet()
        bankAccountRepository.joinTrustScores(bankAccounts)

        return partners
    }

    fun fetchIdentifierDependencies(identifiers: Set<Identifier>): Set<Identifier> {
        identifierRepository.joinType(identifiers)
        identifierRepository.joinStatus(identifiers)
        identifierRepository.joinIssuingBody(identifiers)

        return identifiers
    }


    private fun findOrThrow(bpn: String): LegalEntity {
        return legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException(LegalEntity::class.simpleName!!, bpn)
    }

    fun findOrThrow(identifierType: String, identifierValue: String): LegalEntity {
        val type =
            identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class.simpleName!!, identifierType)
        return legalEntityRepository.findByIdentifierTypeAndValueIgnoreCase(type, identifierValue) ?: throw BpdmNotFoundException(
            "Identifier Value",
            identifierValue
        )
    }

}