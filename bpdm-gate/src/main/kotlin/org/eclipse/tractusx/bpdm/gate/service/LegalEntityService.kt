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
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.wrapper.BusinessPartnerInputDtoWrapper
import org.eclipse.tractusx.bpdm.gate.api.model.wrapper.BusinessPartnerOutputDtoWrapper
import org.eclipse.tractusx.bpdm.gate.api.model.wrapper.BusinessPartnerWrapperInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.wrapper.BusinessPartnerWrapperOutputRequest
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class LegalEntityService(
    private val businessPartnerService: BusinessPartnerService,
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Upsert legal entities input to the database
     **/
    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>) {

        val mappedGBP: MutableCollection<BusinessPartnerWrapperInputRequest> = mutableListOf()

        legalEntities.forEach { legalEntity ->

            val mapBusinessPartner = legalEntity.toBusinessPartnerDto()

            mappedGBP.add(BusinessPartnerWrapperInputRequest(mapBusinessPartner, null, null))
        }

        businessPartnerService.upsertBusinessPartnersInput(mappedGBP)

    }

    /**
     * Upsert legal entities output to the database
     **/
    fun upsertLegalEntitiesOutput(legalEntities: Collection<LegalEntityGateOutputRequest>) {

        val mappedGBP: MutableCollection<BusinessPartnerWrapperOutputRequest> = mutableListOf()

        legalEntities.forEach { legalEntity ->

            val mapBusinessPartner = legalEntity.toBusinessPartnerOutputDto()

            val retrieveBP = businessPartnerRepository.findByStageAndExternalId(StageType.Input, legalEntity.externalId)
            if (retrieveBP == null || retrieveBP.postalAddress.addressType != AddressType.LegalAddress) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Related Output Legal Entity doesn't exist")
            }

            mappedGBP.add(BusinessPartnerWrapperOutputRequest(mapBusinessPartner, null, null))
        }

        businessPartnerService.upsertBusinessPartnersOutput(mappedGBP)

    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputDto {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInputLSA(PageRequest.of(0, 1), listOf(externalId))

        val businessPartner = businessPartnerPage.content.firstOrNull { it.businessPartner.postalAddress.addressType == AddressType.LegalAddress }
            ?: throw BpdmNotFoundException(("LegalEntity does not exist"), externalId)

        return businessPartner.businessPartner.toLegalEntityGateInputDto()
    }

    fun getLegalEntities(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<LegalEntityGateInputDto> {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersInputLSA(PageRequest.of(page, size), externalIds)

        return PageDto( //TODO totalElements and totalPages need change
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidLegalEntitiesGeneric(businessPartnerPage).size,
            content = toValidLegalEntitiesGeneric(businessPartnerPage)
        )
    }

    private fun toValidLegalEntitiesGeneric(businessPartnerPage: PageDto<BusinessPartnerInputDtoWrapper>): List<LegalEntityGateInputDto> {
        return businessPartnerPage.content
            .filter { it.businessPartner.postalAddress.addressType == AddressType.LegalAddress || it.businessPartner.postalAddress.addressType == AddressType.LegalAndSiteMainAddress }
            .map { it.businessPartner.toLegalEntityGateInputDto() }
    }

    /**
     * Get output legal entities by first fetching legal entities from the database
     */
    fun getLegalEntitiesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageDto<LegalEntityGateOutputResponse> {

        val businessPartnerPage = businessPartnerService.getBusinessPartnersOutputLSA(PageRequest.of(page, size), externalIds)

        return PageDto(
            page = page,
            totalElements = businessPartnerPage.totalElements,
            totalPages = businessPartnerPage.totalPages,
            contentSize = toValidOutputLegalEntitiesGeneric(businessPartnerPage).size,
            content = toValidOutputLegalEntitiesGeneric(businessPartnerPage),
        )

    }

    private fun toValidOutputLegalEntitiesGeneric(businessPartnerPage: PageDto<BusinessPartnerOutputDtoWrapper>): List<LegalEntityGateOutputResponse> {
        return businessPartnerPage.content
            .filter { it.businessPartner.postalAddress.addressType == AddressType.LegalAddress || it.businessPartner.postalAddress.addressType == AddressType.LegalAndSiteMainAddress }
            .map { it.businessPartner.toLegalEntityGateOutputResponse() }
    }

}
