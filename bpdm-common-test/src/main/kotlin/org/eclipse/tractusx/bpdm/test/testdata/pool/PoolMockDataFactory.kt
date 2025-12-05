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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.PoolMockContextInitializer

class PoolMockDataFactory(
    private val requestFactory: BusinessPartnerRequestFactory,
    private val expectedResultFactory: ExpectedBusinessPartnerResultFactory,
    private val objectMapper: ObjectMapper
) {

    fun mockAdditionalAddressOfSiteSearchResult(seed: String): AdditionalAddressOfSiteResult{
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())
        WireMock.reset()

        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, givenBpnL = "BPNL$seed")

        val siteRequest = requestFactory.buildSiteCreateRequest(seed, mockedLegalEntity.legalEntity.bpnl)
        val mockedSite = expectedResultFactory.mapToExpectedSite(siteRequest, mockedLegalEntity.legalEntity.isParticipantData, givenBpnS = "BPNS$seed")

        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, mockedSite.site.bpns)
        val mockedAddress = expectedResultFactory.mapToExpectedAdditionalAddress(addressRequest, mockedLegalEntity.legalEntity.isParticipantData, givenBpnA = "BPNA$seed")

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(mockedSite)
        mockAddressSearchResult(mockedAddress)

        return AdditionalAddressOfSiteResult(mockedLegalEntity, mockedSite, mockedAddress)
    }

    private fun mockLegalEntitySearchResult(legalEntityResult: LegalEntityWithLegalAddressVerboseDto){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())

        val mockedLegalEntityPage = PageDto(1, 1, 0, 1, listOf(legalEntityResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.LEGAL_ENTITY_BASE_PATH_V7))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(mockedLegalEntityPage)))
        )
    }

    private fun mockSiteSearchResult(siteResult: SiteWithMainAddressVerboseDto){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())

        val mockedSitePage = PageDto(1, 1, 0, 1, listOf(siteResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.SITE_BASE_PATH_V7))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(mockedSitePage)))
        )
    }

    private fun mockAddressSearchResult(addressResult: LogisticAddressVerboseDto){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())

        val mockedAddressPage = PageDto(1, 1, 0, 1, listOf(addressResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.ADDRESS_BASE_PATH_V7))
                .willReturn(WireMock.okJson(objectMapper.writeValueAsString(mockedAddressPage)))
        )
    }

    data class AdditionalAddressOfSiteResult(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseDto,
        val siteParent: SiteWithMainAddressVerboseDto,
        val additionalAddress: LogisticAddressVerboseDto
    )
}