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

package com.catenax.bpdm.bridge.dummy.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncService(
    val poolClient: PoolApiClient,
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun sync() {
        // Check changelog entries from Gate (after last sync time)
        val externalIdsByType = getChangedExternalIdsByLsaTypeFromGate(null)
        // TODO persist syncAfter=LocalDateTime.now()

        externalIdsByType[LsaType.LegalEntity]?.let { syncLegalEntities(it) }
        externalIdsByType[LsaType.Site]?.let { syncSites(it) }
        externalIdsByType[LsaType.Address]?.let { syncAddresses(it) }
    }

    private fun getChangedExternalIdsByLsaTypeFromGate(modifiedAfter: Instant?): Map<LsaType, Collection<String>> {
        // TODO use pagination properly
        val entriesGate = gateClient.changelog().getChangelogEntriesLsaType(
            lsaType = null,
            fromTime = modifiedAfter,
            paginationRequest = PaginationRequest(0, 100)
        )

        return entriesGate.content
            .groupBy { it.businessPartnerType }
            .mapValues { (_, list) -> list.map { it.externalId }.toSet() }
            .also {
                logger.info {
                    "Changed entries in Gate since last sync: " +
                            "${it[LsaType.LegalEntity]?.size ?: 0} legal entities, " +
                            "${it[LsaType.Site]?.size ?: 0} sites, " +
                            "${it[LsaType.Address]?.size ?: 0} addresses"
                }
            }
    }

    private fun syncLegalEntities(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getLegalEntitiesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createLegalEntitiesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateLegalEntitiesInPool(entriesToUpdate)
        }
    }

    private fun syncSites(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getSitesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createSitesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateSitesInPool(entriesToUpdate)
        }
    }

    private fun syncAddresses(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getAddressesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createAddressesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateAddressesInPool(entriesToUpdate)
        }
    }

    private fun getLegalEntitiesFromGate(externalIds: Collection<String>): Collection<LegalEntityGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.legalEntities().getLegalEntitiesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid legal entities, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun getSitesFromGate(externalIds: Collection<String>): Collection<SiteGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.sites().getSitesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid sites, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun getAddressesFromGate(externalIds: Collection<String>): Collection<AddressGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.addresses().getAddressesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid addresses, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun createLegalEntitiesInPool(entriesToCreate: Collection<LegalEntityGateInputResponse>) {
        val createRequests = entriesToCreate.map {
            LegalEntityPartnerCreateRequest(
                properties = it.legalEntity,
                index = it.externalId
            )
        }

        val responseWrapper = poolClient.legalEntities().createBusinessPartners(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new legal entities, but ${responseWrapper.errorCount} were refused" }
        handleLegalEntityCreateResponse(entriesToCreate, responseWrapper)
    }

    private fun updateLegalEntitiesInPool(entriesToUpdate: Collection<LegalEntityGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            LegalEntityPartnerUpdateRequest(
                properties = it.legalEntity,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.legalEntities().updateBusinessPartners(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated legal entities, but ${responseWrapper.errorCount} were refused" }
        handleLegalEntityUpdateResponse(entriesToUpdate, responseWrapper)
    }

    private fun createSitesInPool(entriesToCreate: List<SiteGateInputResponse>) {
        val leParentsByExternalId = entriesToCreate
            .map { it.legalEntityExternalId }
            .let { getLegalEntitiesFromGate(it) }
            .associateBy { it.externalId }
        val createRequests = entriesToCreate.mapNotNull { entry ->
            leParentsByExternalId[entry.legalEntityExternalId]?.bpn
                ?.let { leParentBpn ->
                    SitePartnerCreateRequest(
                        site = entry.site,
                        index = entry.externalId,
                        legalEntity = leParentBpn
                    )
                }
        }

        if (createRequests.size != entriesToCreate.size) {
            logger.warn {
                "Not all found Gate sites (${entriesToCreate.size}) are passed to the Pool (only ${createRequests.size}) " +
                        "because some parent BPN-L entries are missing!"
            }
        }
        val responseWrapper = poolClient.sites().createSite(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new sites, but ${responseWrapper.errorCount} were refused" }
        handleSiteCreateResponse(entriesToCreate, responseWrapper)
    }

    private fun updateSitesInPool(entriesToUpdate: List<SiteGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            SitePartnerUpdateRequest(
                site = it.site,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.sites().updateSite(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated sites, but ${responseWrapper.errorCount} were refused" }
        handleSiteUpdateResponse(entriesToUpdate, responseWrapper)
    }

    private fun createAddressesInPool(entriesToCreate: List<AddressGateInputResponse>) {
        val leParentsByExternalId = entriesToCreate
            .mapNotNull { it.legalEntityExternalId }
            .let { getLegalEntitiesFromGate(it) }
            .associateBy { it.externalId }
        val leParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            leParentsByExternalId[entry.legalEntityExternalId]?.bpn
                ?.let { leParentBpn ->
                    AddressPartnerCreateRequest(
                        properties = entry.address,
                        index = entry.externalId,
                        parent = leParentBpn
                    )
                }
        }

        val siteParentsByExternalId = entriesToCreate
            .mapNotNull { it.siteExternalId }
            .let { getSitesFromGate(it) }
            .associateBy { it.externalId }
        val siteParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            siteParentsByExternalId[entry.siteExternalId]?.bpn
                ?.let { siteParentBpn ->
                    AddressPartnerCreateRequest(
                        properties = entry.address,
                        index = entry.externalId,
                        parent = siteParentBpn
                    )
                }
        }

        val createRequests = leParentsCreateRequests.plus(siteParentsCreateRequests)
        if (createRequests.size != entriesToCreate.size) {
            logger.warn {
                "Not all found Gate addresses (${entriesToCreate.size}) are passed to the Pool (only ${createRequests.size}) " +
                        "because some parent BPN-L/S entries are missing!"
            }
        }
        val responseWrapper = poolClient.addresses().createAddresses(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new addresses, but ${responseWrapper.errorCount} were refused" }
        handleAddressCreateResponse(entriesToCreate, responseWrapper)
    }

    private fun updateAddressesInPool(entriesToUpdate: List<AddressGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            AddressPartnerUpdateRequest(
                properties = it.address,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.addresses().updateAddresses(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated addresses, but ${responseWrapper.errorCount} were refused" }
        handleAddressUpdateResponse(entriesToUpdate, responseWrapper)
    }


    // TODO Update BPN and status in Gate via new endpoint for create/update and L/S/A.
    //  It's very important to update the BPN in the Gate.
    //  Otherwise duplicates may be created in the Pool (or the request fails because of uniqueness constraints).
    //  For improved robustness we should maybe persistently track all sync entries (status) by LSAType/externalID.

    private fun handleLegalEntityCreateResponse(
        legalEntitiesFromGate: Collection<LegalEntityGateInputResponse>,
        createResponseWrapper: LegalEntityPartnerCreateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${createResponseWrapper.entityCount} new legal entities should be updated in the Gate" }
    }

    private fun handleLegalEntityUpdateResponse(
        legalEntitiesFromGate: Collection<LegalEntityGateInputResponse>,
        updateResponseWrapper: LegalEntityPartnerUpdateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${updateResponseWrapper.entityCount} legal entities should be updated in the Gate" }
    }

    private fun handleSiteCreateResponse(
        sitesFromGate: Collection<SiteGateInputResponse>,
        createResponseWrapper: SitePartnerCreateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${createResponseWrapper.entityCount} new sites should be updated in the Gate" }
    }

    private fun handleSiteUpdateResponse(
        sitesFromGate: Collection<SiteGateInputResponse>,
        updateResponseWrapper: SitePartnerUpdateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${updateResponseWrapper.entityCount} sites should be updated in the Gate" }
    }

    private fun handleAddressCreateResponse(
        addressesFromGate: Collection<AddressGateInputResponse>,
        createResponseWrapper: AddressPartnerCreateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${createResponseWrapper.entityCount} new addresses should be updated in the Gate" }
    }

    private fun handleAddressUpdateResponse(
        addressesFromGate: Collection<AddressGateInputResponse>,
        updateResponseWrapper: AddressPartnerUpdateResponseWrapper
    ) {
        logger.info { "TODO: BPNLs for ${updateResponseWrapper.entityCount} addresses should be updated in the Gate" }
    }
}