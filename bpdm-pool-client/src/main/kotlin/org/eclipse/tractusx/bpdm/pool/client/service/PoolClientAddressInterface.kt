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

package org.eclipse.tractusx.bpdm.pool.client.service

import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressPartnerCreateResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange


//@RequestMapping("/api/catena/addresses")
@HttpExchange("/api/catena/addresses")
interface PoolClientAddressInterface {

    @GetExchange
    fun getAddresses(
        @RequestParam addressSearchRequest: AddressPartnerSearchRequest,
         @RequestParam paginationRequest: PaginationRequest
    ): ResponseEntity<PageResponse<AddressMatchResponse>>

    @GetExchange("/2")
    fun getAddresses2(
        @ParameterObject paginationRequest: PaginationRequest
    ): ResponseEntity<String>




    @GetExchange("/{bpn}")
    fun getAddress(
        bpn: String
    ): ResponseEntity<AddressPartnerSearchResponse>

    @PostExchange("/search")
    fun searchAddresses(
         addressSearchRequest: AddressPartnerBpnSearchRequest,
         paginationRequest: PaginationRequest
    ): PageResponse<AddressPartnerSearchResponse>

    @PostExchange
    fun createAddresses(
        @RequestBody requests: List<AddressPartnerCreateRequest>
    ): Collection<AddressPartnerCreateResponse>

    @PutExchange
    fun updateAddresses(
        requests: Collection<AddressPartnerUpdateRequest>
    ): Collection<AddressPartnerResponse>


}