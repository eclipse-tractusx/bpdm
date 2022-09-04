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
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPartnerResponse
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
class LegalEntityControllerIT @Autowired constructor(
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
     * Given no legal entities
     * When creating new legal entity
     * Then new legal entity is created with first BPN
     */
    @Test
    fun `create new legal entity`() {
        val expectedBpn = CommonValues.bpnL1
        val expected = ResponseValues.legalEntityUpsert1.copy(bpn = expectedBpn)

        val toCreate = RequestValues.legalEntityCreate1
        val response = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, listOf(toCreate))

        assertThat(response.size).isEqualTo(1)
        assertThat(response.single())
            .usingRecursiveComparison()
            .ignoringFields(LegalEntityPartnerCreateResponse::currentness.name)
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given no legal entities
     * When creating new legal entities
     * Then new legal entities created
     */
    @Test
    fun `create new legal entities`() {
        val expected = listOf(ResponseValues.legalEntityUpsert1, ResponseValues.legalEntityUpsert2, ResponseValues.legalEntityUpsert3)

        val toCreate = listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2, RequestValues.legalEntityCreate3)
        val response = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, toCreate)

        testHelpers.assertThatCreatedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entity
     * When creating legal entities
     * Then only create new legal entities with different identifiers
     */
    @Test
    fun `don't create legal entity with same identifier`() {
        val given = with(RequestValues.legalEntityCreate1) { copy(properties = properties.copy(identifiers = listOf(RequestValues.identifier1))) }
        webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, listOf(given))

        val expected = listOf(ResponseValues.legalEntityUpsert2, ResponseValues.legalEntityUpsert3)

        val toCreate = listOf(given, RequestValues.legalEntityCreate2, RequestValues.legalEntityCreate3)
        val response = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, toCreate)

        testHelpers.assertThatCreatedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entity
     * When updating values of legal entity via BPN
     * Then legal entity updated with the values
     */
    @Test
    fun `update existing legal entities`() {
        val given = listOf(RequestValues.legalEntityCreate1)
        val givenBpn = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, given)
            .single().bpn

        val expected = listOf(ResponseValues.legalEntityUpsert3.copy(bpn = givenBpn))

        val toUpdate = listOf(RequestValues.legalEntityUpdate3.copy(bpn = givenBpn))
        val response = webTestClient.invokePutWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, toUpdate)

        testHelpers.assertThatModifiedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entities
     * When trying to update via non-existent BPN
     * Then don't update
     */
    @Test
    fun `ignore invalid legal entity update`() {
        val given = listOf(RequestValues.legalEntityCreate1)
        webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, given)

        val toUpdate = listOf(RequestValues.legalEntityUpdate3.copy(bpn = "NONEXISTENT"))
        val response = webTestClient.invokePutWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, toUpdate)

        assertThat(response).isEmpty()
    }

    /**
     * Given legal addresses of legal entities
     * When asking for legal addresses via BPNs of those legal entities
     * Then get those legal addresses
     */
    @Test
    fun `find legal addresses by BPN`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate3)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures, webTestClient).map { it.legalEntity }

        val expected = givenLegalEntities.map { LegalAddressSearchResponse(it.bpn, it.legalAddress) }

        val bpnsToSearch = givenLegalEntities.map { it.bpn }
        val response = webTestClient.invokePostWithArrayResponse<LegalAddressSearchResponse>(EndpointValues.CATENA_LEGAL_ADDRESS_SEARCH_PATH, bpnsToSearch)

        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When asking for legal address with wrong and correct BPNLs
     * Then only get legal addresses with correct BPNLs
     */
    @Test
    fun `only find legal addresses with matching BPNLs`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate3)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures, webTestClient).map { it.legalEntity }

        val expected = givenLegalEntities.map { LegalAddressSearchResponse(it.bpn, it.legalAddress) }.take(2)

        val bpnsToSearch = expected.map { it.legalEntity }.plus("NONEXISTENT")
        val response = webTestClient.invokePostWithArrayResponse<LegalAddressSearchResponse>(EndpointValues.CATENA_LEGAL_ADDRESS_SEARCH_PATH, bpnsToSearch)

        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given business partner
     * When updating currentness of an imported business partner
     * Then currentness timestamp is updated
     */
    @Test
    fun `set business partner currentness`() {
        val given = listOf(RequestValues.legalEntityCreate1)
        val bpnL = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, given).single().bpn
        val initialCurrentness = retrieveCurrentness(bpnL)
        val instantBeforeCurrentnessUpdate = Instant.now()

        assertThat(initialCurrentness).isBeforeOrEqualTo(instantBeforeCurrentnessUpdate)

        webTestClient.invokePostEndpointWithoutResponse(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpnL}" + EndpointValues.CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX)

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
        webTestClient.post().uri(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/NONEXISTENT_BPN" + EndpointValues.CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    private fun retrieveCurrentness(bpn: String) = webTestClient
        .get()
        .uri(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpn}")
        .exchange().expectStatus().isOk
        .returnResult<LegalEntityPartnerResponse>()
        .responseBody
        .blockFirst()!!.currentness

}