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

package org.eclipse.tractusx.bpdm.pool.component.cdq.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.dto.response.IdentifierResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityMatchResponse
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


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class CdqControllerImportIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    bpnConfigProperties: BpnConfigProperties,
    private val objectMapper: ObjectMapper,
    private val testHelpers: TestHelpers
) {
    private val idTypeBpn = TypeKeyNameUrlCdq(bpnConfigProperties.id, bpnConfigProperties.name, "")
    private val issuerBpn = TypeKeyNameUrlCdq(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName, "")
    private val statusBpn = TypeKeyNameCdq("UNKNOWN", "Unknown")

    private val idTypeCdq = TypeKeyNameUrlDto("CDQID", "CDQ Identifier", "")
    private val issuerCdq = TypeKeyNameUrlDto("CDQ", "CDQ AG", "")
    private val statusCdq = TypeKeyNameDto("CDQ_IMPORTED", "Imported from CDQ but not synchronized")

    private val identifier1 = IdentifierResponse(CdqValues.partnerId1, idTypeCdq, issuerCdq, statusCdq)
    private val identifier2 = IdentifierResponse(CdqValues.partnerId2, idTypeCdq, issuerCdq, statusCdq)
    private val identifier3 = IdentifierResponse(CdqValues.partnerId3, idTypeCdq, issuerCdq, statusCdq)

    val expectedLegalEntity1 = with(ResponseValues.legalEntity1) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier1))) }
    val expectedLegalEntity2 = with(ResponseValues.legalEntity2) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier2))) }
    val expectedLegalEntity3 = with(ResponseValues.legalEntity3) { copy(properties = properties.copy(identifiers = properties.identifiers.plus(identifier3))) }


    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given new partners in CDQ
     * When Import from CDQ
     * Then partners imported
     */
    @Test
    fun importNewPartners() {

        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner  and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)
    }

    /**
     * Given new partners
     * When Import partners multiple times
     * Then no duplicate partners
     */
    @Test
    fun importPartnersMultipleTimes() {
        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner first time and check whether successfully imported
        val firstResponse = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(firstResponse.content.map { it.legalEntity }, partnersExpected)

        //Import partner second time and check for no duplicates or other changes
        val secondResponse = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(secondResponse.content.map { it.legalEntity }, partnersExpected)
    }

    /**
     * Given new partners in CDQ
     * When import with pagination
     * Then partners imported
     */
    @Test
    fun importNewPartnersWithPagination() {
        val expectedPartners = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3,
        )

        val page1 = PagedResponseCdq(
            1,
            null,
            CdqValues.partnerId2,
            1,
            listOf(CdqValues.businessPartner1)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page1))
                )
        )

        val page2 = PagedResponseCdq(
            1,
            null,
            CdqValues.partnerId3,
            1,
            listOf(CdqValues.businessPartner2)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId2))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page2))
                )
        )

        val page3 = PagedResponseCdq(
            1,
            null,
            null,
            1,
            listOf(CdqValues.businessPartner3)
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(CdqValues.partnerId3))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page3))
                )
        )

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.CDQ_SYNCH_PATH)

        val savedBusinessPartners = webTestClient.invokeGetEndpoint<PageResponse<LegalEntityMatchResponse>>(EndpointValues.CATENA_LEGAL_ENTITY_PATH)
        assertLegalEntityResponseEquals(savedBusinessPartners.content.map { it.legalEntity }, expectedPartners)
    }

    /**
     * Several partner updates should be considered:
     * Given imported partners names are modified in CDQ
     * When import partners
     * Then partners are updated
     */
    @Test
    fun updateModifiedPartners_names(){
        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2,
            CdqValues.businessPartner3
        )

        val partnersExpected = listOf(
            expectedLegalEntity1,
            expectedLegalEntity2,
            expectedLegalEntity3
        )

        //Import partner first and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)

        //retrieve BPNs for update
        val cdqIdBpnPairs = response.content.map { Pair(testHelpers.extractCdqId(it.legalEntity.properties), it.legalEntity.bpn) }
        val bpnL1 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.businessPartner1.id }!!.second
        val bpnL2 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.businessPartner2.id }!!.second
        val bpnL3 = cdqIdBpnPairs.find { (cdqId, _) -> cdqId == CdqValues.businessPartner3.id }!!.second


        //Prepare modified partners to import
        val modifiedName1 = CdqValues.name1.copy(value = "${CdqValues.name1.value}_mod")
        val modifiedName2 = CdqValues.name2.copy(value = "${CdqValues.name2.value}_mod")
        val modifiedName3 = CdqValues.name3.copy(value = "${CdqValues.name3.value}_mod")

        val updatedBusinessPartner1 = plusBpn(CdqValues.businessPartner1.copy(names = listOf(modifiedName1)), bpnL1)
        val updatedBusinessPartner2 = plusBpn(CdqValues.businessPartner2.copy(names = listOf(modifiedName2)), bpnL2)
        val updatedBusinessPartner3 = plusBpn(CdqValues.businessPartner3.copy(names = listOf(modifiedName3)), bpnL3)

        val modifiedNameResponse1 = ResponseValues.name1.copy(value = modifiedName1.value)
        val modifiedNameResponse2 = ResponseValues.name2.copy(value = modifiedName2.value)
        val modifiedNameResponse3 = ResponseValues.name3.copy(value = modifiedName3.value)

        val updatedPartnerResponse1 =
            expectedLegalEntity1.copy(properties = expectedLegalEntity1.properties.copy(names = listOf(modifiedNameResponse1)))
        val updatedPartnerResponse2 =
            expectedLegalEntity2.copy(properties = expectedLegalEntity2.properties.copy(names = listOf(modifiedNameResponse2)))
        val updatedPartnerResponse3 =
            expectedLegalEntity3.copy(properties = expectedLegalEntity3.properties.copy(names = listOf(modifiedNameResponse3)))

        val modifiedPartnersToImport = listOf(
            updatedBusinessPartner1,
            updatedBusinessPartner2,
            updatedBusinessPartner3
        )

        val modifiedExpectedPartners = listOf(
            updatedPartnerResponse1,
            updatedPartnerResponse2,
            updatedPartnerResponse3,
        )

        //Import updated partners from CDQ and check whether updates in our system
        val modifiedResponse = testHelpers.importAndGetResponse(modifiedPartnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(modifiedResponse.content.map { it.legalEntity }, modifiedExpectedPartners)
    }

    /**
     *
     * All modified fields of a partner should be updated:
     * Given imported partner is modified in CDQ
     * When import partners
     * Then partner is updated
     */
    @Test
    fun updateModifiedPartner() {
        val partnersToImport = listOf(
            CdqValues.businessPartner1
        )

        val partnersExpected = listOf(
            expectedLegalEntity1
        )

        //Import partner first and check whether successfully imported
        val response = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(response.content.map { it.legalEntity }, partnersExpected)

        //Prepare modified partner to import
        val bpn = response.content.single().legalEntity.bpn
        val modifiedPartnersToImport = listOf(
            plusBpn(CdqValues.businessPartner3, bpn)
        )

        val modifiedExpectedPartners = listOf(
            expectedLegalEntity3.copy(bpn = bpn)
        )

        //Import updated partners from CDQ and check whether updates in our system
        val modifiedResponse = testHelpers.importAndGetResponse(modifiedPartnersToImport, webTestClient, wireMockServer)
        assertLegalEntityResponseEquals(modifiedResponse.content.map { it.legalEntity }, modifiedExpectedPartners)
    }

    private fun plusBpn(partner: BusinessPartnerCdq, bpn: String) =
        partner.copy(identifiers = partner.identifiers.plus(IdentifierCdq(idTypeBpn, bpn, issuerBpn, statusBpn)))

    private fun assertLegalEntityResponseEquals(
        actualPartners: Collection<LegalEntityPartnerResponse>,
        expectedPartners: Collection<LegalEntityPartnerResponse>
    ) {
        assertThat(actualPartners)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*bpn", ".*currentness")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(expectedPartners)
    }
}