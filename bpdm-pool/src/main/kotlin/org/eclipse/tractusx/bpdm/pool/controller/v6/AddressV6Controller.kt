/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolAddressV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v6.AddressCreateApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.AddressUpdateApplicationV6Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("AddressControllerLegacy")
class AddressV6Controller(
    private val addressLegacyServiceMapper: AddressLegacyServiceMapper,
    private val addressCreateApplicationService: AddressCreateApplicationV6Service,
    private val addressUpdateApplicationService: AddressUpdateApplicationV6Service
) : PoolAddressV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(addressSearchRequest: AddressSearchRequestV6, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDtoV6> {
        return searchAddresses(addressSearchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddress(
        bpna: String
    ): LogisticAddressVerboseDtoV6 {
        return addressLegacyServiceMapper.findByBpn(bpna.uppercase())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun searchAddresses(
        searchRequest: AddressSearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDtoV6> {
        return addressLegacyServiceMapper.searchAddresses(
            AddressLegacyServiceMapper.AddressSearchRequest(
                addressBpns = searchRequest.addressBpns,
                siteBpns = searchRequest.siteBpns,
                legalEntityBpns = searchRequest.legalEntityBpns,
                name = searchRequest.name,
                isCatenaXMemberData = null
            ),
            paginationRequest
        )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createAddresses(
        requests: Collection<AddressPartnerCreateRequestV6>
    ): AddressPartnerCreateResponseWrapperV6 {
        return addressCreateApplicationService.createAddresses(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateAddresses(
        requests: Collection<AddressPartnerUpdateRequestV6>
    ): AddressPartnerUpdateResponseWrapperV6 {
        return addressUpdateApplicationService.updateAddresses(requests)
    }
}
