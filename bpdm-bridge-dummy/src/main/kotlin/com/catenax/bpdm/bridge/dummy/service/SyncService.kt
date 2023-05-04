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
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponse
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncService(
    val poolClient: PoolApiClient,
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun sync() {

        // 1) Check changelog entries from Gate (after last sync time)
        val externalIdsByLsaType = getChangedExternalIdsByLsaTypeFromGate(null)

        // 2) Retrieve business partners (LSA) from Gate
        val legalEntityExternalIdsRequested = externalIdsByLsaType[LsaType.LegalEntity] ?: emptyList()
        val legalEntityGateResponses = getLegalEntitiesFromGate(legalEntityExternalIdsRequested)

        // 3) Create or update (LSAs) in Pool
        val legalEntityPoolResponses = upsertLegalEntitiesIntoPool(legalEntityGateResponses)

        // 4) Update BPN and status in Gate via new endpoint
        sendLegalEntityUpdatesToGate(legalEntityPoolResponses)
    }

    private fun getChangedExternalIdsByLsaTypeFromGate(modifiedAfter: Instant?): Map<LsaType, Collection<String>> {
        val entriesGate = gateClient.changelog().getChangelogEntriesLsaType(
            lsaType = null,
            fromTime = modifiedAfter,
            paginationRequest = PaginationRequest(0, 100)
        )
        logger.debug { "Found ${entriesGate.contentSize} changelog entries in Gate" }
        return entriesGate.content
            .groupBy { it.businessPartnerType }
            .mapValues { (_, list) -> list.map { it.externalId } }
    }

    private fun getLegalEntitiesFromGate(externalIds: Collection<String>): Collection<LegalEntityGateInputResponse> {
        val response = gateClient.legalEntities().getLegalEntitiesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.debug { "Gate returned ${response.content.size} valid legal entities, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun upsertLegalEntitiesIntoPool(legalEntityGateResponses: Collection<LegalEntityGateInputResponse>): Collection<LegalEntityPartnerCreateResponse> {
        val (entriesToCreate, entriesToUpdate) = legalEntityGateResponses.partition { it.bpn == null }
        val responses = mutableListOf<LegalEntityPartnerCreateResponse>()
        if (entriesToCreate.isNotEmpty()) {
            val createRequests = entriesToCreate.map {
                LegalEntityPartnerCreateRequest(properties = it.legalEntity, index = it.externalId)
            }
            val responseWrapper = poolClient.legalEntities().createBusinessPartners(createRequests)
            logger.debug { "Pool accepted ${responseWrapper.entityCount} new legal entities, but ${responseWrapper.errorCount} were refused" }
            responses.addAll(responseWrapper.entities)
        }
        if (entriesToUpdate.isNotEmpty()) {
            val updateRequests = entriesToUpdate.map {
                LegalEntityPartnerUpdateRequest(properties = it.legalEntity, bpn = it.bpn!!)
            }
            val responseWrapper = poolClient.legalEntities().updateBusinessPartners(updateRequests)
            logger.debug { "Pool accepted ${responseWrapper.entityCount} updated legal entities, but ${responseWrapper.errorCount} were refused" }
            responses.addAll(responseWrapper.entities)
        }
        return responses
    }

    private fun sendLegalEntityUpdatesToGate(legalEntityPoolResponses: Collection<LegalEntityPartnerCreateResponse>) {
        logger.debug { "TODO: BPNLs for ${legalEntityPoolResponses.size} legal entities should be updated in the Gate" }
    }
}