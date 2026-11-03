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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.containers.PoolMockContextInitializer
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class PoolMockDataFactory(
    val requestFactory: BusinessPartnerRequestFactory,
    val expectedResultFactory: ExpectedBusinessPartnerResultFactory,
    private val jsonMapper: JsonMapper
) {

    fun mockLegalEntityAndLegalAddressSearchResult(seed: String): LegalEntityWithLegalAddressVerboseV7Dto{
        configureWireMock()

        // Advancing timestamp per refinement so a re-share carries a newer golden-record updatedAt than the
        // stored one - otherwise the Gate's shouldWriteOutputByGoldenRecordTimestamp gate skips the update.
        val now = Instant.now()
        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, givenBpnL = "BPNL$seed", currentness = now)

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockAddressSearchResult(LogisticAddressVerboseV7Dto(mockedLegalEntity.legalAddress, emptyList()))

        return mockedLegalEntity
    }

    fun mockSiteAndMainAddressSearchResult(seed: String): SiteWithLegalEntityParent{
        configureWireMock()

        val now = Instant.now()
        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, givenBpnL = "BPNL$seed", currentness = now)

        val siteRequest = requestFactory.buildSiteCreateRequest(seed, mockedLegalEntity.header.bpnl)
        val mockedSite = expectedResultFactory.mapToExpectedSite(siteRequest, mockedLegalEntity.header.isParticipantData, givenBpnS = "BPNS$seed", siteCreatedAt = now)

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(mockedSite)
        mockAddressSearchResult(LogisticAddressVerboseV7Dto(mockedSite.mainAddress, emptyList()))

        return SiteWithLegalEntityParent(mockedLegalEntity, mockedSite)
    }

    fun mockLegalAndSiteMainAddressSearchResult(seed: String): SiteWithLegalEntityParent{
        configureWireMock()

        val now = Instant.now()
        val legalEntity = requestFactory.createLegalEntityRequest(seed)
            .let{ expectedResultFactory.mapToExpectedLegalEntity(it,  givenBpnL = "BPNL$seed", currentness = now) }

        val site = requestFactory.buildSiteCreateRequest(seed, "BPNL$seed")
            .let { expectedResultFactory.mapToExpectedSite(it, legalEntity.header.isParticipantData, givenBpnS = "BPNS$seed", siteCreatedAt = now) }

        val legalAndSiteMainAddress = requestFactory.buildAdditionalAddressCreateRequest(seed, bpnParent = "BPNS$seed")
            .let { expectedResultFactory.mapToExpectedAdditionalAddress(it, legalEntity.header.isParticipantData, createdAt = now) }
            .copy(addressType = AddressType.LegalAndSiteMainAddress)


        val mockedLegalEntityResponse = legalEntity.copy(legalAddress = legalAndSiteMainAddress)
        val mockedSiteResponse = site.copy(mainAddress = legalAndSiteMainAddress)

        mockLegalEntitySearchResult(mockedLegalEntityResponse)
        mockSiteSearchResult(mockedSiteResponse)
        mockAddressSearchResult(LogisticAddressVerboseV7Dto(mockedSiteResponse.mainAddress, emptyList()))

        return SiteWithLegalEntityParent(mockedLegalEntityResponse, mockedSiteResponse)
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
        legalEntityRequest: LegalEntityPartnerCreateV7Request,
        siteRequest: SitePartnerCreateV7Request,
        additionalAddressRequest: AddressPartnerCreateV7Request,
        seed: String
    ): AdditionalAddressOfSiteResult{
        configureWireMock()
        WireMock.reset()

        val now = Instant.now()
        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, siteRequest.bpnlParent, currentness = now, isMaintainConfidences = true)
        val mockedSite = expectedResultFactory.mapToExpectedSite(siteRequest, mockedLegalEntity.header.isParticipantData, additionalAddressRequest.bpnParent, siteCreatedAt = now, isMaintainConfidences = true)
        val mockedInvariantAddress = expectedResultFactory.mapToExpectedAdditionalAddress(additionalAddressRequest, mockedLegalEntity.header.isParticipantData, givenBpnA = "BPNA$seed", createdAt = now, isMaintainConfidences = true)
        val mockedAddress = LogisticAddressVerboseV7Dto(mockedInvariantAddress, additionalAddressRequest.scriptVariants)

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(mockedSite)
        mockAddressSearchResult(mockedAddress)

        return AdditionalAddressOfSiteResult(mockedLegalEntity, mockedSite, mockedAddress)
    }



    fun mockAdditionalAddressWithAdditionalSitesSearchResult(seed: String): AdditionalAddressWithAdditionalSitesResult {
        configureWireMock()
        WireMock.reset()

        val legalEntityRequest = requestFactory.createLegalEntityRequest(seed)
        val trackedSiteRequest = requestFactory.buildSiteCreateRequest(seed, "BPNL$seed")
        val otherSiteRequest = requestFactory.buildSiteCreateRequest("${seed}Other", "BPNL$seed")
        // The address's Pool `bpnSite` (its oldest member) is deliberately the OTHER site, not the one the output
        // follows. This exercises that "additional" is computed relative to the followed site (bpnS), not `bpnSite`:
        // the followed site "BPNS$seed" arrives via the address's additionalSites and must be excluded from the output.
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(seed, "BPNS${seed}Other")

        val mockedLegalEntity = expectedResultFactory.mapToExpectedLegalEntity(legalEntityRequest, trackedSiteRequest.bpnlParent, isMaintainConfidences = true)
        val trackedSite = expectedResultFactory.mapToExpectedSite(trackedSiteRequest, mockedLegalEntity.header.isParticipantData, "BPNS$seed", isMaintainConfidences = true)
        val otherSite = expectedResultFactory.mapToExpectedSite(otherSiteRequest, mockedLegalEntity.header.isParticipantData, "BPNS${seed}Other", isMaintainConfidences = true)

        val mockedInvariantAddress = expectedResultFactory
            .mapToExpectedAdditionalAddress(addressRequest, mockedLegalEntity.header.isParticipantData, givenBpnA = "BPNA$seed", isMaintainConfidences = true)
            .copy(additionalSites = listOf(trackedSite.site.bpns))
        val mockedAddress = LogisticAddressVerboseV7Dto(mockedInvariantAddress, addressRequest.scriptVariants)

        mockLegalEntitySearchResult(mockedLegalEntity)
        mockSiteSearchResult(listOf(trackedSite, otherSite))
        mockAddressSearchResult(mockedAddress)

        return AdditionalAddressWithAdditionalSitesResult(mockedLegalEntity, trackedSite, mockedAddress, listOf(otherSite))
    }

    fun mockLegalEntitySearchResult(legalEntityResult: LegalEntityWithLegalAddressVerboseV7Dto){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())

        val mockedLegalEntityPage = PageDto(1, 1, 0, 1, listOf(legalEntityResult))
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.LEGAL_ENTITY_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedLegalEntityPage)))
        )
    }

    fun mockNoLegalEntitySearchResult(){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())

        val emptyLegalEntityPage = PageDto<LegalEntityWithLegalAddressVerboseV7Dto>(0, 0, 0, 0, emptyList())
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.LEGAL_ENTITY_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(emptyLegalEntityPage)))
        )
    }

    fun mockNoGoldenRecordSearchResults(){
        mockNoLegalEntitySearchResult()
        mockSiteSearchResult(emptyList())
        mockAddressSearchResult(emptyList())
    }

    private fun mockSiteSearchResult(siteResult: SiteWithMainAddressVerboseV7Dto) = mockSiteSearchResult(listOf(siteResult))

    private fun mockSiteSearchResult(siteResults: List<SiteWithMainAddressVerboseV7Dto>){

        val mockedSitePage = PageDto(siteResults.size.toLong(), 1, 0, siteResults.size, siteResults)
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.SITE_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedSitePage)))
        )
    }

    private fun mockAddressSearchResult(addressResult: LogisticAddressVerboseV7Dto) = mockAddressSearchResult(listOf(addressResult))

    private fun mockAddressSearchResult(addressResults: List<LogisticAddressVerboseV7Dto>){

        val mockedAddressPage = PageDto(addressResults.size.toLong(), 1, 0, addressResults.size, addressResults)
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(ApiCommons.ADDRESS_BASE_PATH_V7))
                .willReturn(WireMock.okJson(jsonMapper.writeValueAsString(mockedAddressPage)))
        )
    }

    data class SiteWithLegalEntityParent(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto,
        val site: SiteWithMainAddressVerboseV7Dto
    )

    data class AdditionalAddressOfSiteResult(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto,
        val siteParent: SiteWithMainAddressVerboseV7Dto,
        val additionalAddress: LogisticAddressVerboseV7Dto
    )

    data class AdditionalAddressWithAdditionalSitesResult(
        val legalEntityParent: LegalEntityWithLegalAddressVerboseV7Dto,
        val siteParent: SiteWithMainAddressVerboseV7Dto,
        val additionalAddress: LogisticAddressVerboseV7Dto,
        val additionalSites: List<SiteWithMainAddressVerboseV7Dto>
    )

    fun configureWireMock(){
        WireMock.configureFor("localhost", PoolMockContextInitializer.wiremockServer.port())
        WireMock.reset()
    }
}