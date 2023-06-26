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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.exception.SaasNonexistentParentException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val bpnConfigProperties: BpnConfigProperties,
    private val changelogRepository: ChangelogRepository,
    private val addressPersistenceService: AddressPersistenceService,
    private val addressRepository: GateAddressRepository
) {
    private val logger = KotlinLogging.logger { }

    fun getAddresses(page: Int, size: Int, externalIds: Collection<String>? = null): PageResponse<AddressGateInputResponse> {

        val logisticAddressPage = if (externalIds != null) {
            addressRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Input, PageRequest.of(page, size))
        } else {
            addressRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = logisticAddressPage.totalElements,
            totalPages = logisticAddressPage.totalPages,
            contentSize = logisticAddressPage.content.size,
            content = toValidLogisticAddresses(logisticAddressPage),
        )
    }

    private fun toValidLogisticAddresses(logisticAddressPage: Page<LogisticAddress>): List<AddressGateInputResponse> {
        return logisticAddressPage.content.map { logisticAddress ->
            logisticAddress.toAddressGateInputResponse(logisticAddress)
        }
    }

    fun getAddressByExternalId(externalId: String): AddressGateInputResponse {

        val logisticAddress =
            addressRepository.findByExternalIdAndDataType(externalId, OutputInputEnum.Input) ?: throw BpdmNotFoundException("Logistic Address", externalId)

        return logisticAddress.toAddressGateInputResponse(logisticAddress)

    }

    /**
     * Get output addresses by fetching addresses from the database.
     */
    fun getAddressesOutput(externalIds: Collection<String>? = null, page: Int, size: Int): PageResponse<AddressGateOutputResponse> {

        val logisticAddressPage = if (externalIds != null && externalIds.isNotEmpty()) {
            addressRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Output, PageRequest.of(page, size))
        } else {
            addressRepository.findByDataType(OutputInputEnum.Output, PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = logisticAddressPage.totalElements,
            totalPages = logisticAddressPage.totalPages,
            contentSize = logisticAddressPage.content.size,
            content = toValidOutputLogisticAddresses(logisticAddressPage),
        )

    }

    private fun toValidOutputLogisticAddresses(logisticAddressPage: Page<LogisticAddress>): List<AddressGateOutputResponse> {
        return logisticAddressPage.content.map { logisticAddress ->
            logisticAddress.toAddressGateOutputResponse(logisticAddress)
        }
    }

    /**
     * Upsert addresses input to the database
     **/
    fun upsertAddresses(addresses: Collection<AddressGateInputRequest>) {

        // create changelog entry if all goes well from saasClient
        addresses.forEach { address ->
            changelogRepository.save(ChangelogEntry(address.externalId, LsaType.ADDRESS,OutputInputEnum.Input))
        }

        addressPersistenceService.persistAddressBP(addresses, OutputInputEnum.Input)
    }

    /**
     * Upsert addresses output to the database
     **/
    fun upsertOutputAddresses(addresses: Collection<AddressGateOutputRequest>) {

        addressPersistenceService.persistOutputAddressBP(addresses, OutputInputEnum.Output)

    }

    /**
     * Fetches parent information and converts the given [addresses] to their corresponding SaaS models
     */
    fun toSaasModels(addresses: Collection<AddressGateInputRequest>): Collection<BusinessPartnerSaas> {
        val parentLegalEntitiesByExternalId: Map<String, BusinessPartnerSaas> = getParentLegalEntities(addresses)
        val parentSitesByExternalId: Map<String, BusinessPartnerSaas> = getParentSites(addresses)

        return addresses.map { toSaasModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId], parentSitesByExternalId[it.siteExternalId]) }
    }

    private fun getParentSites(addresses: Collection<AddressGateInputRequest>): Map<String, BusinessPartnerSaas> {
        val parentSiteExternalIds = addresses.mapNotNull { it.siteExternalId }.distinct().toList()
        var parentSitesByExternalId: Map<String, BusinessPartnerSaas> = HashMap()
        if (parentSiteExternalIds.isNotEmpty()) {
            val parentSitesPage = saasClient.getSites(externalIds = parentSiteExternalIds)
            if (parentSitesPage.limit < parentSiteExternalIds.size) {
                // should not happen as long as configured upsert limit is lower than SaaS's limit
                throw IllegalStateException("Could not fetch all parent sites in single request.")
            }
            parentSitesByExternalId = parentSitesPage.values.associateBy { it.externalId!! }
        }
        return parentSitesByExternalId
    }

    private fun getParentLegalEntities(addresses: Collection<AddressGateInputRequest>): Map<String, BusinessPartnerSaas> {
        val parentLegalEntityExternalIds = addresses.mapNotNull { it.legalEntityExternalId }.distinct().toList()
        var parentLegalEntitiesByExternalId: Map<String, BusinessPartnerSaas> = HashMap()
        if (parentLegalEntityExternalIds.isNotEmpty()) {
            val parentLegalEntitiesPage = saasClient.getLegalEntities(externalIds = parentLegalEntityExternalIds)
            if (parentLegalEntitiesPage.limit < parentLegalEntityExternalIds.size) {
                // should not happen as long as configured upsert limit is lower than SaaS's limit
                throw IllegalStateException("Could not fetch all parent legal entities in single request.")
            }
            parentLegalEntitiesByExternalId = parentLegalEntitiesPage.values.associateBy { it.externalId!! }
        }
        return parentLegalEntitiesByExternalId
    }

    private fun toSaasModel(address: AddressGateInputRequest, parentLegalEntity: BusinessPartnerSaas?, parentSite: BusinessPartnerSaas?): BusinessPartnerSaas {
        if (parentLegalEntity == null && parentSite == null) {
            throw SaasNonexistentParentException(address.legalEntityExternalId ?: address.siteExternalId!!)
        }
        val addressSaas = saasRequestMappingService.toSaasModel(address)
        val parentNames = (parentLegalEntity ?: parentSite!!).names
        val parentIdentifiersWithoutBpn = (parentLegalEntity ?: parentSite!!).identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }
        // TODO Is this still okay? Address has its own name and identifiers with different valid types from LEs!
        return addressSaas.copy(identifiers = addressSaas.identifiers.plus(parentIdentifiersWithoutBpn), names = parentNames)
    }

}