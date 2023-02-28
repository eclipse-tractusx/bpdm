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
import org.eclipse.tractusx.bpdm.pool.client.config.SpringWebClientConfig
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.client.exception.PoolRequestException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.lang.Thread.sleep


class PoolClientAddressService(webClient: WebClient) {


    private val springWebClientConfig = SpringWebClientConfig(webClient)
    private val client = springWebClientConfig.httpServiceProxyFactory.createClient(PoolClientAddressInterface::class.java)

    fun getAddresses(
        addressSearchRequest: AddressPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<AddressMatchResponse> {
        return client.getAddresses(addressSearchRequest, paginationRequest);
    }

    fun getAddress(
        requests: Collection<AddressPartnerCreateRequest>
    ): Collection<AddressPartnerCreateResponse> {
        return client.createAddresses(requests);
    }

    fun getAddress(
        bpn: String
    ): AddressPartnerSearchResponse {

        try {
            println("valid" )
            sleep(5000)
            return client.getAddress(bpn)
        } catch (e: WebClientResponseException) {
            throw e
        }
    }

    fun updateAddresses(
        requests: Collection<AddressPartnerUpdateRequest>
    ): Collection<AddressPartnerResponse> {
        return client.updateAddresses(requests);
    }

    fun searchAddresses(
        addressSearchRequest: AddressPartnerBpnSearchRequest,
        pageRequest: PaginationRequest
    ): PageResponse<AddressPartnerSearchResponse> {
        return client.searchAddresses(addressSearchRequest, pageRequest);
    }

    private fun <T : Any> validateResult(method: T): T {

        try {
            sleep(5000)
            return method;
        } /*catch (e: WebClientResponseException) {

        } */catch (e: Exception) {
            println("error " + e.message + " " + e.cause)
            throw PoolRequestException("Error", e)
        }

    }
}