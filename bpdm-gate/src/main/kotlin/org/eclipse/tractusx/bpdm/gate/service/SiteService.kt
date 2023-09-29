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
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
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

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInput(PageRequest.of(page, size), externalIds)

        val validData = toValidSiteGeneric(businessPartnerPage)

        return PageDto( //TODO totalElements and totalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validData.size,
            content = validData
        )
    }

    private fun toValidSiteGeneric(businessPartnerPage: PageDto<BusinessPartnerInputDto>): List<SiteGateInputDto> {
        return businessPartnerPage.content
            .filter {
                it.postalAddress.addressType == AddressType.SiteMainAddress || it.postalAddress.addressType == AddressType.LegalAndSiteMainAddress
                        && checkExistentRelation(StageType.Input, it.parentId)
            }
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

        val validData = toValidOutputSitesGeneric(businessPartnerPage)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validData.size,
            content = validData,
        )
    }

    private fun toValidOutputSitesGeneric(businessPartnerPage: PageDto<BusinessPartnerOutputDto>): List<SiteGateOutputResponse> {
        return businessPartnerPage.content
            .filter {
                (it.postalAddress.addressType == AddressType.SiteMainAddress || it.postalAddress.addressType == AddressType.LegalAndSiteMainAddress)
                        && checkExistentRelation(StageType.Output, it.parentId)
            }
            .map { it.toSiteGateOutputResponse() }
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

        val mappedGBP: MutableCollection<BusinessPartnerInputRequest> = mutableListOf()

        sites.forEach { site ->
            val mapBusinessPartner = site.toBusinessPartnerDto()

            val retrieveBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)
            if (retrieveBP == null || retrieveBP.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Related Legal Entity doesn't exist")
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

            val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Output, site.legalEntityExternalId)
            if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Output Legal Entity doesn't exist")
            }

            val mapBusinessPartner = site.toBusinessPartnerOutputDto()

            val relatedSite = businessPartnerRepository.findByStageAndExternalId(StageType.Input, site.externalId)
            if (relatedSite == null || relatedSite.postalAddress.addressType != AddressType.SiteMainAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
            }

            mappedGBP.add(mapBusinessPartner)
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

    }

}