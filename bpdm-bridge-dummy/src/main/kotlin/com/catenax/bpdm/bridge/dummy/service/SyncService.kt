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

import com.catenax.bpdm.bridge.dummy.entity.SyncType
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncService(
    val gateQueryService: GateQueryService,
    val poolUpdateService: PoolUpdateService,
    val gateUpdateService: GateUpdateService,
    val syncRecordService: SyncRecordService
) {

    private val logger = KotlinLogging.logger { }

    // TODO For improved robustness we should maybe persistently track all sync entries (status) by businessPartnerType/externalID.

    fun sync() {
        logger.info("Bridge sync started...")
        val syncRecord = syncRecordService.setSynchronizationStart(SyncType.GATE_TO_POOL)
        try {
            syncInternal(syncRecord.fromTime)
            logger.info("Bridge sync completed")
            syncRecordService.setSynchronizationSuccess(SyncType.GATE_TO_POOL)
        } catch (e: Exception) {
            logger.error("Bridge sync failed with critical error:", e)
            syncRecordService.setSynchronizationError(SyncType.GATE_TO_POOL, e.toString(), null)
            throw e
        }
    }

    private fun syncInternal(modifiedAfter: Instant) {
        // Check changelog entries from Gate (after last sync time)
        val externalIdsByType = gateQueryService.getChangedExternalIdsByBusinessPartnerType(modifiedAfter)

        externalIdsByType[BusinessPartnerType.LEGAL_ENTITY]?.let { syncLegalEntities(it) }
        externalIdsByType[BusinessPartnerType.SITE]?.let { syncSites(it) }
        externalIdsByType[BusinessPartnerType.ADDRESS]?.let { syncAddresses(it) }
    }

    private fun syncLegalEntities(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getLegalEntityInfos(externalIdsRequested)
        val entryByExternalId = entries.associateBy { it.externalId }
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createLegalEntitiesInPool(entriesToCreate)
            gateUpdateService.handleLegalEntityCreateResponse(responseWrapper, entryByExternalId)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateLegalEntitiesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleLegalEntityUpdateResponse(responseWrapper, externalIdByBpn, entryByExternalId)
        }
    }

    private fun syncSites(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getSiteInfos(externalIdsRequested)
        val entryByExternalId = entries.associateBy { it.externalId }
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createSitesInPool(entriesToCreate)
            gateUpdateService.handleSiteCreateResponse(responseWrapper, entryByExternalId)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateSitesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleSiteUpdateResponse(responseWrapper, externalIdByBpn, entryByExternalId)
        }
    }

    private fun syncAddresses(externalIdsRequested: Set<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = gateQueryService.getAddressInfos(externalIdsRequested)
        val entryByExternalId = entries.associateBy { it.externalId }
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.createAddressesInPool(entriesToCreate)
            gateUpdateService.handleAddressCreateResponse(responseWrapper, entryByExternalId)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val responseWrapper = poolUpdateService.updateAddressesInPool(entriesToUpdate)
            val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
            gateUpdateService.handleAddressUpdateResponse(responseWrapper, externalIdByBpn, entryByExternalId)
        }
    }

}
