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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
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

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInput(PageRequest.of(page, size), externalIds)

        return PageDto( //TODO totalElements and totalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidSiteGeneric(businessPartnerPage).size,
            content = toValidSiteGeneric(businessPartnerPage)
        )
    }

    private fun toValidSiteGeneric(businessPartnerPage: PageDto<BusinessPartnerInputDto>): List<SiteGateInputDto> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.SiteMainAddress }
            .map { it.toSiteGateInputDto() }
    }

    fun getSiteByExternalId(externalId: String): SiteGateInputDto {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInput(PageRequest.of(0, 1), listOf(externalId))

        val businessPartner = businessPartnerPage.content.firstOrNull { it.postalAddress.addressType == AddressType.SiteMainAddress }
            ?: throw BpdmNotFoundException(("site does not exist"), externalId)

        return businessPartner.toSiteGateInputDto()

    }

    /**
     * Get output sites by first fetching sites from the database
     */
    fun getSitesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageDto<SiteGateOutputResponse> {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersOutput(PageRequest.of(page, size), externalIds)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidOutputSitesGeneric(businessPartnerPage).size,
            content = toValidOutputSitesGeneric(businessPartnerPage),
        )
    }

    private fun toValidOutputSitesGeneric(businessPartnerPage: PageDto<BusinessPartnerOutputDto>): List<SiteGateOutputResponse> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.AdditionalAddress }
            .map { it.toSiteGateOutputResponse() }
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