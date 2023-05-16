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

import com.catenax.bpdm.bridge.dummy.dto.GateAddressInfo
import com.catenax.bpdm.bridge.dummy.dto.GateLegalEntityInfo
import com.catenax.bpdm.bridge.dummy.dto.GateSiteInfo
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GateQueryService(
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun getChangedExternalIdsByLsaType(modifiedAfter: Instant?): Map<LsaType, Set<String>> {
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

    fun getLegalEntityInfos(externalIds: Set<String>): Collection<GateLegalEntityInfo> {
        val entries = getLegalEntitiesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.LegalEntity, externalIds)

        return entries.map {
            GateLegalEntityInfo(
                legalEntity = it.legalEntity,
                externalId = it.externalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getSiteInfos(externalIds: Set<String>): Collection<GateSiteInfo> {
        val entries = getSitesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.Site, externalIds)

        return entries.map {
            GateSiteInfo(
                site = it.site,
                externalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getAddressInfos(externalIds: Set<String>): Collection<GateAddressInfo> {
        val entries = getAddressesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.Address, externalIds)

        return entries.map {
            GateAddressInfo(
                address = it.address,
                externalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId,
                siteExternalId = it.siteExternalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getBpnByExternalId(lsaType: LsaType, externalIds: Set<String>): Map<String, String> {
        if (externalIds.isEmpty()) {
            return emptyMap()
        }
        // TODO use pagination properly
        val page = gateClient.sharingState().getSharingStates(
            lsaType = lsaType,
            externalIds = externalIds,
            paginationRequest = PaginationRequest(0, 100)
        )
        return page.content
            .associateBy { it.externalId }
            .filter { it.value.bpn != null }
            .mapValues { it.value.bpn!! }
    }

    private fun getLegalEntitiesInput(externalIds: Set<String>): Collection<LegalEntityGateInputResponse> {
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

    private fun getSitesInput(externalIds: Set<String>): Collection<SiteGateInputResponse> {
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

    private fun getAddressesInput(externalIds: Set<String>): Collection<AddressGateInputResponse> {
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

}
