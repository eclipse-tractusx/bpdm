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
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolAddressV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressCreateApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressGetApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressUpdateApplicationV7Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressGetApplicationService: AddressGetApplicationV7Service,
    private val addressSearchApplicationService: AddressSearchApplicationV7Service,
    private val addressCreateApplicationService: AddressCreateApplicationV7Service,
    private val addressUpdateApplicationService: AddressUpdateApplicationV7Service
) : PoolAddressV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddresses(addressSearchRequest: AddressSearchV7Request, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseV7Dto> {
        return searchAddresses(addressSearchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getAddress(
        bpna: String
    ): LogisticAddressVerboseV7Dto {
        return addressGetApplicationService.getAddress(bpna)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun searchAddresses(
        searchRequest: AddressSearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto> {
        return addressSearchApplicationService.searchAddresses(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createAddresses(
        requests: Collection<AddressPartnerCreateV7Request>
    ): AddressPartnerCreateV7ResponseWrapper {
        return addressCreateApplicationService.createAddresses(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateAddresses(
        requests: Collection<AddressPartnerUpdateV7Request>
    ): AddressPartnerUpdateV7ResponseWrapper {
        return addressUpdateApplicationService.updateAddresses(requests)
    }
}
