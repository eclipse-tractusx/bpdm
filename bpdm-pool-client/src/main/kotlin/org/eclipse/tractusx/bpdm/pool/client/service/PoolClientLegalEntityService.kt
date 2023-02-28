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

import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.pool.client.config.SpringWebClientConfig
import org.eclipse.tractusx.bpdm.pool.client.dto.request.*
import org.eclipse.tractusx.bpdm.pool.client.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.client.exception.PoolRequestException
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient


class PoolClientLegalEntityService(webClient: WebClient) {

    private val springWebClientConfig = SpringWebClientConfig(webClient)
    private val client = springWebClientConfig.httpServiceProxyFactory.createClient(PoolClientLegalEntityInterface::class.java)

    fun getLegalEntity(
        bpSearchRequest: LegalEntityPropertiesSearchRequest,
        addressSearchRequest: AddressPropertiesSearchRequest,
        siteSearchRequest: SitePropertiesSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {
        return client.getLegalEntities(bpSearchRequest, addressSearchRequest, siteSearchRequest, paginationRequest)
    }

    fun getLegalEntity(idValue: String, idType: String?): LegalEntityPartnerResponse {
        return client.getLegalEntity(idValue, idType)
    }

    fun setLegalEntityCurrentness(bpn: String) {
        return client.setLegalEntityCurrentness(bpn)
    }

    fun searchSites(bpnLs: Collection<String>): ResponseEntity<Collection<LegalEntityPartnerResponse>> {
        return client.searchSites(bpnLs)
    }

    fun getSites(bpn: String, paginationRequest: PaginationRequest): PageResponse<SitePartnerResponse> {
        return client.getSites(bpn, paginationRequest)
    }

    fun getAddresses(bpn: String, paginationRequest: PaginationRequest): PageResponse<AddressPartnerResponse> {
        return client.getAddresses(bpn, paginationRequest)
    }

    fun searchLegalAddresses(bpnLs: Collection<String>): Collection<LegalAddressSearchResponse> {
        return client.searchLegalAddresses(bpnLs)
    }

    fun createBusinessPartners(businessPartners: Collection<LegalEntityPartnerCreateRequest>): Collection<LegalEntityPartnerCreateResponse> {
        return validateResult(client.createBusinessPartners(businessPartners));
    }

    fun updateBusinessPartners(businessPartners: Collection<LegalEntityPartnerUpdateRequest>): Collection<LegalEntityPartnerCreateResponse> {
        return validateResult(client.updateBusinessPartners(businessPartners));
    }

    private fun <T : Any> validateResult(method:T): T  {

        try {
            return method;
        } catch (e: Exception) {
            println("error " + e.message + " " + e.cause)
            throw PoolRequestException("Error", e)
        }
    }


}