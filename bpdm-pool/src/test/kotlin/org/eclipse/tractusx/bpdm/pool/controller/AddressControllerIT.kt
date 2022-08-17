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

package org.eclipse.tractusx.bpdm.pool.controller

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.response.AddressBpnResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient,
) {
    companion object {
        @RegisterExtension
        val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata(webTestClient)
    }

    /**
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1)
                )
            ), webTestClient
        )

        val importedPartner = createdStructures.single().legalEntity
        importedPartner.bpn
            .let { bpn -> requestAddressesOfLegalEntity(bpn).content.single().bpn }
            .let { bpnAddress -> requestAddress(bpnAddress) }
            .let { addressResponse ->
                assertThat(addressResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1)
                )
            ), webTestClient
        )

        webTestClient.get()
            .uri(EndpointValues.CATENA_ADDRESSES_PATH + "/NONEXISTENT_BPN")
            .exchange().expectStatus().isNotFound
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNL
     * Then return addresses belonging to those legal entities
     */
    @Test
    fun `search addresses by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            ),
            webTestClient
        )

        val bpnL1 = createdStructures[0].legalEntity.bpn
        val bpnL2 = createdStructures[1].legalEntity.bpn

        val searchRequest = AddressSearchRequest(listOf(bpnL1, bpnL2), emptyList())
        val searchResult =
            webTestClient.invokePostEndpoint<PageResponse<AddressWithReferenceResponse>>(EndpointValues.CATENA_ADDRESSES_SEARCH_PATH, searchRequest)

        val expectedAddress1 = ResponseValues.addressPartner1
        val expectedAddress2 = ResponseValues.addressPartner2
        val expectedAddress3 = ResponseValues.addressPartner3

        val expectedAddressWithReferences1 = AddressWithReferenceResponse(expectedAddress1, bpnL1, null)
        val expectedAddressWithReferences2 = AddressWithReferenceResponse(expectedAddress2, bpnL1, null)
        val expectedAddressWithReferences3 = AddressWithReferenceResponse(expectedAddress3, bpnL2, null)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*${AddressBpnResponse::bpn.name}")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedAddressWithReferences1, expectedAddressWithReferences2, expectedAddressWithReferences3))
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNS
     * Then return addresses belonging to those sites
     */
    @Test
    fun `search addresses by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate2,
                            addresses = listOf(RequestValues.addressPartnerCreate3)
                        )
                    )
                )
            ),
            webTestClient
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.bpn
        val bpnS2 = createdStructures[1].siteStructures[0].site.bpn

        val searchRequest = AddressSearchRequest(emptyList(), listOf(bpnS1, bpnS2))
        val searchResult =
            webTestClient.invokePostEndpoint<PageResponse<AddressWithReferenceResponse>>(EndpointValues.CATENA_ADDRESSES_SEARCH_PATH, searchRequest)

        val expectedAddressWithReferences1 = AddressWithReferenceResponse(ResponseValues.addressPartner1, null, bpnS1)
        val expectedAddressWithReferences2 = AddressWithReferenceResponse(ResponseValues.addressPartner2, null, bpnS1)
        val expectedAddressWithReferences3 = AddressWithReferenceResponse(ResponseValues.addressPartner3, null, bpnS2)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*${AddressBpnResponse::bpn.name}")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedAddressWithReferences1, expectedAddressWithReferences2, expectedAddressWithReferences3))
    }


    private fun requestAddress(bpnAddress: String) =
        webTestClient.invokeGetEndpoint<AddressWithReferenceResponse>(EndpointValues.CATENA_ADDRESSES_PATH + "/${bpnAddress}")

    private fun requestAddressesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<AddressBpnResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX)
}