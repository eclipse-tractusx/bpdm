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

import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.pool.client.dto.request.*
import org.eclipse.tractusx.bpdm.pool.client.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.client.service.PoolClientLegalEntityInterface
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
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
    val siteService: SiteService,
    val addressService: AddressService
) : PoolClientLegalEntityInterface {


    override fun getLegalEntities(
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {
        return searchService.searchLegalEntities(
            BusinessPartnerSearchRequest(bpSearchRequest, addressSearchRequest, siteSearchRequest),
            paginationRequest
        )
    }

    override fun getLegalEntity(idValue: String, idType: String?): LegalEntityPartnerResponse {
        val actualType = idType ?: bpnConfigProperties.id
        return if (actualType == bpnConfigProperties.id) businessPartnerFetchService.findLegalEntityIgnoreCase(idValue.uppercase())
        else businessPartnerFetchService.findLegalEntityIgnoreCase(actualType, idValue)
    }


    override fun setLegalEntityCurrentness(bpn: String) {
        businessPartnerBuildService.setBusinessPartnerCurrentness(bpn.uppercase())
    }


    override fun searchSites(
        bpnLs: Collection<String>
    ): ResponseEntity<Collection<LegalEntityPartnerResponse>> {
        if (bpnLs.size > bpnConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity(businessPartnerFetchService.fetchDtosByBpns(bpnLs), HttpStatus.OK)
    }

    override fun getSites(
        bpn: String,
        paginationRequest: PaginationRequest
    ): PageResponse<SitePartnerResponse> {
        return siteService.findByPartnerBpn(bpn.uppercase(), paginationRequest.page, paginationRequest.size)
    }


    override fun getAddresses(
        bpn: String,
        paginationRequest: PaginationRequest
    ): PageResponse<AddressPartnerResponse> {
        return addressService.findByPartnerBpn(bpn.uppercase(), paginationRequest.page, paginationRequest.size)
    }


    override fun searchLegalAddresses(
        bpnLs: Collection<String>
    ): Collection<LegalAddressSearchResponse> {
        return addressService.findLegalAddresses(bpnLs)
    }


    override fun createBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerCreateRequest>
    ): Collection<LegalEntityPartnerCreateResponse> {
        return businessPartnerBuildService.createLegalEntities(businessPartners)
    }


    override fun updateBusinessPartners(
        businessPartners: Collection<LegalEntityPartnerUpdateRequest>
    ): Collection<LegalEntityPartnerCreateResponse> {
        return businessPartnerBuildService.updateLegalEntities(businessPartners)
    }


}