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
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springframework.stereotype.Service

@Service
class PoolUpdateService(
    val gateQueryService: GateQueryService,
    val poolClient: PoolApiClient
) {

    private val logger = KotlinLogging.logger { }

    fun createLegalEntitiesInPool(entriesToCreate: Collection<GateLegalEntityInfo>): LegalEntityPartnerCreateResponseWrapper {
        val createRequests = entriesToCreate.map {
            LegalEntityPartnerCreateRequest(
                legalEntity = it.legalEntity,
                index = it.externalId
            )
        }

        return poolClient.legalEntities().createBusinessPartners(createRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} new legal entities, ${it.errorCount} were refused" } }
    }

    fun updateLegalEntitiesInPool(entriesToUpdate: Collection<GateLegalEntityInfo>): LegalEntityPartnerUpdateResponseWrapper {
        val updateRequests = entriesToUpdate.map {
            LegalEntityPartnerUpdateRequest(
                legalEntity = it.legalEntity,
                bpnl = it.bpn!!
            )
        }

        return poolClient.legalEntities().updateBusinessPartners(updateRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} updated legal entities, ${it.errorCount} were refused" } }
    }

    fun createSitesInPool(entriesToCreate: Collection<GateSiteInfo>): SitePartnerCreateResponseWrapper {
        val leParentBpnByExternalId = entriesToCreate
            .map { it.legalEntityExternalId }
            .let { gateQueryService.getBpnByExternalId(LsaType.LegalEntity, it.toSet()) }
        val createRequests = entriesToCreate.mapNotNull { entry ->
            leParentBpnByExternalId[entry.legalEntityExternalId]
                ?.let { leParentBpn ->
                    SitePartnerCreateRequest(
                        site = entry.site,
                        index = entry.externalId,
                        bpnParent = leParentBpn
                    )
                }
        }

        if (createRequests.size != entriesToCreate.size) {
            logger.warn {
                "Not all found Gate sites (${entriesToCreate.size}) are passed to the Pool (only ${createRequests.size}) " +
                        "because some parent BPN-L entries are missing!"
            }
        }
        return poolClient.sites().createSite(createRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} new sites, ${it.errorCount} were refused" } }
    }

    fun updateSitesInPool(entriesToUpdate: Collection<GateSiteInfo>): SitePartnerUpdateResponseWrapper {
        val updateRequests = entriesToUpdate.map {
            SitePartnerUpdateRequest(
                site = it.site,
                bpns = it.bpn!!
            )
        }

        return poolClient.sites().updateSite(updateRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} updated sites, ${it.errorCount} were refused" } }
    }

    fun createAddressesInPool(entriesToCreate: Collection<GateAddressInfo>): AddressPartnerCreateResponseWrapper {
        val leParentBpnByExternalId = entriesToCreate
            .mapNotNull { it.legalEntityExternalId }
            .let { gateQueryService.getBpnByExternalId(LsaType.LegalEntity, it.toSet()) }
        val leParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            leParentBpnByExternalId[entry.legalEntityExternalId]
                ?.let { leParentBpn ->
                    AddressPartnerCreateRequest(
                        address = entry.address,
                        index = entry.externalId,
                        bpnParent = leParentBpn
                    )
                }
        }

        val siteParentBpnByExternalId = entriesToCreate
            .mapNotNull { it.siteExternalId }
            .let { gateQueryService.getBpnByExternalId(LsaType.Site, it.toSet()) }
        val siteParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            siteParentBpnByExternalId[entry.siteExternalId]
                ?.let { siteParentBpn ->
                    AddressPartnerCreateRequest(
                        address = entry.address,
                        index = entry.externalId,
                        bpnParent = siteParentBpn
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
        return poolClient.addresses().createAddresses(createRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} new addresses, ${it.errorCount} were refused" } }
    }

    fun updateAddressesInPool(entriesToUpdate: Collection<GateAddressInfo>): AddressPartnerUpdateResponseWrapper {
        val updateRequests = entriesToUpdate.map {
            AddressPartnerUpdateRequest(
                address = it.address,
                bpna = it.bpn!!
            )
        }

        return poolClient.addresses().updateAddresses(updateRequests)
            .also { logger.info { "Pool accepted ${it.entityCount} updated addresses, ${it.errorCount} were refused" } }
    }

}
