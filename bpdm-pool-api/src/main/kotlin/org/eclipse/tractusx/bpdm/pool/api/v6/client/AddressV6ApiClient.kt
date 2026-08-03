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

package org.eclipse.tractusx.bpdm.pool.api.v6.client

import io.swagger.v3.oas.annotations.Parameter
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolAddressV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressPartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapperV6
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface AddressV6ApiClient: PoolAddressV6Api {

    @PostExchange(value = ApiCommons.ADDRESS_BASE_PATH_V6)
    override fun createAddresses(
        @RequestBody requests: Collection<AddressPartnerCreateRequestV6>
    ): AddressPartnerCreateResponseWrapperV6

    @PutExchange(value = ApiCommons.ADDRESS_BASE_PATH_V6)
    override fun updateAddresses(
        @RequestBody requests: Collection<AddressPartnerUpdateRequestV6>
    ): AddressPartnerUpdateResponseWrapperV6

    @GetExchange(value = ApiCommons.ADDRESS_BASE_PATH_V6)
    override fun getAddresses(
        @ParameterObject addressSearchRequest: AddressSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDtoV6>

    @PostExchange(value = "${ApiCommons.ADDRESS_BASE_PATH_V6}${CommonApiPathNames.SUBPATH_SEARCH}")
    override fun searchAddresses(
        @RequestBody searchRequest: AddressSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDtoV6>

    @GetExchange(value = "${ApiCommons.ADDRESS_BASE_PATH_V6}/{bpna}")
    override fun getAddress(
        @Parameter(description = "BPNA value") @PathVariable bpna: String
    ): LogisticAddressVerboseDtoV6
}