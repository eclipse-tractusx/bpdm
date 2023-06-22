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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.dto.response.PoolLegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.PoolLegalEntityApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class LegalEntityController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val businessPartnerBuildService: BusinessPartnerBuildService,
    val searchService: SearchService,
    val bpnConfigProperties: BpnConfigProperties,
    val controllerConfigProperties: ControllerConfigProperties,
    val siteService: SiteService,
    val addressService: AddressService
) : PoolLegalEntityApi {


    override fun getLegalEntities(
        bpSearchRequest: LegalEntityPropertiesSearchDto,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityMatchVerboseDto> {
        return searchService.searchLegalEntities(
            BusinessPartnerSearchDto(bpSearchRequest),
            paginationRequest
        )
    }

    override fun getLegalEntity(idValue: String, idType: String?): PoolLegalEntityVerboseDto {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findLegalEntityIgnoreCase(idValue.uppercase())
        else businessPartnerFetchService.findLegalEntityIgnoreCase(actualType, idValue)
    }


    override fun setLegalEntityCurrentness(bpnl: String) {
        businessPartnerBuildService.setBusinessPartnerCurrentness(bpnl.uppercase())
    }

    override fun searchSites(
        bpnLs: Collection<String>
    ): ResponseEntity<Collection<PoolLegalEntityVerboseDto>> {
        if (bpnLs.size > controllerConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity(businessPartnerFetchService.fetchDtosByBpns(bpnLs), HttpStatus.OK)
    }

    override fun getSites(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<SiteVerboseDto> {
        return siteService.findByPartnerBpn(bpnl.uppercase(), paginationRequest.page, paginationRequest.size)
    }


    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return addressService.findByPartnerBpn(bpnl.uppercase(), paginationRequest.page, paginationRequest.size)
    }


    override fun searchLegalAddresses(
        bpnLs: Collection<String>
    ): Collection<LegalAddressVerboseDto> {
        return addressService.findLegalAddresses(bpnLs)
    }


    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateDto>
    ): LegalEntityPartnerCreateResponseWrapper {
        return businessPartnerBuildService.createLegalEntities(businessPartners)
    }

    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateDto>
    ): LegalEntityPartnerUpdateResponseWrapper {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }

}