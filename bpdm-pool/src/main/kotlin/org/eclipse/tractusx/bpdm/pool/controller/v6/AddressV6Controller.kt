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
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolAddressV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6AddressMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressGetResult
import org.eclipse.tractusx.bpdm.pool.service.application.AddressCreateApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.AddressGetApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.AddressSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.AddressUpdateApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("AddressControllerLegacy")
class AddressV6Controller(
    private val addressGetApplicationService: AddressGetApplicationService,
    private val addressSearchApplicationService: AddressSearchApplicationService,
    private val addressCreateApplicationService: AddressCreateApplicationService,
    private val addressUpdateApplicationService: AddressUpdateApplicationService,
    private val apiV6InboundMapper: ApiV6InboundMapper,
    private val apiV6AddressMapper: ApiV6AddressMapper
) : PoolAddressV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(addressSearchRequest: AddressSearchV6Request, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseV6Dto> {
        return searchAddresses(addressSearchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddress(
        bpna: String
    ): LogisticAddressVerboseV6Dto =
        when (val result = addressGetApplicationService.get(apiV6InboundMapper.toAddressGetRaw(bpna))) {
            is AddressGetResult.Found -> apiV6AddressMapper.toDto(result.address)
            is AddressGetResult.Rejected -> throw apiV6AddressMapper.toGetException(result.errors)
        }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun searchAddresses(
        searchRequest: AddressSearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV6Dto> {
        val request = apiV6InboundMapper.toAddressSearchRaw(searchRequest)
        return addressSearchApplicationService.search(request, paginationRequest.toPageRequest())
            .toPageDto(apiV6AddressMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createAddresses(
        requests: Collection<AddressPartnerCreateV6Request>
    ): AddressPartnerCreateV6ResponseWrapper {
        val requestList = requests.toList()
        val results = addressCreateApplicationService.create(requestList.map { apiV6InboundMapper.toAddressCreateRaw(it) })
        return apiV6AddressMapper.toCreateResponse(requestList, results)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateAddresses(
        requests: Collection<AddressPartnerUpdateV6Request>
    ): AddressPartnerUpdateV6ResponseWrapper {
        val requestList = requests.toList()
        val results = addressUpdateApplicationService.update(requestList.map { apiV6InboundMapper.toAddressUpdateRaw(it) })
        return apiV6AddressMapper.toUpdateResponse(requestList, results)
    }
}
