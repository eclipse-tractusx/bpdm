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
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.service.SearchService
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.PoolSecurityConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class LegalEntityController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val businessPartnerBuildService: BusinessPartnerBuildService,
    val searchService: SearchService,
    val bpnConfigProperties: BpnConfigProperties,
    val controllerConfigProperties: ControllerConfigProperties,
    val siteService: SiteService,
    val addressService: AddressService,
    val poolSecurityConfigProperties: PoolSecurityConfigProperties
) : PoolLegalEntityApi {

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun getLegalEntities(
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityMatchVerboseDto> {
        return searchService.searchLegalEntities(
            BusinessPartnerSearchRequest(bpSearchRequest),
            paginationRequest
        )
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun getLegalEntity(idValue: String, idType: String?): PoolLegalEntityVerboseDto {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findLegalEntityIgnoreCase(idValue.uppercase())
        else businessPartnerFetchService.findLegalEntityIgnoreCase(actualType, idValue)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getChangePoolPartnerDataAsRole())")
    override fun setLegalEntityCurrentness(bpnl: String) {
        businessPartnerBuildService.setBusinessPartnerCurrentness(bpnl.uppercase())
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
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

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun getAddresses(
        bpnl: String,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto> {
        return addressService.findByPartnerBpn(bpnl.uppercase(), paginationRequest.page, paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun searchLegalAddresses(
        bpnLs: Collection<String>
    ): Collection<LegalAddressVerboseDto> {
        return addressService.findLegalAddresses(bpnLs)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getChangePoolPartnerDataAsRole())")
    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateRequest>
    ): LegalEntityPartnerCreateResponseWrapper {
        return businessPartnerBuildService.createLegalEntities(businessPartners)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getChangePoolPartnerDataAsRole())")
    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateRequest>
    ): LegalEntityPartnerUpdateResponseWrapper {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }
}
