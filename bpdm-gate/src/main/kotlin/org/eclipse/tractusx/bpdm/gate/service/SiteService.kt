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
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateOutputResponse
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
    private val saasClient: SaasClient,
    private val bpnConfigProperties: BpnConfigProperties,
    private val changelogRepository: ChangelogRepository,
    private val sitePersistenceService: SitePersistenceService,
    private val siteRepository: SiteRepository
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(page: Int, size: Int, externalIds: Collection<String>? = null): PageResponse<SiteGateInputResponse> {

        val sitesPage = if (externalIds != null) {
            siteRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Input, PageRequest.of(page, size))
        } else {
            siteRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
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
        val siteRecord = siteRepository.findByExternalIdAndDataType(externalId, OutputInputEnum.Input) ?: throw BpdmNotFoundException("Site", externalId)

        return siteRecord.toSiteGateInputResponse(siteRecord)
    }

    /**
     * Get output sites by first fetching sites from the database
     */
    fun getSitesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageResponse<SiteGateOutputResponse> {

        val sitePage = if (externalIds != null && externalIds.isNotEmpty()) {
            siteRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Output, PageRequest.of(page, size))
        } else {
            siteRepository.findByDataType(OutputInputEnum.Output, PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = sitePage.totalElements,
            totalPages = sitePage.totalPages,
            contentSize = sitePage.content.size,
            content = toValidOutputSites(sitePage),
        )

    }

    private fun toValidOutputSites(sitePage: Page<Site>): List<SiteGateOutputResponse> {
        return sitePage.content.map { sites ->
            sites.toSiteGateOutputResponse(sites)
        }
    }

    /**
     * Upsert sites input to the database
     **/
    fun upsertSites(sites: Collection<SiteGateInputRequest>) {

        sites.forEach { site ->
            changelogRepository.save(ChangelogEntry(site.externalId, LsaType.Site))
        }

        sitePersistenceService.persistSitesBP(sites, OutputInputEnum.Input)
    }

    /**
     * Upsert sites output to the database
     **/
    fun upsertSitesOutput(sites: Collection<SiteGateOutputRequest>) {

        sitePersistenceService.persistSitesOutputBP(sites, OutputInputEnum.Output)
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