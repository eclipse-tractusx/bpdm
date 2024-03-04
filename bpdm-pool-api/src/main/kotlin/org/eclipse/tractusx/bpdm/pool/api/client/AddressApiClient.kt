/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.api.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.PoolAddressApi
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerbose
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchVerboseResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateResponseWrapper
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange(PoolAddressApi.ADDRESS_PATH)
interface AddressApiClient: PoolAddressApi {

    @GetExchange
    override fun getAddresses(
        @ParameterObject addressSearchRequest: AddressPartnerSearchRequest,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<AddressMatchVerboseResponse>

    @GetExchange(PoolAddressApi.SUBPATH_BPNA)
    override fun getAddress(@PathVariable(PoolAddressApi.PATHVAR_BPNA) bpna: String): LogisticAddressVerbose

    @PostExchange(CommonApiPathNames.SUBPATH_SEARCH)
    override fun searchAddresses(
        @RequestBody addressSearchRequest: AddressPartnerBpnSearchRequest,
        @ParameterObject @Valid paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerbose>

    @PostExchange
    override fun createAddresses(@RequestBody requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper

    @PutExchange
    override fun updateAddresses(@RequestBody requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper
}