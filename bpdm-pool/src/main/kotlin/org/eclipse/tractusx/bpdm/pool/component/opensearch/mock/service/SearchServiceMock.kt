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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.mock.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.BusinessPartnerMatchResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.toBusinessPartnerMatchDto
import org.eclipse.tractusx.bpdm.pool.service.toDto
import org.eclipse.tractusx.bpdm.pool.service.toMatchDto
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Mock implementation of the [SearchService] in case the system is configured to run without OpenSearch
 */
@Service
class SearchServiceMock(
    val legalEntityRepository: LegalEntityRepository,
    val logisticAddressRepository: LogisticAddressRepository
) : SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Ignores [searchRequest] and returns an unfiltered result of legal entities in the database,
     * adding a default relevancy score to each entry
     */
    override fun searchLegalEntities(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<LegalEntityMatchResponse> {
        val resultPage =
            legalEntityRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        logger.info { "Mock search: Returning ${resultPage.size} business partners from database" }

        return resultPage.toDto(resultPage.content.map { it.toMatchDto(1f) })
    }

    /**
     * Ignores [searchRequest] and returns an unfiltered result of addresses in the database,
     * adding a default relevancy score to each entry
     */
    override fun searchAddresses(searchRequest: AddressPartnerSearchRequest, paginationRequest: PaginationRequest): PageResponse<AddressMatchResponse> {
        val resultPage = logisticAddressRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        logger.info { "Mock search: Returning ${resultPage.size} addresses from database" }

        return resultPage.toDto(resultPage.content.map { it.toMatchDto(1f) })
    }

    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerMatchResponse> {
        val resultPage =
            legalEntityRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))

        logger.info { "Mock search: Returning ${resultPage.size} business partners from database" }

        return resultPage.toDto(resultPage.content.map { it.toBusinessPartnerMatchDto(1f) })
    }

}