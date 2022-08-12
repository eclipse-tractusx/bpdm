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
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.CdqRequestException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant

private const val BUSINESS_PARTNER_PATH = "/businesspartners"
private const val FETCH_BUSINESS_PARTNER_PATH = "$BUSINESS_PARTNER_PATH/fetch"

@Service
class CdqClient(
    private val webClient: WebClient,
    private val cdqConfigProperties: CdqConfigProperties,
    private val objectMapper: ObjectMapper
) {
    fun getAugmentedBusinessPartners(
        limit: Int,
        startAfter: String?,
        from: Instant?
    ): PagedResponseCdq<AugmentedBusinessPartnerResponseCdq> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(cdqConfigProperties.dataClinicApiUrl + "/augmentedbusinesspartners")
                        .queryParam("limit", limit)
                        .queryParam("datasource", cdqConfigProperties.datasource)
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    if (from != null) builder.queryParam("from", from)
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseCdq<AugmentedBusinessPartnerResponseCdq>>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Read augmented business partners request failed.", e)
        }
        return partnerCollection
    }

    fun getAugmentedBusinessPartner(externalId: String): AugmentedBusinessPartnerResponseCdq {
        val fetchRequest = ReadAugmentedBusinessPartnerRequestCdq(externalId)

        val response = try {
            webClient
                .post()
                .uri(cdqConfigProperties.dataClinicApiUrl + "/datasources/${cdqConfigProperties.datasource}/augmentedbusinesspartners/fetch")
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<AugmentedBusinessPartnerResponseCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Read augmented business partner request failed.", e)
        }
        return response
    }

    fun upsertBusinessPartners(legalEntitiesCdq: List<BusinessPartnerCdq>) {
        val upsertRequest =
            UpsertRequest(
                cdqConfigProperties.datasource,
                legalEntitiesCdq,
                listOf(UpsertRequest.CdqFeatures.UPSERT_BY_EXTERNAL_ID, UpsertRequest.CdqFeatures.API_ERROR_ON_FAILURES)
            )

        try {
            webClient
                .put()
                .uri(cdqConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert business partners request failed.", e)
        }
    }

    fun getBusinessPartner(externalId: String): FetchResponse {
        val fetchRequest = FetchRequest(cdqConfigProperties.datasource, externalId)

        val fetchResponse = try {
            webClient
                .post()
                .uri(cdqConfigProperties.dataExchangeApiUrl + FETCH_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<FetchResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Fetch business partners request failed.", e)
        }
        return fetchResponse
    }

    fun getBusinessPartners(
        limit: Int,
        startAfter: String?
    ): PagedResponseCdq<BusinessPartnerCdq> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(cdqConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                        .queryParam("limit", limit)
                        .queryParam("datasource", cdqConfigProperties.datasource)
                        .queryParam("featuresOn", "USE_NEXT_START_AFTER")
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseCdq<BusinessPartnerCdq>>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Get business partners request failed.", e)
        }
        return partnerCollection
    }
}