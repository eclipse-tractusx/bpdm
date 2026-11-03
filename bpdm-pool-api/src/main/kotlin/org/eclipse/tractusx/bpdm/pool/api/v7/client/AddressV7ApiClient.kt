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

package org.eclipse.tractusx.bpdm.pool.api.v7.client

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolAddressV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange
interface AddressV7ApiClient: PoolAddressV7Api {

    @GetExchange(value = ApiCommons.ADDRESS_BASE_PATH_V7)
    override fun getAddresses(
        @ParameterObject addressSearchRequest: AddressSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto>

    @GetExchange(value = "${ApiCommons.ADDRESS_BASE_PATH_V7}/{bpna}")
    override fun getAddress(@PathVariable bpna: String): LogisticAddressVerboseV7Dto

    @PostExchange(value = "${ApiCommons.ADDRESS_BASE_PATH_V7}${CommonApiPathNames.SUBPATH_SEARCH}")
    override fun searchAddresses(
        @RequestBody searchRequest: AddressSearchV7Request,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseV7Dto>

    @PostExchange(value = ApiCommons.ADDRESS_BASE_PATH_V7)
    override fun createAddresses(@RequestBody requests: Collection<AddressPartnerCreateV7Request>): AddressPartnerCreateV7ResponseWrapper

    @PutExchange(value = ApiCommons.ADDRESS_BASE_PATH_V7)
    override fun updateAddresses(@RequestBody requests: Collection<AddressPartnerUpdateV7Request>): AddressPartnerUpdateV7ResponseWrapper
}