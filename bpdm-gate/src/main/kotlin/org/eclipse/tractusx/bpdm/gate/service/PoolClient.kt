/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.tractusx.bpdm.common.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.gate.exception.PoolRequestException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PoolClient(
    @Qualifier("poolClient")
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper
) {
    fun searchLegalEntities(bpnLs: Collection<String>): Collection<LegalEntityPartnerResponse> {
        val legalEntities = try {
            webClient
                .post()
                .uri("/legal-entities/search")
                .bodyValue(objectMapper.writeValueAsString(bpnLs))
                .retrieve()
                .bodyToMono<Collection<LegalEntityPartnerResponse>>()
                .block()!!
        } catch (e: Exception) {
            throw PoolRequestException("Request to search legal entities failed.", e)
        }
        return legalEntities
    }

    fun searchLegalAddresses(bpnLs: Collection<String>): Collection<LegalAddressSearchResponse> {
        val legalAddresses = try {
            webClient
                .post()
                .uri("/legal-entities/legal-addresses/search")
                .bodyValue(objectMapper.writeValueAsString(bpnLs))
                .retrieve()
                .bodyToMono<Collection<LegalAddressSearchResponse>>()
                .block()!!
        } catch (e: Exception) {
            throw PoolRequestException("Request to search legal addresses failed.", e)
        }
        return legalAddresses
    }

    fun searchSites(bpnSs: Collection<String>): Collection<SitePartnerSearchResponse> {
        val sites = try {
            webClient
                .post()
                .uri("/sites/search")
                .bodyValue(objectMapper.writeValueAsString(SiteSearchRequest(sites = bpnSs)))
                .retrieve()
                .bodyToMono<Collection<SitePartnerSearchResponse>>()
                .block()!!
        } catch (e: Exception) {
            throw PoolRequestException("Request to search sites failed.", e)
        }
        return sites
    }

    fun searchMainAddresses(bpnSs: Collection<String>): Collection<MainAddressSearchResponse> {
        val mainAddresses = try {
            webClient
                .post()
                .uri("/sites/main-addresses/search")
                .bodyValue(objectMapper.writeValueAsString(bpnSs))
                .retrieve()
                .bodyToMono<Collection<MainAddressSearchResponse>>()
                .block()!!
        } catch (e: Exception) {
            throw PoolRequestException("Request to main addresses of sites failed.", e)
        }
        return mainAddresses
    }
}