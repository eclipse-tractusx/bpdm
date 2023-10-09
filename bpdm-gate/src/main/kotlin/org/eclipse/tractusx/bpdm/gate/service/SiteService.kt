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
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartner
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class SiteService(
    private val businessPartnerService: BusinessPartnerService,
    private val businessPartnerRepository: BusinessPartnerRepository
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<SiteGateInputDto> {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(page, size), externalIds = externalIds, stage = StageType.Input)

        val validData = toValidSiteGeneric(businessPartnerPage)

        return PageDto( //TODO totalElements and totalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validData.size,
            content = validData
        )
    }

    private fun toValidSiteGeneric(businessPartnerPage: Page<BusinessPartner>): List<SiteGateInputDto> {
        return businessPartnerPage.content
            .filter {
                it.postalAddress.addressType == AddressType.SiteMainAddress || it.postalAddress.addressType == AddressType.LegalAndSiteMainAddress
                        && checkExistentRelation(StageType.Input, it.parentId)
            }
            .map { it.toSiteGateInputDto(it.parentId) }
    }

    fun getSiteByExternalId(externalId: String): SiteGateInputDto {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(0, 1), externalIds = listOf(externalId), stage = StageType.Input)

        val businessPartner = businessPartnerPage.content.firstOrNull { it.postalAddress.addressType == AddressType.SiteMainAddress }
            ?: throw BpdmNotFoundException(("site does not exist"), externalId)

        return businessPartner.toSiteGateInputDto(businessPartner.parentId)

    }

    /**
     * Get output sites by first fetching sites from the database
     */
    fun getSitesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageDto<SiteGateOutputResponse> {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(page, size), externalIds = externalIds, stage = StageType.Output)

        val validData = toValidOutputSitesGeneric(businessPartnerPage)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validData.size,
            content = validData,
        )
    }

    private fun toValidOutputSitesGeneric(businessPartnerPage: Page<BusinessPartner>): List<SiteGateOutputResponse> {
        return businessPartnerPage.content
            .filter {
                (it.postalAddress.addressType == AddressType.SiteMainAddress || it.postalAddress.addressType == AddressType.LegalAndSiteMainAddress)
                        && checkExistentRelation(StageType.Output, it.parentId)
            }
            .map { it.toSiteGateOutputResponse(it.parentId) }
    }

    private fun checkExistentRelation(type: StageType, searchId: String?): Boolean {
        val retrieveBP = businessPartnerRepository.findByStageAndExternalId(type, searchId)
        if (retrieveBP == null || retrieveBP.postalAddress.addressType != AddressType.LegalAddress) {
            return false
        }
        return true
    }

    /**
     * Upsert sites input to the database
     **/
    fun upsertSites(sites: Collection<SiteGateInputRequest>) {

        var mappedGBP: List<BusinessPartner> = emptyList()

        sites.forEach { site ->
            val mapBusinessPartner = site.toBusinessPartnerDtoSite()

            val retrieveBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, site.legalEntityExternalId)
            if (retrieveBP == null || retrieveBP.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Related Legal Entity doesn't exist")
            }

            mappedGBP = mappedGBP.plus(mapBusinessPartner)
        }

        businessPartnerService.upsertBusinessPartnersInput(mappedGBP)

    }

    /**
     * Upsert sites output to the database
     **/
    fun upsertSitesOutput(sites: Collection<SiteGateOutputRequest>) {

        var mappedGBP: List<BusinessPartner> = emptyList()

        sites.forEach { site ->

            val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Output, site.legalEntityExternalId)
            if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Output Legal Entity doesn't exist")
            }

            val mapBusinessPartner = site.toBusinessPartnerOutputDtoSite()

            val relatedSite = businessPartnerRepository.findByStageAndExternalId(StageType.Input, site.externalId)
            if (relatedSite == null || relatedSite.postalAddress.addressType != AddressType.SiteMainAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
            }

            mappedGBP = mappedGBP.plus(mapBusinessPartner)
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

    }

}