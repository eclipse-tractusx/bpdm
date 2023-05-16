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

import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.springframework.stereotype.Service

@Service
class SyncService(
    val gateQueryService: GateQueryService,
    val poolUpdateService: PoolUpdateService,
    val gateUpdateService: GateUpdateService
) {

    // TODO For improved robustness we should maybe persistently track all sync entries (status) by LSAType/externalID.

    fun sync() {
        // Check changelog entries from Gate (after last sync time)
        val externalIdsByType = gateQueryService.getChangedExternalIdsByLsaType(null)
        // TODO persist syncAfter=LocalDateTime.now()

        externalIdsByType[LsaType.LegalEntity]?.let { syncLegalEntities(it) }
        externalIdsByType[LsaType.Site]?.let { syncSites(it) }
        externalIdsByType[LsaType.Address]?.let { syncAddresses(it) }
    }

    private fun syncLegalEntities(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getLegalEntityInfos(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createLegalEntitiesInPool(entriesToCreate)
            gateUpdateService.handleLegalEntityCreateResponse(responseWrapper)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateLegalEntitiesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleLegalEntityUpdateResponse(responseWrapper, externalIdByBpn)
        }
    }

    private fun syncSites(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getSiteInfos(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createSitesInPool(entriesToCreate)
            gateUpdateService.handleSiteCreateResponse(responseWrapper)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateSitesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleSiteUpdateResponse(responseWrapper, externalIdByBpn)
        }
    }

    private fun syncAddresses(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getAddressInfos(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createAddressesInPool(entriesToCreate)
            gateUpdateService.handleAddressCreateResponse(responseWrapper)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateAddressesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleAddressUpdateResponse(responseWrapper, externalIdByBpn)
        }
    }

}
