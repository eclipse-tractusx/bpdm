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

import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.client.service.PoolClientSiteInterface
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springframework.web.bind.annotation.RestController

@RestController
class SiteController(
    private val siteService: SiteService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val addressService: AddressService
) : PoolClientSiteInterface {

    override fun searchMainAddresses(
        bpnS: Collection<String>
    ): Collection<MainAddressSearchResponse> {
        return addressService.findMainAddresses(bpnS)
    }

    override fun getSite(
        bpn: String
    ): SitePartnerSearchResponse {
        return siteService.findByBpn(bpn.uppercase())
    }

    override fun searchSites(
        siteSearchRequest: SiteBpnSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SitePartnerSearchResponse> {
        return siteService.findByPartnerBpns(siteSearchRequest, paginationRequest)
    }

    override fun createSite(
        requests: Collection<SitePartnerCreateRequest>
    ): Collection<SitePartnerCreateResponse> {
        return businessPartnerBuildService.createSites(requests)
    }

    override fun updateSite(
        requests: Collection<SitePartnerUpdateRequest>
    ): Collection<SitePartnerCreateResponse> {
        return businessPartnerBuildService.updateSites(requests)
    }
}