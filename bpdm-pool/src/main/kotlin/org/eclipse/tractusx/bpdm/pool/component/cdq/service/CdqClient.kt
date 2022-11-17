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

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchBatchRecord
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchBatchRequest
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class CdqClient(
    private val webClient: WebClient,
    private val adapterProperties: CdqAdapterConfigProperties,
) {

    fun readBusinessPartners(modifiedAfter: Instant, startAfter: String?): PagedResponseCdq<BusinessPartnerCdq> {
        return webClient
            .get()
            .uri { builder ->
                builder
                    .path(adapterProperties.readBusinessPartnerUrl)
                    .queryParam("modifiedAfter", toModifiedAfterFormat(modifiedAfter))
                    .queryParam("limit", adapterProperties.importLimit)
                    .queryParam("datasource", adapterProperties.datasource)
                    .queryParam("featuresOn", "USE_NEXT_START_AFTER", "FETCH_RELATIONS")
                if (startAfter != null) builder.queryParam("startAfter", startAfter)
                builder.build()
            }
            .retrieve()
            .bodyToMono<PagedResponseCdq<BusinessPartnerCdq>>()
            .block()!!
    }

    fun fetchBusinessPartnersInBatch(idValues: Collection<String>): Collection<FetchBatchRecord> {
        if (idValues.isEmpty()) return emptyList()

        return webClient
            .post()
            .uri(adapterProperties.fetchBusinessPartnersBatchUrl)
            .body(BodyInserters.fromValue(FetchBatchRequest(idValues)))
            .retrieve()
            .bodyToMono<Collection<FetchBatchRecord>>()
            .block()!!
    }

    private fun toModifiedAfterFormat(dateTime: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }

}