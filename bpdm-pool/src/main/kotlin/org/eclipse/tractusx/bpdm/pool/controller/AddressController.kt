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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolAddressV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7AddressMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressGetResult
import org.eclipse.tractusx.bpdm.pool.service.application.AddressCreateApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.AddressGetApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.AddressSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressUpdateApplicationV7Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressGetApplicationService: AddressGetApplicationService,
    private val addressSearchApplicationService: AddressSearchApplicationService,
    private val addressCreateApplicationService: AddressCreateApplicationService,
    private val addressUpdateApplicationService: AddressUpdateApplicationV7Service,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7AddressMapper: ApiV7AddressMapper
) : PoolAddressV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(addressSearchRequest: AddressSearchV7Request, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseV7Dto> {
        return searchAddresses(addressSearchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddress(
        bpna: String
    ): LogisticAddressVerboseV7Dto =
        when (val result = addressGetApplicationService.get(apiV7InboundMapper.toAddressGetRaw(bpna))) {
            is AddressGetResult.Found -> apiV7AddressMapper.toDto(result.address)
            is AddressGetResult.Rejected -> throw apiV7AddressMapper.toGetException(result.errors)
        }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun searchAddresses(
        searchRequest: AddressSearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto> {
        val request = apiV7InboundMapper.toAddressSearchRaw(searchRequest)
        return addressSearchApplicationService.search(request, paginationRequest.toPageRequest())
            .toPageDto(apiV7AddressMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createAddresses(
        requests: Collection<AddressPartnerCreateV7Request>
    ): AddressPartnerCreateV7ResponseWrapper {
        val requestList = requests.toList()
        val results = addressCreateApplicationService.create(requestList.map { apiV7InboundMapper.toAddressCreateRaw(it) })
        return apiV7AddressMapper.toCreateResponse(requestList, results)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateAddresses(
        requests: Collection<AddressPartnerUpdateV7Request>
    ): AddressPartnerUpdateV7ResponseWrapper {
        return addressUpdateApplicationService.updateAddresses(requests)
    }
}
