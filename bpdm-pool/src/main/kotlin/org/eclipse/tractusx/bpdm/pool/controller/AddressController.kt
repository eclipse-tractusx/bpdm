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

import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.PoolAddressApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressService: AddressService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val searchService: SearchService
) : PoolAddressApi {


    override fun getAddresses(
        addressSearchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<AddressMatchResponse> {

        return searchService.searchAddresses(addressSearchRequest, paginationRequest)
    }

    override fun getAddress(
        bpn: String
    ): AddressPartnerSearchResponse {
        return addressService.findByBpn(bpn.uppercase())
    }

    override fun searchAddresses(
        addressSearchRequest: AddressPartnerBpnSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<AddressPartnerSearchResponse> {

        return addressService.findByPartnerAndSiteBpns(addressSearchRequest, pageRequest)
    }


    override fun createAddresses(
        requests: Collection<AddressPartnerCreateRequest>
    ): Collection<AddressPartnerCreateResponse> {
        return businessPartnerBuildService.createAddresses(requests)
    }


    override fun updateAddresses(
        requests: Collection<AddressPartnerUpdateRequest>
    ): Collection<AddressPartnerResponse> {
        return businessPartnerBuildService.updateAddresses(requests)
    }
}