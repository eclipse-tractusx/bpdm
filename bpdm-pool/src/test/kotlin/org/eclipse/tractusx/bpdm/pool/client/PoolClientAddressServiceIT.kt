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

package org.eclipse.tractusx.bpdm.pool.client

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.response.AddressBpnResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
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


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PoolClientAddressServiceIT  @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient,
){

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
                Assertions.assertThat(addressResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }
    private fun requestAddress(bpnAddress: String) =
        webTestClient.invokeGetEndpoint<AddressPartnerSearchResponse>(EndpointValues.CATENA_ADDRESSES_PATH + "/${bpnAddress}")

    private fun requestAddressesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<AddressBpnResponse>>(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX)
}