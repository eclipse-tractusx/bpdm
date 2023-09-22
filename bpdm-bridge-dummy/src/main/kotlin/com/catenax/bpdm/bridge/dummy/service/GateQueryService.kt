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

import com.catenax.bpdm.bridge.dummy.config.BridgeConfigProperties
import com.catenax.bpdm.bridge.dummy.dto.GateAddressInfo
import com.catenax.bpdm.bridge.dummy.dto.GateLegalEntityInfo
import com.catenax.bpdm.bridge.dummy.dto.GateSiteInfo
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GateQueryService(
    val gateClient: GateClient,
    val bridgeConfigProperties: BridgeConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun getChangedExternalIdsByBusinessPartnerType(modifiedAfter: Instant?): Map<BusinessPartnerType, Set<String>> {
        var page = 0
        var totalPages: Int
        val content = mutableListOf<ChangelogGateDto>()

        do {
            val pageResponse = gateClient.changelog.getInputChangelog(
                searchRequest = ChangelogSearchRequest(timestampAfter = modifiedAfter),
                paginationRequest = PaginationRequest(page, bridgeConfigProperties.queryPageSize)
            )
            page++
            totalPages = pageResponse.totalPages
            content.addAll(pageResponse.content)
        } while (page < totalPages)

        return content
            .groupBy { it.businessPartnerType }
            .mapValues { (_, list) -> list.map { it.externalId }.toSet() }
            .also {
                logger.info {
                    "Changed entries in Gate since last sync: " +
                            "${it[BusinessPartnerType.LEGAL_ENTITY]?.size ?: 0} legal entities, " +
                            "${it[BusinessPartnerType.SITE]?.size ?: 0} sites, " +
                            "${it[BusinessPartnerType.ADDRESS]?.size ?: 0} addresses"
                }
            }
    }

    fun getLegalEntityInfos(externalIds: Set<String>): Collection<GateLegalEntityInfo> {
        val entries = getLegalEntitiesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(BusinessPartnerType.LEGAL_ENTITY, externalIds)

        return entries.map {
            GateLegalEntityInfo(
                legalNameParts = it.legalNameParts,
                legalEntity = it.legalEntity,
                legalAddress = it.legalAddress,
                externalId = it.externalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getSiteInfos(externalIds: Set<String>): Collection<GateSiteInfo> {
        val entries = getSitesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(BusinessPartnerType.SITE, externalIds)

        return entries.map {
            GateSiteInfo(
                site = it.site,
                externalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId,
                bpn = bpnByExternalId[it.externalId],
                mainAddress = it.mainAddress.address
            )
        }
    }

    fun getAddressInfos(externalIds: Set<String>): Collection<GateAddressInfo> {
        val entries = getAddressesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(BusinessPartnerType.ADDRESS, externalIds)

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

    fun getBpnByExternalId(businessPartnerType: BusinessPartnerType, externalIds: Set<String>): Map<String, String> {
        if (externalIds.isEmpty()) {
            return emptyMap()
        }

        var page = 0
        var totalPages: Int
        val content = mutableListOf<SharingStateDto>()

        do {
            val pageResponse = gateClient.sharingState.getSharingStates(
                businessPartnerType = businessPartnerType,
                externalIds = externalIds,
                paginationRequest = PaginationRequest(page, bridgeConfigProperties.queryPageSize)
            )
            page++
            totalPages = pageResponse.totalPages
            content.addAll(pageResponse.content)
        } while (page < totalPages)

        return content
            .associateBy { it.externalId }
            .filter { it.value.bpn != null }
            .mapValues { it.value.bpn!! }
    }

    private fun getLegalEntitiesInput(externalIds: Set<String>): Collection<LegalEntityGateInputDto> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var page = 0
        val validContent = mutableListOf<LegalEntityGateInputDto>()

        do {
            val pageResponse = gateClient.legalEntities.getLegalEntitiesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationRequest(page, bridgeConfigProperties.queryPageSize)
            )
            page++
            validContent.addAll(pageResponse.content)
        } while (pageResponse.content.isNotEmpty())

        logger.info { "Gate returned ${validContent.size} valid legal entities" }
        return validContent
    }

    private fun getSitesInput(externalIds: Set<String>): Collection<SiteGateInputDto> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var page = 0
        val validContent = mutableListOf<SiteGateInputDto>()

        do {
            val pageResponse = gateClient.sites.getSitesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationRequest(page, bridgeConfigProperties.queryPageSize)
            )
            page++
            validContent.addAll(pageResponse.content)
        } while (pageResponse.content.isNotEmpty())

        logger.info { "Gate returned ${validContent.size} valid sites" }
        return validContent
    }

    private fun getAddressesInput(externalIds: Set<String>): Collection<AddressGateInputDto> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var page = 0
        val validContent = mutableListOf<AddressGateInputDto>()

        do {
            val pageResponse = gateClient.addresses.getAddressesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationRequest(page, bridgeConfigProperties.queryPageSize)
            )
            page++
            validContent.addAll(pageResponse.content)
        } while (pageResponse.content.isNotEmpty())

        logger.info { "Gate returned ${validContent.size} valid addresses" }
        return validContent
    }

}
