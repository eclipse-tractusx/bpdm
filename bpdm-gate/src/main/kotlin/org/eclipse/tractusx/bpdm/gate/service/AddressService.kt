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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartner
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AddressService(
    private val businessPartnerService: BusinessPartnerService,
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    fun getAddresses(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<AddressGateInputDto> {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(page, size), externalIds = externalIds, stage = StageType.Input)

        val validEntities = toValidLogisticAddressesGeneric(businessPartnerPage)

        return PageDto( //TODO TotalElements and TotalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validEntities.size,
            content = validEntities
        )
    }

    private fun toValidLogisticAddressesGeneric(businessPartnerPage: Page<BusinessPartner>): List<AddressGateInputDto> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.AdditionalAddress && checkExistentRelation(StageType.Input, it.parentId) }
            .map { it.toAddressGateInputDto(it.parentId, it.parentType) }
    }

    fun getAddressByExternalId(externalId: String): AddressGateInputDto {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(0, 1), externalIds = listOf(externalId), stage = StageType.Input)

        val businessPartner = businessPartnerPage.content.firstOrNull { it.postalAddress.addressType == AddressType.AdditionalAddress }
            ?: throw BpdmNotFoundException(("Address does not exist"), externalId)

        return businessPartner.toAddressGateInputDto(businessPartner.parentId, businessPartner.parentType)

    }

    /**
     * Get output addresses by fetching addresses from the database.
     */
    fun getAddressesOutput(externalIds: Collection<String>? = null, page: Int, size: Int): PageDto<AddressGateOutputDto> {

        val businessPartnerPage =
            businessPartnerService.getBusinessPartners(pageRequest = PageRequest.of(page, size), externalIds = externalIds, stage = StageType.Output)

        val validEntities = toValidOutputLogisticAddressesGeneric(businessPartnerPage)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = validEntities.size,
            content = validEntities,
        )

    }

    private fun toValidOutputLogisticAddressesGeneric(businessPartnerPage: Page<BusinessPartner>): List<AddressGateOutputDto> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.AdditionalAddress && checkExistentRelation(StageType.Output, it.parentId) }
            .map { it.toAddressGateOutputDto(it.parentId, it.parentType) }
    }

    private fun checkExistentRelation(type: StageType, searchId: String?): Boolean {
        val retrieveBP = businessPartnerRepository.findByStageAndExternalId(type, searchId)
        if (retrieveBP == null || (retrieveBP.postalAddress.addressType != AddressType.SiteMainAddress && retrieveBP.postalAddress.addressType != AddressType.LegalAddress)) {
            return false
        }
        return true
    }

    /**
     * Upsert addresses input to the database
     **/
    fun upsertAddresses(addresses: Collection<AddressGateInputRequest>) {

        var mappedGBP: List<BusinessPartner> = emptyList()

        addresses.forEach { address ->

            val duplicateBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.externalId)

            if (duplicateBP != null && duplicateBP.postalAddress.addressType != AddressType.AdditionalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "There is already a BP with same ID!")
            }

            if (address.siteExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerDtoSite(address.siteExternalId, BusinessPartnerType.SITE)

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.siteExternalId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.SiteMainAddress) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Related Input Site doesn't exist")
                }

                mappedGBP = mappedGBP.plus(mapBusinessPartner)

            } else if (address.legalEntityExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerDtoSite(address.legalEntityExternalId, BusinessPartnerType.LEGAL_ENTITY)

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.legalEntityExternalId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Related Input Legal Entity doesn't exist")
                }

                mappedGBP = mappedGBP.plus(mapBusinessPartner)

            }
        }

        businessPartnerService.upsertBusinessPartnersInput(mappedGBP)

    }

    /**
     * Upsert addresses output to the database
     **/
    fun upsertOutputAddresses(addresses: Collection<AddressGateOutputRequest>) {

        var mappedGBP: List<BusinessPartner> = emptyList()

        addresses.forEach { address ->

            val relatedAddress = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.externalId)
            if (relatedAddress == null || relatedAddress.postalAddress.addressType != AddressType.AdditionalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't exist")
            }

            if (address.siteExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerOutputDtoSite(address.siteExternalId, BusinessPartnerType.SITE)

                if (relatedAddress.parentType != BusinessPartnerType.SITE) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't have site relationship")
                }

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.siteExternalId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.SiteMainAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
                }

                mappedGBP = mappedGBP.plus(mapBusinessPartner)

            } else if (address.legalEntityExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerOutputDtoSite(address.legalEntityExternalId, BusinessPartnerType.LEGAL_ENTITY)

                if (relatedAddress.parentType != BusinessPartnerType.LEGAL_ENTITY) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't have Legal Entity relationship")
                }

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.legalEntityExternalId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Legal Entity doesn't exist")
                }

                mappedGBP = mappedGBP.plus(mapBusinessPartner)

            }
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

    }

}