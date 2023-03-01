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
import org.eclipse.tractusx.bpdm.common.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.FetchResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.isError
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.SaasMappings
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.dto.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError
import org.eclipse.tractusx.bpdm.gate.exception.SaasInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.SaasNonexistentParentException
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val inputSaasMappingService: InputSaasMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val poolClient: PoolClient,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(limit: Int, startAfter: String?): PageStartAfterResponse<SiteGateInputResponse> {
        val sitesPage = saasClient.getSites(limit, startAfter)

        val validEntries = sitesPage.values.filter { validateSiteBusinessPartner(it) }

        return PageStartAfterResponse(
            total = sitesPage.total,
            nextStartAfter = sitesPage.nextStartAfter,
            content = validEntries.map { inputSaasMappingService.toInputSite(it) },
            invalidEntries = sitesPage.values.size - validEntries.size
        )
    }

    fun getSiteByExternalId(externalId: String): SiteGateInputResponse {
        val fetchResponse = saasClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidSiteInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Site", externalId)
        }
    }

    /**
     * Get sites by first fetching sites from "augmented business partners" in SaaS. Augmented business partners from SaaS should contain a BPN,
     * which is then used to fetch the data for the sites from the bpdm pool.
     */
    fun getSitesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageOutputResponse<SiteGateOutput> {
        val augmentedPartnerResponse = saasClient.getAugmentedSites(limit = limit, startAfter = startAfter, externalIds = externalIds)
        val augmentedPartnerWrapperCollection = augmentedPartnerResponse.values

        val bpnByExternalIdMap = outputSaasMappingService.buildBpnByExternalIdMap(augmentedPartnerWrapperCollection)

        val bpnList = bpnByExternalIdMap.values.filterNotNull()
        val sitesByBpnMap = poolClient.searchSites(bpnList).associateBy { it.site.bpn }
        val mainAddressesByBpnMap = poolClient.searchMainAddresses(bpnList).associateBy { it.site }

        if (bpnList.size > sitesByBpnMap.size) {
            logger.warn { "Requested ${bpnList.size} sites from pool, but only ${sitesByBpnMap.size} were found." }
        }
        if (bpnList.size > mainAddressesByBpnMap.size) {
            logger.warn { "Requested ${bpnList.size} main addresses of sites from pool, but only ${mainAddressesByBpnMap.size} were found." }
        }

        // We need the sharing status from BusinessPartnerSaas
        val partnerResponse = saasClient.getSites(externalIds = bpnByExternalIdMap.keys)
        val partnerByExternalIdMap = partnerResponse.values.associateBy { it.externalId }

        // We sort all the entries in one of 3 buckets: valid content, errors or still pending
        val validContent = mutableListOf<SiteGateOutput>()
        val errors = mutableListOf<ErrorInfo<BusinessPartnerOutputError>>()
        val pendingExternalIds = mutableListOf<String>()

        for ((externalId, bpn) in bpnByExternalIdMap) {
            // Business partner sharing
            val partner = partnerByExternalIdMap[externalId]
            val sharingStatus = partner?.metadata?.sharingStatus
            val sharingStatusType = sharingStatus?.status

            if (sharingStatusType == null || sharingStatusType.isError()) {
                // ERROR: SharingProcessError
                errors.add(
                    outputSaasMappingService.buildErrorInfoSharingProcessError(externalId, sharingStatus)
                )
            } else if (bpn != null) {
                val site = sitesByBpnMap[bpn]
                val mainAddress = mainAddressesByBpnMap[bpn]
                if (site != null && mainAddress != null) {
                    // OKAY: entry found in pool
                    validContent.add(
                        toSiteOutput(externalId, site, mainAddress)
                    )
                } else {
                    // ERROR: BpnNotInPool
                    errors.add(
                        outputSaasMappingService.buildErrorInfoBpnNotInPool(externalId, bpn)
                    )
                }
            } else if (outputSaasMappingService.isSharingTimeoutReached(partner)) {
                // ERROR: SharingTimeout
                errors.add(
                    outputSaasMappingService.buildErrorInfoSharingTimeout(externalId, partner.lastModifiedAt)
                )
            } else {
                pendingExternalIds.add(externalId)
            }
        }

        return PageOutputResponse(
            total = augmentedPartnerResponse.total,
            nextStartAfter = augmentedPartnerResponse.nextStartAfter,
            content = validContent,
            invalidEntries = augmentedPartnerWrapperCollection.size - validContent.size, // difference between all entries from SaaS and valid content
            pending = pendingExternalIds,
            errors = errors,
        )
    }

    fun toSiteOutput(externalId: String, site: SitePartnerSearchResponse, mainAddress: MainAddressSearchResponse) =
        SiteGateOutput(
            site = SiteResponse(
                name = site.site.name
            ),
            mainAddress = mainAddress.mainAddress,
            externalId = externalId,
            bpn = site.site.bpn,
            legalEntityBpn = site.bpnLegalEntity
        )

    /**
     * Upsert sites by:
     *
     * - Retrieving parent legal entities to check whether they exist and since their identifiers are copied to site
     * - Upserting the sites
     * - Retrieving the old relations of the sites and deleting them
     * - Upserting the new relations
     */
    fun upsertSites(sites: Collection<SiteGateInputRequest>) {
        val sitesSaas = toSaasModels(sites)
        saasClient.upsertSites(sitesSaas)

        deleteRelationsOfSites(sites)

        upsertRelations(sites)
    }

    /**
     * Fetches parent information and converts the given [sites] to their corresponding SaaS models
     */
    fun toSaasModels(sites: Collection<SiteGateInputRequest>): Collection<BusinessPartnerSaas> {
        val parentLegalEntitiesByExternalId = getParentLegalEntities(sites)
        return sites.map { toSaasModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId]) }
    }

    private fun upsertRelations(sites: Collection<SiteGateInputRequest>) {
        val relations = sites.map {
            SaasClient.SiteLegalEntityRelation(
                siteExternalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId
            )
        }.toList()
        saasClient.upsertSiteRelations(relations)
    }

    private fun deleteRelationsOfSites(sites: Collection<SiteGateInputRequest>) {
        val sitesPage = saasClient.getSites(externalIds = sites.map { it.externalId })
        val relationsToDelete = sitesPage.values.flatMap { it.relations }.map { SaasMappings.toRelationToDelete(it) }
        if (relationsToDelete.isNotEmpty()) {
            saasClient.deleteRelations(relationsToDelete)
        }
    }

    private fun toValidSiteInput(partner: BusinessPartnerSaas): SiteGateInputResponse {
        if (!validateSiteBusinessPartner(partner)) {
            throw SaasInvalidRecordException(partner.id)
        }
        return inputSaasMappingService.toInputSite(partner)
    }

    private fun validateSiteBusinessPartner(partner: BusinessPartnerSaas): Boolean {
        var valid = true
        val logMessageStart = "SaaS business partner for site with ${if (partner.id != null) "ID " + partner.id else "external id " + partner.externalId}"

        valid = valid && validateAddresses(partner, logMessageStart)
        valid = valid && validateLegalEntityParents(partner, logMessageStart)
        valid = valid && validateNames(partner, logMessageStart)

        return valid
    }

    private fun validateNames(partner: BusinessPartnerSaas, logMessageStart: String): Boolean {
        if (partner.names.size > 1) {
            logger.warn { "$logMessageStart has multiple names." }
        }
        if (partner.names.isEmpty()) {
            logger.warn { "$logMessageStart does not have a name." }
            return false
        }
        return true
    }

    private fun validateAddresses(partner: BusinessPartnerSaas, logMessageStart: String): Boolean {
        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple main addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have a main address" }
            return false
        }
        return true
    }

    private fun validateLegalEntityParents(partner: BusinessPartnerSaas, logMessageStart: String): Boolean {
        val numLegalEntityParents = inputSaasMappingService.toParentLegalEntityExternalIds(partner.relations).size
        if (numLegalEntityParents > 1) {
            logger.warn { "$logMessageStart has multiple parent legal entities." }
        }
        if (numLegalEntityParents == 0) {
            logger.warn { "$logMessageStart does not have a parent legal entity." }
            return false
        }
        return true
    }

    private fun getParentLegalEntities(sites: Collection<SiteGateInputRequest>): Map<String, BusinessPartnerSaas> {
        val parentLegalEntityExternalIds = sites.map { it.legalEntityExternalId }.distinct().toList()
        val parentLegalEntitiesPage = saasClient.getLegalEntities(externalIds = parentLegalEntityExternalIds)
        if (parentLegalEntitiesPage.limit < parentLegalEntityExternalIds.size) {
            // should not happen as long as configured upsert limit is lower than SaaS's limit
            throw IllegalStateException("Could not fetch all parent legal entities in single request.")
        }
        return parentLegalEntitiesPage.values.associateBy { it.externalId!! }
    }

    private fun toSaasModel(site: SiteGateInputRequest, parentLegalEntity: BusinessPartnerSaas?): BusinessPartnerSaas {
        if (parentLegalEntity == null) {
            throw SaasNonexistentParentException(site.legalEntityExternalId)
        }
        val siteSaas = saasRequestMappingService.toSaasModel(site)
        val parentIdentifiersWithoutBpn = parentLegalEntity.identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }
        return siteSaas.copy(identifiers = siteSaas.identifiers.plus(parentIdentifiersWithoutBpn))
    }
}