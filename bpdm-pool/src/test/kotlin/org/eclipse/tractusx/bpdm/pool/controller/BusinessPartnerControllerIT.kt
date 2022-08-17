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
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPoolUpsertResponse
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
import org.springframework.test.web.reactive.server.returnResult
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BusinessPartnerControllerIT @Autowired constructor(
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

    lateinit var createdLegalEntity: LegalEntityPoolUpsertResponse

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata(webTestClient)
        val createdStructure = testHelpers.createBusinessPartnerStructure(
            listOf(LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1)),
            webTestClient
        )
        createdLegalEntity = createdStructure[0].legalEntity
    }

    /**
     * Given business partner imported
     * When updating currentness of an imported business partner
     * Then currentness timestamp is updated
     */
    @Test
    fun `set business partner currentness`() {
        val bpnL = createdLegalEntity.bpn
        val initialCurrentness = retrieveCurrentness(bpnL)
        val instantBeforeCurrentnessUpdate = Instant.now()

        assertThat(initialCurrentness).isBeforeOrEqualTo(instantBeforeCurrentnessUpdate)

        webTestClient.invokePostEndpointWithoutResponse(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpnL}" + EndpointValues.CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX)

        val updatedCurrentness = retrieveCurrentness(bpnL)
        assertThat(updatedCurrentness).isBetween(instantBeforeCurrentnessUpdate, Instant.now())
    }

    /**
     * Given business partners imported
     * When trying to update currentness using a nonexistent bpn
     * Then a "not found" response is sent
     */
    @Test
    fun `set business partner currentness using nonexistent bpn`() {
        webTestClient.post().uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/NONEXISTENT_BPN" + EndpointValues.CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    private fun retrieveCurrentness(bpn: String) = webTestClient
        .get()
        .uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}")
        .exchange().expectStatus().isOk
        .returnResult<BusinessPartnerResponse>()
        .responseBody
        .blockFirst()!!.currentness
}