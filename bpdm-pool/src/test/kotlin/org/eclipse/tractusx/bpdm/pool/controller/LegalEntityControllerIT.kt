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

package org.eclipse.tractusx.bpdm.pool.controller

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.config.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.dto.response.LegalEntityPartnerCreateResponse
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
    val poolClient: PoolApiClient
) {
    companion object {
        @RegisterExtension
        val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }


    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
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
        val response = poolClient.legalEntities().createBusinessPartners(listOf(toCreate))

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
        val response = poolClient.legalEntities().createBusinessPartners(toCreate)
        assertThatCreatedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entity
     * When creating legal entities
     * Then only create new legal entities with different identifiers
     */
    @Test
    fun `don't create legal entity with same identifier`() {
        val given = with(RequestValues.legalEntityCreate1) { copy(properties = properties.copy(identifiers = listOf(RequestValues.identifier1))) }
        poolClient.legalEntities().createBusinessPartners(listOf(given))
        val expected = listOf(ResponseValues.legalEntityUpsert2, ResponseValues.legalEntityUpsert3)

        val toCreate = listOf(given, RequestValues.legalEntityCreate2, RequestValues.legalEntityCreate3)
        val response =poolClient.legalEntities().createBusinessPartners(toCreate)
        assertThatCreatedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entity
     * When updating values of legal entity via BPN
     * Then legal entity updated with the values
     */
    @Test
    fun `update existing legal entities`() {
        val given = listOf(RequestValues.legalEntityCreate1)

        val givenBpn = poolClient.legalEntities().createBusinessPartners(given).single().bpn

        val expected = listOf(ResponseValues.legalEntityUpsert3.copy(bpn = givenBpn))

        val toUpdate = listOf(RequestValues.legalEntityUpdate3.copy(bpn = givenBpn))
        val response = poolClient.legalEntities().updateBusinessPartners(toUpdate)
        assertThatModifiedLegalEntitiesEqual(response, expected)
    }

    /**
     * Given legal entities
     * When trying to update via non-existent BPN
     * Then don't update
     */
    @Test
    fun `ignore invalid legal entity update`() {
        val given = listOf(RequestValues.legalEntityCreate1)
        poolClient.legalEntities().createBusinessPartners(given)
        val toUpdate = listOf(RequestValues.legalEntityUpdate3.copy(bpn = "NONEXISTENT"))
        val response =         poolClient.legalEntities().updateBusinessPartners(toUpdate)
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
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map { LegalAddressSearchResponse(it.bpn, it.legalAddress) }

        val bpnsToSearch = givenLegalEntities.map { it.bpn }
        val response = poolClient.legalEntities().searchLegalAddresses(bpnsToSearch)
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
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map { LegalAddressSearchResponse(it.bpn, it.legalAddress) }.take(2)

        val bpnsToSearch = expected.map { it.legalEntity }.plus("NONEXISTENT")
        val response = poolClient.legalEntities().searchLegalAddresses(bpnsToSearch)
        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving those legal entities via BPNLs
     * Then get those legal entities
     */
    @Test
    fun `find legal entities by BPN`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate3)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.take(2) // only search for a subset of the existing legal entities

        val bpnsToSearch = expected.map { it.bpn }
        val response = poolClient.legalEntities().searchSites(bpnsToSearch).body
        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving a legal entity via identifier
     * Then the legal entity is returned
     */
    @Test
    fun `find legal entities by identifier`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.first() // search for first

        val identifierToFind = expected.properties.identifiers.first()


       val response = poolClient.legalEntities().getLegalEntity(identifierToFind.value,identifierToFind.type.technicalKey);

        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving a legal entity via identifier using a different case
     * Then the legal entity is returned
     */
    @Test
    fun `find legal entities by identifier case insensitive`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.first() // search for first

        var identifierToFind = expected.properties.identifiers.first()
        identifierToFind = identifierToFind.copy(value = changeCase(identifierToFind.value))


        val response = poolClient.legalEntities().getLegalEntity(identifierToFind.value,identifierToFind.type.technicalKey);

        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving a legal entity via bpn identifier
     * Then the legal entity is returned
     */
    @Test
    fun `find legal entities by bpn identifier`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.first() // search for first

        val bpnToFind = expected.bpn

        val response = poolClient.legalEntities().getLegalEntity(bpnToFind)

        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving a legal entity via bpn identifier using a different case
     * Then the legal entity is returned
     */
    @Test
    fun `find legal entities by bpn identifier case insensitive`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.first() // search for first

        val bpnToFind = changeCase(expected.bpn)
        val response = poolClient.legalEntities().getLegalEntity(bpnToFind)
        assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .isEqualTo(expected)
    }

    /**
     * Given legal entities
     * When retrieving legal entities via BPNLs where some BPNLs exist and others don't
     * Then get those legal entities that could be found
     */
    @Test
    fun `find legal entities by BPN, some BPNs not found`() {
        val givenStructures = listOf(
            LegalEntityStructureRequest(RequestValues.legalEntityCreate1),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate2),
            LegalEntityStructureRequest(RequestValues.legalEntityCreate3)
        )
        val givenLegalEntities = testHelpers.createBusinessPartnerStructure(givenStructures).map { it.legalEntity }

        val expected = givenLegalEntities.map {
            LegalEntityPartnerResponse(
                bpn = it.bpn,
                properties = it.properties,
                currentness = it.currentness
            )
        }.take(2) // only search for a subset of the existing legal entities

        val bpnsToSearch = expected.map { it.bpn }.plus("NONEXISTENT") // also search for nonexistent BPN

        val response = poolClient.legalEntities().searchSites(bpnsToSearch).body
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
        //val bpnL = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(EndpointValues.CATENA_LEGAL_ENTITY_PATH, given).single().bpn
        val bpnL = poolClient.legalEntities().createBusinessPartners(given).single().bpn
        val initialCurrentness = retrieveCurrentness(bpnL)
        val instantBeforeCurrentnessUpdate = Instant.now()

        assertThat(initialCurrentness).isBeforeOrEqualTo(instantBeforeCurrentnessUpdate)

        poolClient.legalEntities().setLegalEntityCurrentness(bpnL)


        val updatedCurrentness = poolClient.legalEntities().getLegalEntity(bpnL).currentness
        assertThat(updatedCurrentness).isBetween(instantBeforeCurrentnessUpdate, Instant.now())
    }

    /**
     * Given business partners imported
     * When trying to update currentness using a nonexistent bpn
     * Then a "not found" response is sent
     */
    @Test
    fun `set business partner currentness using nonexistent bpn`() {

        testHelpers.`set business partner currentness using nonexistent bpn`("NONEXISTENT_BPN")

    }

    fun assertThatCreatedLegalEntitiesEqual(actuals: Collection<LegalEntityPartnerCreateResponse>, expected: Collection<LegalEntityPartnerCreateResponse>) {
        val now = Instant.now()
        val justBeforeCreate = now.minusSeconds(2)
        actuals.forEach { assertThat(it.currentness).isBetween(justBeforeCreate, now) }
        actuals.forEach { assertThat(it.bpn).matches(testHelpers.bpnLPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields(LegalEntityPartnerCreateResponse::currentness.name, LegalEntityPartnerCreateResponse::bpn.name)
            .isEqualTo(expected)
    }

    fun assertThatModifiedLegalEntitiesEqual(actuals: Collection<LegalEntityPartnerCreateResponse>, expected: Collection<LegalEntityPartnerCreateResponse>) {
        val now = Instant.now()
        val justBeforeCreate = now.minusSeconds(2)
        actuals.forEach { assertThat(it.currentness).isBetween(justBeforeCreate, now) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields(LegalEntityPartnerCreateResponse::currentness.name, LegalEntityPartnerCreateResponse::index.name)
            .isEqualTo(expected)
    }

    private fun retrieveCurrentness(bpn: String) = webTestClient
        .get()
        .uri(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpn}")
        .exchange().expectStatus().isOk
        .returnResult<LegalEntityPartnerResponse>()
        .responseBody
        .blockFirst()!!.currentness

    private fun changeCase(value: String): String {
        return if (value.uppercase() != value)
            value.uppercase()
        else if (value.lowercase() != value)
            value.lowercase()
        else
            throw IllegalArgumentException("Can't change case of string $value")
    }
}