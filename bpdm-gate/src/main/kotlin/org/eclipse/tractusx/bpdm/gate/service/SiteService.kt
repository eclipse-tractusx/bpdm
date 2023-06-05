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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.response.LogisticAddressGateResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.exception.SaasNonexistentParentException
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val poolClient: PoolClient,
    private val bpnConfigProperties: BpnConfigProperties,
    private val changelogRepository: ChangelogRepository,
    private val sitePersistenceService: SitePersistenceService,
    private val siteRepository: SiteRepository
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(page: Int, size: Int, externalIds: Collection<String>? = null): PageResponse<SiteGateInputResponse> {

        val sitesPage = if (externalIds != null) {
            siteRepository.findByExternalIdIn(externalIds, PageRequest.of(page, size))
        } else {
            siteRepository.findAll(PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = sitesPage.totalElements,
            totalPages = sitesPage.totalPages,
            contentSize = sitesPage.content.size,
            content = toValidSite(sitesPage)
        )
    }

    private fun toValidSite(sitePage: Page<Site>): List<SiteGateInputResponse> {
        return sitePage.content.map { site ->
            site.toSiteGateInputResponse(site)
        }
    }

    fun getSiteByExternalId(externalId: String): SiteGateInputResponse {
        val siteRecord = siteRepository.findByExternalId(externalId) ?: throw BpdmNotFoundException("Site", externalId)

        return siteRecord.toSiteGateInputResponse(siteRecord)
    }

    /**
     * Get sites by first fetching sites from "augmented business partners" in SaaS. Augmented business partners from SaaS should contain a BPN,
     * which is then used to fetch the data for the sites from the bpdm pool.
     */
    fun getSitesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageOutputResponse<SiteGateOutput> {
        val partnerResponse = saasClient.getSites(limit = limit, startAfter = startAfter, externalIds = externalIds)
        val partners = partnerResponse.values

        val partnersWithExternalId = outputSaasMappingService.mapWithExternalId(partners)
        val augmentedPartnerResponse = saasClient.getAugmentedSites(externalIds = partnersWithExternalId.map { it.externalId })
        val partnersWithLocalBpn = outputSaasMappingService.mapWithLocalBpn(partnersWithExternalId, augmentedPartnerResponse.values)

        val bpnSet = partnersWithLocalBpn.map { it.bpn }.toSet()
        val sitesByBpnMap = poolClient.searchSites(bpnSet).associateBy { it.bpns }
        val mainAddressesByBpnMap = poolClient.searchMainAddresses(bpnSet).associateBy { it.bpnSite }

        if (bpnSet.size > sitesByBpnMap.size) {
            logger.warn { "Requested ${bpnSet.size} sites from pool, but only ${sitesByBpnMap.size} were found." }
        }
        if (bpnSet.size > mainAddressesByBpnMap.size) {
            logger.warn { "Requested ${bpnSet.size} main addresses of sites from pool, but only ${mainAddressesByBpnMap.size} were found." }
        }

        //Filter only sites which can be found with their main address  in the Pool under the given local BPN
        val partnersWithPoolBpn = partnersWithLocalBpn.filter { sitesByBpnMap[it.bpn] != null && mainAddressesByBpnMap[it.bpn] != null }
        val bpnByExternalIdMap = partnersWithPoolBpn.map { Pair(it.partner.externalId!!, it.bpn) }.toMap()

        //Evaluate the sharing status of the legal entities
        val sharingStatus = outputSaasMappingService.evaluateSharingStatus(partners, partnersWithLocalBpn, partnersWithPoolBpn)

        val validSites = sharingStatus.validExternalIds.map { externalId ->
            val bpn = bpnByExternalIdMap[externalId]!!
            val site = sitesByBpnMap[bpn]!!
            val mainAddress = mainAddressesByBpnMap[bpn]!!
            toSiteOutput(externalId, site, mainAddress)
        }

        return PageOutputResponse(
            total = partnerResponse.total,
            nextStartAfter = partnerResponse.nextStartAfter,
            content = validSites,
            invalidEntries = partners.size - sharingStatus.validExternalIds.size, // difference between all entries from SaaS and valid content
            pending = sharingStatus.pendingExternalIds,
            errors = sharingStatus.errors,
        )
    }

    fun toSiteOutput(externalId: String, site: SiteResponse, mainAddress: LogisticAddressGateResponse) =
        SiteGateOutput(
            site = site,
            mainAddress = mainAddress,
            externalId = externalId
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

        // create changelog entry if all goes well from saasClient
        sites.forEach { site ->
            changelogRepository.save(ChangelogEntry(site.externalId, LsaType.Site))
        }

        sitePersistenceService.persistSitesBP(sites)
    }

    /**
     * Fetches parent information and converts the given [sites] to their corresponding SaaS models
     */
    fun toSaasModels(sites: Collection<SiteGateInputRequest>): Collection<BusinessPartnerSaas> {
        val parentLegalEntitiesByExternalId = getParentLegalEntities(sites)
        return sites.map { toSaasModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId]) }
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