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
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AddressService(
    private val addressPersistenceService: AddressPersistenceService,
    private val addressRepository: GateAddressRepository,
    private val sharingStateService: SharingStateService,
    private val businessPartnerService: BusinessPartnerService,
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    fun getAddresses(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<AddressGateInputDto> {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInput(PageRequest.of(page, size), externalIds)

        return PageDto( //TODO totalElements and totalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidLogisticAddressesGeneric(businessPartnerPage).size,
            content = toValidLogisticAddressesGeneric(businessPartnerPage)
        )
    }

    private fun toValidLogisticAddressesGeneric(businessPartnerPage: PageDto<BusinessPartnerInputDto>): List<AddressGateInputDto> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.AdditionalAddress }
            .map { it.toAddressGateInputDto() }
    }

    fun getAddressByExternalId(externalId: String): AddressGateInputDto {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInput(PageRequest.of(0, 1), listOf(externalId))

        val businessPartner = businessPartnerPage.content.firstOrNull { it.postalAddress.addressType == AddressType.AdditionalAddress }
            ?: throw BpdmNotFoundException(("Address does not exist"), externalId)

        return businessPartner.toAddressGateInputDto()

    }

    /**
     * Get output addresses by fetching addresses from the database.
     */
    fun getAddressesOutput(externalIds: Collection<String>? = null, page: Int, size: Int): PageDto<AddressGateOutputDto> {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersOutput(PageRequest.of(page, size), externalIds)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidOutputLogisticAddressesGeneric(businessPartnerPage).size,
            content = toValidOutputLogisticAddressesGeneric(businessPartnerPage),
        )

    }

    private fun toValidOutputLogisticAddressesGeneric(businessPartnerPage: PageDto<BusinessPartnerOutputDto>): List<AddressGateOutputDto> {
        return businessPartnerPage.content
            .filter { it.postalAddress.addressType == AddressType.AdditionalAddress }
            .map { it.toAddressGateOutputDto() }
    }


    /**
     * Upsert addresses input to the database
     **/
    fun upsertAddresses(addresses: Collection<AddressGateInputRequest>) {

        val mappedGBP: MutableCollection<BusinessPartnerInputRequest> = mutableListOf()

        addresses.forEach { address ->

            val duplicateBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.externalId)

            if (duplicateBP != null && (duplicateBP.postalAddress.addressType != AddressType.AdditionalAddress || duplicateBP.postalAddress.addressType != AddressType.LegalAndSiteMainAddress)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "There is already a BP with same ID!")
            }

            if (address.siteExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerDto(address.siteExternalId, BusinessPartnerType.SITE)

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.SiteMainAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
                }

                mappedGBP.add(mapBusinessPartner)

            } else if (address.legalEntityExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerDto(address.legalEntityExternalId, BusinessPartnerType.LEGAL_ENTITY)

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Legal Entity doesn't exist")
                }

                mappedGBP.add(mapBusinessPartner)

            }
        }

        businessPartnerService.upsertBusinessPartnersInput(mappedGBP)


        //addressPersistenceService.persistAddressBP(addresses, StageType.Input)

    }

    /**
     * Upsert addresses output to the database
     **/
    fun upsertOutputAddresses(addresses: Collection<AddressGateOutputRequest>) {

        val mappedGBP: MutableCollection<BusinessPartnerOutputRequest> = mutableListOf()

        addresses.forEach { address ->

            val relatedAddress = businessPartnerRepository.findByStageAndExternalId(StageType.Input, address.externalId)
            if (relatedAddress == null || relatedAddress.postalAddress.addressType != AddressType.AdditionalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't exist")
            }

            val duplicateBP = businessPartnerRepository.findByStageAndExternalId(StageType.Output, address.externalId)
            if (duplicateBP != null && (duplicateBP.postalAddress.addressType != AddressType.AdditionalAddress || duplicateBP.postalAddress.addressType != AddressType.LegalAndSiteMainAddress)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "There is already a BP with same ID!")
            }

            if (address.siteExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerOutputDto(address.siteExternalId, BusinessPartnerType.SITE)

                if (relatedAddress.parentType != mapBusinessPartner.parentType) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't have site relationship")
                }

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.SiteMainAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Site doesn't exist")
                }

                mappedGBP.add(mapBusinessPartner)

            } else if (address.legalEntityExternalId != null) {

                val mapBusinessPartner = address.toBusinessPartnerOutputDto(address.legalEntityExternalId, BusinessPartnerType.LEGAL_ENTITY)

                if (relatedAddress.parentType != mapBusinessPartner.parentType) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Address doesn't have Legal Entity relationship")
                }

                val relatedLE = businessPartnerRepository.findByStageAndExternalId(StageType.Input, mapBusinessPartner.parentId)
                if (relatedLE == null || relatedLE.postalAddress.addressType != AddressType.LegalAddress) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Input Legal Entity doesn't exist")
                }

                mappedGBP.add(mapBusinessPartner)

            }
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

    }

}