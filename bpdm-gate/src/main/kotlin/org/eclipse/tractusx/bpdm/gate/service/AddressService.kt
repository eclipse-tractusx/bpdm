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
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.FetchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.OptionalLsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.exception.SaasInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.SaasNonexistentParentException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val inputSaasMappingService: InputSaasMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val poolClient: PoolClient,
    private val bpnConfigProperties: BpnConfigProperties,
    private val typeMatchingService: TypeMatchingService,
    private val changelogRepository: ChangelogRepository,
    private val addressPersistenceService: AddressPersistenceService
) {
    private val logger = KotlinLogging.logger { }

    fun getAddresses(limit: Int, startAfter: String?): PageStartAfterResponse<AddressGateInputResponse> {
        val addressesPage = saasClient.getAddresses(limit, startAfter)
        val validEntries = addressesPage.values.filter { validateAddressBusinessPartner(it) }

        val addressesWithParent = validEntries.map { Pair(it, inputSaasMappingService.toParentLegalEntityExternalId(it)!!) }

        val parents =
            if (addressesWithParent.isNotEmpty()) saasClient.getBusinessPartners(externalIds = addressesWithParent.map { (_, parentId) -> parentId }).values else emptyList()
        val (legalEntityParents, siteParents) = typeMatchingService.partitionIntoParentTypes(parents)
        val legalEntityParentIds = legalEntityParents.mapNotNull { it.externalId }.toHashSet()
        val siteParentIds = siteParents.mapNotNull { it.externalId }.toHashSet()

        val inputAddresses = addressesWithParent.mapNotNull { (address, parentId) ->
            when {
                legalEntityParentIds.contains(parentId) -> inputSaasMappingService.toInputAddress(address, parentId, null)
                siteParentIds.contains(parentId) -> inputSaasMappingService.toInputAddress(address, null, parentId)
                else -> {
                    logger.warn { "Could not fetch parent for SaaS address record with ID ${address.id}" }
                    null
                }
            }
        }

        return PageStartAfterResponse(
            total = addressesPage.total,
            nextStartAfter = addressesPage.nextStartAfter,
            content = inputAddresses,
            invalidEntries = addressesPage.values.size - inputAddresses.size
        )
    }

    fun getAddressByExternalId(externalId: String): AddressGateInputResponse {
        val fetchResponse = saasClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidAddressInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Address", externalId)
        }
    }

    /**
     * Get addresses by first fetching addresses from "augmented business partners" in SaaS. Augmented business partners from SaaS should contain a BPN,
     * which is then used to fetch the data for the addresses from the bpdm pool.
     */
    fun getAddressesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageOutputResponse<AddressGateOutput> {
        val partnerResponse = saasClient.getAddresses(limit = limit, startAfter = startAfter, externalIds = externalIds)
        val partners = partnerResponse.values

        val partnersWithExternalId = outputSaasMappingService.mapWithExternalId(partners)
        val augmentedPartnerResponse = saasClient.getAugmentedAddresses(externalIds = partnersWithExternalId.map { it.externalId })
        val partnersWithLocalBpn = outputSaasMappingService.mapWithLocalBpn(partnersWithExternalId, augmentedPartnerResponse.values)

        //Search entries in the pool with BPNs found in the local mirror
        val bpnSet = partnersWithLocalBpn.map { it.bpn }.toSet()
        val addressesByBpnMap = poolClient.searchAddresses(bpnSet).associateBy { it.bpn }

        if (bpnSet.size > addressesByBpnMap.size) {
            logger.warn { "Requested ${bpnSet.size} addresses from pool, but only ${addressesByBpnMap.size} were found." }
        }

        val partnersWithPoolBpn = partnersWithLocalBpn.filter { addressesByBpnMap[it.bpn] != null }
        val bpnByExternalIdMap = partnersWithPoolBpn.map { Pair(it.partner.externalId!!, it.bpn) }.toMap()

        //Evaluate the sharing status of the legal entities
        val sharingStatus = outputSaasMappingService.evaluateSharingStatus(partners, partnersWithLocalBpn, partnersWithPoolBpn)

        val validAddresses = sharingStatus.validExternalIds.map { externalId ->
            val bpn = bpnByExternalIdMap[externalId]!!
            val address = addressesByBpnMap[bpn]!!
            toAddressOutput(externalId, address)
        }

        return PageOutputResponse(
            total = partnerResponse.total,
            nextStartAfter = partnerResponse.nextStartAfter,
            content = validAddresses,
            invalidEntries = partners.size - sharingStatus.validExternalIds.size, // difference between all entries from SaaS and valid content
            pending = sharingStatus.pendingExternalIds,
            errors = sharingStatus.errors,
        )
    }

    fun toAddressOutput(externalId: String, address: LogisticAddressResponse): AddressGateOutput {
        return AddressGateOutput(
            address = address,
            externalId = externalId
        )
    }

    /**
     * Upsert addresses by:
     *
     * - Retrieving parent legal entities and sites to check whether they exist and since their identifiers are copied to site
     * - Upserting the addresses
     * - Retrieving the old relations of the addresses and deleting them
     * - Upserting the new relations
     */
    fun upsertAddresses(addresses: Collection<AddressGateInputRequest>) {

        val addressesSaas = toSaasModels(addresses)
        saasClient.upsertAddresses(addressesSaas)

        addressPersistenceService.persistAddressBP(addresses)

        // create changelog entry if all goes well from saasClient
        addresses.forEach { address ->
            changelogRepository.save(ChangelogEntry(address.externalId, LsaType.Address))
        }

        deleteParentRelationsOfAddresses(addresses)

        upsertParentRelations(addresses)

    }

    /**
     * Fetches parent information and converts the given [addresses] to their corresponding SaaS models
     */
    fun toSaasModels(addresses: Collection<AddressGateInputRequest>): Collection<BusinessPartnerSaas> {
        val parentLegalEntitiesByExternalId: Map<String, BusinessPartnerSaas> = getParentLegalEntities(addresses)
        val parentSitesByExternalId: Map<String, BusinessPartnerSaas> = getParentSites(addresses)

        return addresses.map { toSaasModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId], parentSitesByExternalId[it.siteExternalId]) }
    }

    private fun upsertParentRelations(addresses: Collection<AddressGateInputRequest>) {
        val legalEntityRelations = toLegalEntityParentRelations(addresses)
        val siteRelations = toSiteParentRelations(addresses)
        saasClient.upsertAddressRelations(legalEntityRelations, siteRelations)
    }

    private fun deleteParentRelationsOfAddresses(addresses: Collection<AddressGateInputRequest>) {
        val addressesPage = saasClient.getAddresses(externalIds = addresses.map { it.externalId })
        saasClient.deleteParentRelations(addressesPage.values)
    }

    private fun toSiteParentRelations(addresses: Collection<AddressGateInputRequest>) =
        addresses.filter {
            it.siteExternalId != null
        }.map {
            SaasClient.AddressSiteRelation(
                addressExternalId = it.externalId,
                siteExternalId = it.siteExternalId!!
            )
        }.toList()

    private fun toLegalEntityParentRelations(addresses: Collection<AddressGateInputRequest>) =
        addresses.filter {
            it.legalEntityExternalId != null
        }.map {
            SaasClient.AddressLegalEntityRelation(
                addressExternalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId!!
            )
        }.toList()

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
        return addressSaas.copy(identifiers = addressSaas.identifiers.plus(parentIdentifiersWithoutBpn), names = parentNames)
    }

    private fun toValidAddressInput(partner: BusinessPartnerSaas): AddressGateInputResponse {
        if (!validateAddressBusinessPartner(partner)) {
            throw SaasInvalidRecordException(partner.id)
        }

        val parentId = inputSaasMappingService.toParentLegalEntityExternalId(partner)
        val parentType = parentId?.let { saasClient.getBusinessPartner(it).businessPartner }?.let { typeMatchingService.determineType(it) }

        return when (parentType) {
            OptionalLsaType.LegalEntity -> inputSaasMappingService.toInputAddress(partner, parentId, null)
            OptionalLsaType.Site -> inputSaasMappingService.toInputAddress(partner, null, parentId)
            else -> throw SaasInvalidRecordException(parentId)
        }
    }

    private fun validateAddressBusinessPartner(partner: BusinessPartnerSaas): Boolean {
        val logMessageStart = "SaaS business partner for address with ${if (partner.id != null) "ID " + partner.id else "external id " + partner.externalId}"

        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have an address" }
            return false
        }

        val numParents = inputSaasMappingService.toParentLegalEntityExternalIds(partner).size
        if (numParents > 1) {
            logger.warn { "$logMessageStart has multiple parents." }
        }

        if (numParents == 0) {
            logger.warn { "$logMessageStart does not have a parent legal entity or site." }
            return false
        }

        return true
    }
}