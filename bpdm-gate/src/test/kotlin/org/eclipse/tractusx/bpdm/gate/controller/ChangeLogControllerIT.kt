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

package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangeLogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.CommonValues.lsaTypeParam
import org.eclipse.tractusx.bpdm.gate.util.CommonValues.lsaTypeParamNotFound
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class ChangeLogControllerIT @Autowired constructor(
    val gateClient: GateClient,
    private val objectMapper: ObjectMapper,
    val saasConfigProperties: SaasConfigProperties,
    private val testHelpers: DbTestHelpers,
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }


    val instant = Instant.now()

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        wireMockServer.resetAll()
        mockSaas()
        createChangeLogs()
    }

    /**
     * Given externalId exists in database
     * When getting changeLog by external id
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by external id`() {

        val searchRequest = ChangeLogSearchRequest(externalIds = setOf(CommonValues.externalIdAddress1))

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content)
            .ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::modifiedAt.name}")
            .isEqualTo(listOf(ChangelogGateDto(CommonValues.externalIdAddress1, lsaTypeParam, instant)))
    }


    /**
     * Given externalId does not exist in database
     * When getting changeLog by external id
     * Then changeLog mapped to the catena data model should not be returned
     */
    @Test
    fun `get changeLog by external id not found`() {

        val searchRequest = ChangeLogSearchRequest(externalIds = setOf("NONEXIST"))

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(emptyList<ChangelogEntry>())

        assertRecursively(searchResult.content)
            .isEqualTo(emptyList<ChangelogEntry>())

        assertRecursively(searchResult.errors)
            .isEqualTo(
                listOf(
                    ErrorInfo(
                        ChangeLogOutputError.ExternalIdNotFound,
                        "NONEXIST not found",
                        "NONEXIST"
                    )
                )
            )


    }

    /**
     * Given externalId and timestamp a changeLog exist in database
     * When getting changeLog by external id and timestamp
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by external id and timeStamp`() {

        val searchRequest = ChangeLogSearchRequest(externalIds = setOf(CommonValues.externalIdAddress1), fromTime = instant)

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)


        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::modifiedAt.name}")
            .isEqualTo(listOf(ChangelogGateDto(CommonValues.externalIdAddress1, lsaTypeParam, instant)))
    }

    /**
     * Given a lsaType a changeLog exists in database
     * When getting changeLog by lsaType
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by lsaType`() {

        val searchRequest = ChangeLogSearchRequest(lsaTypes = setOf(lsaTypeParam))

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::modifiedAt.name}")
            .isEqualTo(listOf(ChangelogGateDto(CommonValues.externalIdAddress1, lsaTypeParam, instant)))
    }

    /**
     * Given lsaType does not exist in database
     * When getting changeLog by lsaType
     * Then changeLog mapped to the catena data model should not be returned
     */

    @Test
    fun `get changeLog by lsaType not found`() {
        val searchRequest = ChangeLogSearchRequest(lsaTypes = setOf(lsaTypeParamNotFound))

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content)
            .isEqualTo(emptyList<ChangelogEntry>())
    }

    /**
     * Given lsaType and timestamp a changeLog exist in database
     * When getting changeLog by lsaType and timestamp
     * Then changeLog mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog by lsaType and timeStamp`() {
        val searchRequest = ChangeLogSearchRequest(lsaTypes = setOf(lsaTypeParam), fromTime = instant)

        val searchResult = gateClient.changelog().getInputChangelog(PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::modifiedAt.name}")
            .isEqualTo(listOf(ChangelogGateDto(CommonValues.externalIdAddress1, lsaTypeParam, instant)))
    }

    /**
     * Given a timeStamp a changeLog exists in database
     * When getting changeLog by timeStamp
     * Then changeLog from that instant until now is mapped to the catena data model should be returned
     */
    @Test
    fun `get changeLog from timeStamp`() {

        val searchRequest = ChangeLogSearchRequest(lsaTypes = emptySet(), fromTime = instant)

        val searchResult = gateClient.changelog().getInputChangelog(paginationRequest = PaginationRequest(), searchRequest)

        assertRecursively(searchResult.content).ignoringFieldsMatchingRegexes(".*${ChangelogGateDto::modifiedAt.name}")
            .isEqualTo(listOf(ChangelogGateDto(CommonValues.externalIdAddress1, lsaTypeParam, instant)))
    }

    fun mockSaas() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1
        )

        val parentLegalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1
        )

        val parentSitesSaas = listOf(
            SaasValues.siteBusinessPartner1
        )


        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 1,
                                    values = parentLegalEntitiesSaas
                                )
                            )
                        )
                )
        )
        // mock "get parent sites"
        wireMockServer.stubFor(
            get(urlPathMatching(EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.siteExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 1,
                                    values = parentSitesSaas
                                )
                            )
                        )
                )
        )

        // mock "get addresses with relations"
        // this simulates the case that the address already had some relations
        wireMockServer.stubFor(
            get(urlPathMatching(EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.map { it.externalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .withQueryParam("featuresOn", containing("FETCH_RELATIONS"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 2,
                                    values = listOf(
                                        SaasValues.addressBusinessPartnerWithRelations1,
                                        SaasValues.addressBusinessPartnerWithRelations2
                                    )
                                )
                            )
                        )
                )
        )
    }


    fun <T> assertRecursively(actual: T): RecursiveComparisonAssert<*> {
        return assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()

    }

    /**
     * Creates changelog entities
     * Retains the order: All response objects will be in the same order as their request counterparts
     * Assumption: Changelog entities have unique indexes among them each
     */
    fun createChangeLogs() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1
        )
        gateClient.addresses().upsertAddresses(addresses)
    }

}