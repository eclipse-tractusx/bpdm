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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class SiteService(
    private val bpnConfigProperties: BpnConfigProperties,
    private val sitePersistenceService: SitePersistenceService,
    private val siteRepository: SiteRepository,
    private val sharingStateService: SharingStateService,
    private val businessPartnerService: BusinessPartnerService,
    private val businessPartnerRepository: BusinessPartnerRepository
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<SiteGateInputDto> {

        val sitesPage = if (externalIds != null) {
            siteRepository.findByExternalIdInAndStage(externalIds, StageType.Input, PageRequest.of(page, size))
        } else {
            siteRepository.findByStage(StageType.Input, PageRequest.of(page, size))
        }

        return PageDto(
            page = page,
            totalElements = sitesPage.totalElements,
            totalPages = sitesPage.totalPages,
            contentSize = sitesPage.content.size,
            content = toValidSite(sitesPage)
        )
    }

    private fun toValidSite(sitePage: Page<Site>): List<SiteGateInputDto> {
        return sitePage.content.map { site ->
            site.toSiteGateInputResponse(site)
        }
    }

    fun getSiteByExternalId(externalId: String): SiteGateInputDto {
        val siteRecord = siteRepository.findByExternalIdAndStage(externalId, StageType.Input) ?: throw BpdmNotFoundException("Site", externalId)

        return siteRecord.toSiteGateInputResponse(siteRecord)
    }

    /**
     * Get output sites by first fetching sites from the database
     */
    fun getSitesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageDto<SiteGateOutputResponse> {

        val sitePage = if (!externalIds.isNullOrEmpty()) {
            siteRepository.findByExternalIdInAndStage(externalIds, StageType.Output, PageRequest.of(page, size))
        } else {
            siteRepository.findByStage(StageType.Output, PageRequest.of(page, size))
        }

        return PageDto(
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

        val mappedGBP: MutableCollection<BusinessPartnerInputRequest> = mutableListOf()

        sites.forEach { site ->
            val mapBusinessPartner = site.toBusinessPartnerDto()

            val duplicateBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, site.externalId)
            if (mapBusinessPartner.parentId == null || mapBusinessPartner.parentType == null || duplicateBP != null && duplicateBP.postalAddress.addressType != AddressType.SiteMainAddress) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No parentID or Parent Type have correct values or there is already" +
                            "a BP with same ID!"
                )
            }

            val retrieveBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)

            if (retrieveBP == null || retrieveBP.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Legal Entity doesn't exist")
            }

            mappedGBP.add(mapBusinessPartner)
        }

        businessPartnerService.upsertBusinessPartnersInput(mappedGBP)

    }

    /**
     * Upsert sites output to the database
     **/
    fun upsertSitesOutput(sites: Collection<SiteGateOutputRequest>) {

        val mappedGBP: MutableCollection<BusinessPartnerOutputRequest> = mutableListOf()

        sites.forEach { site ->
            val mapBusinessPartner = site.toBusinessPartnerOutputDto()

            val duplicateBP = businessPartnerRepository.findByStageAndExternalId(StageType.Output, site.externalId)
            if (mapBusinessPartner.parentId == null || mapBusinessPartner.parentType == null || duplicateBP != null && duplicateBP.postalAddress.addressType != AddressType.SiteMainAddress) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No parentID or Parent Type have correct values or there is already" +
                            "a BP with same ID!"
                )
            }

            val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Output, mapBusinessPartner.parentId)
            if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Output Legal Entity doesn't exist")
            }

            val relatedSite = businessPartnerRepository.findByStageAndExternalId(StageType.Input, site.externalId)
            if (relatedSite == null || relatedSite.postalAddress.addressType != AddressType.SiteMainAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
            }

            mappedGBP.add(mapBusinessPartner)
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

        //sitePersistenceService.persistSitesOutputBP(sites, StageType.Output)
    }

}