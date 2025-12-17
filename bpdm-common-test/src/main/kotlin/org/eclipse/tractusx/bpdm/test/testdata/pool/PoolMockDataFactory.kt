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

import com.github.tomakehurst.wiremock.client.WireMock
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.PoolMockContextInitializer
import tools.jackson.databind.json.JsonMapper

class PoolMockDataFactory(
    private val requestFactory: BusinessPartnerRequestFactory,
    private val expectedResultFactory: ExpectedBusinessPartnerResultFactory,
    private val jsonMapper: JsonMapper
) {

    fun mockLegalEntityAndLegalAddressSearchResult(seed: String): LegalEntityWithLegalAddressVerboseDto{
        configureWireMock()

        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, givenBpnL = "BPNL$seed")

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockAddressSearchResult(mockedLegalEntity.legalAddress)

        return mockedLegalEntity
    }

    fun mockSiteAndMainAddressSearchResult(seed: String): SiteWithLegalEntityParent{
        configureWireMock()

        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, givenBpnL = "BPNL$seed")

        val siteRequest = requestFactory.buildSiteCreateRequest(seed, mockedLegalEntity.legalEntity.bpnl)
        val mockedSite = expectedResultFactory.mapToExpectedSite(siteRequest, mockedLegalEntity.legalEntity.isParticipantData, givenBpnS = "BPNS$seed")

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(mockedSite)
        mockAddressSearchResult(mockedSite.mainAddress)

        return SiteWithLegalEntityParent(mockedLegalEntity, mockedSite)
    }

    fun mockAdditionalAddressOfSiteSearchResult(seed: String): AdditionalAddressOfSiteResult{
        configureWireMock()
        WireMock.reset()

        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val siteRequest = requestFactory.buildSiteCreateRequest(seed, "BPNL$seed")
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, "BPNS$seed")

        return mockAdditionalAddressOfSiteSearchResult(legalEntityRequest, siteRequest, addressRequest, seed)
    }

    fun mockAdditionalAddressOfSiteSearchResult(
        legalEntityRequest: LegalEntityPartnerCreateRequest,
        siteRequest: SitePartnerCreateRequest,
        additionalAddressRequest: AddressPartnerCreateRequest,
        seed: String
    ): AdditionalAddressOfSiteResult{
        configureWireMock()
        WireMock.reset()

        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, siteRequest.bpnlParent, isMaintainConfidences = true)
        val mockedSite = expectedResultFactory.mapToExpectedSite(siteRequest, mockedLegalEntity.legalEntity.isParticipantData, additionalAddressRequest.bpnParent, isMaintainConfidences = true)
        val mockedAddress = expectedResultFactory.mapToExpectedAdditionalAddress(additionalAddressRequest, mockedLegalEntity.legalEntity.isParticipantData, givenBpnA = "BPNA$seed", isMaintainConfidences = true)

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(mockedSite)
        mockAddressSearchResult(mockedAddress)

        return AdditionalAddressOfSiteResult(mockedLegalEntity, mockedSite, mockedAddress)
    }



    private fun mockLegalEntitySearchResult(legalEntityResult: LegalEntityWithLegalAddressVerboseDto){

        val mockedLegalEntityPage = PageDto(1, 1, 0, 1, listOf(legalEntityResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.LEGAL_ENTITY_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedLegalEntityPage)))
        )
    }

    private fun mockSiteSearchResult(siteResult: SiteWithMainAddressVerboseDto){

        val mockedSitePage = PageDto(1, 1, 0, 1, listOf(siteResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.SITE_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedSitePage)))
        )
    }

    private fun mockAddressSearchResult(addressResult: LogisticAddressVerboseDto){

        val mockedAddressPage = PageDto(1, 1, 0, 1, listOf(addressResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.ADDRESS_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedAddressPage)))
        )
    }

    data class SiteWithLegalEntityParent(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseDto,
        val site: SiteWithMainAddressVerboseDto
    )

    data class AdditionalAddressOfSiteResult(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseDto,
        val siteParent: SiteWithMainAddressVerboseDto,
        val additionalAddress: LogisticAddressVerboseDto
    )

    private fun configureWireMock(){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())
        WireMock.reset()
    }
}