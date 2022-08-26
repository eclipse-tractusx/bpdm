/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.CdqNonexistentParentException
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val cdqClient: CdqClient,
    private val bpnConfigProperties: BpnConfigProperties,
    private val cdqConfigProperties: CdqConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(limit: Int, startAfter: String?): PageStartAfterResponse<SiteGateInput> {
        val sitesPage = cdqClient.getSites(limit, startAfter)

        val validEntries = sitesPage.values.filter { validateBusinessPartner(it) }

        return PageStartAfterResponse(
            total = sitesPage.total,
            nextStartAfter = sitesPage.nextStartAfter,
            content = validEntries.map { inputCdqMappingService.toInputSite(it) },
            invalidEntries = sitesPage.values.size - validEntries.size
        )
    }

    fun getSiteByExternalId(externalId: String): SiteGateInput {
        val fetchResponse = cdqClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidSiteInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Site", externalId)
        }
    }

    fun upsertSites(sites: Collection<SiteGateInput>) {
        val parentLegalEntitiesByExternalId = getParentLegalEntities(sites)

        val sitesCdq = sites.map { toCdqModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId]) }
        cdqClient.upsertSites(sitesCdq)

        val relations = sites.map {
            CdqClient.SiteLegalEntityRelation(
                siteExternalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId
            )
        }.toList()
        cdqClient.upsertSiteRelations(relations)
    }

    private fun toValidSiteInput(partner: BusinessPartnerCdq): SiteGateInput {
        if (!validateBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return inputCdqMappingService.toInputSite(partner)
    }

    private fun validateBusinessPartner(partner: BusinessPartnerCdq): Boolean {
        var valid = true
        val logMessageStart = "CDQ business partner for site with ${if (partner.id != null) "CDQ ID " + partner.id else "external id " + partner.externalId}"

        valid = valid && validateAddresses(partner, logMessageStart)
        valid = valid && validateLegalEntityParents(partner, logMessageStart)
        valid = valid && validateNames(partner, logMessageStart)

        return valid
    }

    private fun validateNames(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        if (partner.names.size > 1) {
            logger.warn { "$logMessageStart has multiple names." }
        }
        if (partner.names.isEmpty()) {
            logger.warn { "$logMessageStart does not have a name." }
            return false
        }
        return true
    }

    private fun validateAddresses(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple main addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have a main address" }
            return false
        }
        return true
    }

    private fun validateLegalEntityParents(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        val numLegalEntityParents = partner.relations.filter { it.startNodeDataSource == cdqConfigProperties.datasourceLegalEntity }
            .filter { it.type.technicalKey == "PARENT" }
            .map { it.startNode }.size
        if (numLegalEntityParents > 1) {
            logger.warn { "$logMessageStart has multiple parent legal entities." }
        }
        if (numLegalEntityParents == 0) {
            logger.warn { "$logMessageStart does not have a parent legal entity." }
            return false
        }
        return true
    }

    private fun getParentLegalEntities(sites: Collection<SiteGateInput>): Map<String, BusinessPartnerCdq> {
        val parentLegalEntityExternalIds = sites.map { it.legalEntityExternalId }.distinct().toList()
        val parentLegalEntitiesPage = cdqClient.getLegalEntities(externalIds = parentLegalEntityExternalIds)
        if (parentLegalEntitiesPage.limit < parentLegalEntityExternalIds.size) {
            // should not happen as long as configured upsert limit is lower than cdq's limit
            throw IllegalStateException("Could not fetch all parent legal entities in single request.")
        }
        return parentLegalEntitiesPage.values.associateBy { it.externalId!! }
    }

    fun toCdqModel(site: SiteGateInput, parentLegalEntity: BusinessPartnerCdq?): BusinessPartnerCdq {
        if (parentLegalEntity == null) {
            throw CdqNonexistentParentException(site.legalEntityExternalId)
        }
        val siteCdq = cdqRequestMappingService.toCdqModel(site)
        val parentIdentifiersWithoutBpn = parentLegalEntity.identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }
        return siteCdq.copy(identifiers = siteCdq.identifiers.plus(parentIdentifiersWithoutBpn))
    }
}