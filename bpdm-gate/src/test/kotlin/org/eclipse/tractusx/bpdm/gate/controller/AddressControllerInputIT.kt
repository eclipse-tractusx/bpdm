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
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationStatus
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository

import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_RELATIONS_PATH
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class AddressControllerInputIT @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val saasConfigProperties: SaasConfigProperties,
    val gateClient: GateClient,
    private val gateAddressRepository: GateAddressRepository
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

    @BeforeEach
    fun beforeEach() {
        wireMockServer.resetAll()
    }

    /**
     * Given address exists in SaaS
     * When getting address by external id
     * Then address mapped to the catena data model should be returned
     */
    @Test
    fun `get address by external id`() {
        val externalIdToQuery = SaasValues.addressBusinessPartnerWithRelations1.externalId!!
        val expectedAddress = ResponseValues.addressGateInputResponse1

        val addressRequest = FetchRequest(
            dataSource = saasConfigProperties.datasource,
            externalId = externalIdToQuery,
            featuresOn = listOf(FetchRequest.SaasFeatures.FETCH_RELATIONS)
        )
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(addressRequest)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = SaasValues.addressBusinessPartnerWithRelations1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val parentRequest = FetchRequest(
            dataSource = saasConfigProperties.datasource,
            externalId = SaasValues.legalEntityRequest1.externalId!!,
            featuresOn = listOf(FetchRequest.SaasFeatures.FETCH_RELATIONS)
        )
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(parentRequest)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = SaasValues.legalEntityResponse1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )


        val valueResponse = gateClient.addresses().getAddressByExternalId(externalIdToQuery)

        assertThat(valueResponse).usingRecursiveComparison().isEqualTo(expectedAddress)
    }

    /**
     * Given address does not exist in SaaS
     * When getting address by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get address by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = null,
                                    status = FetchResponse.Status.NOT_FOUND
                                )
                            )
                        )
                )
        )

        try {
            gateClient.addresses().getAddressByExternalId("NONEXISTENT_BPN")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }
    }

    /**
     * When SaaS api responds with an error status code while fetching address by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get address by external id, SaaS error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try {
            gateClient.addresses().getAddressByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            Assertions.assertTrue(statusCodeValue in 500..599)
        }
    }

    /**
     * Given address business partner without address data in SaaS
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get address for which SaaS data is invalid, expect error`() {

        val invalidPartner = SaasValues.addressBusinessPartnerWithRelations1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = invalidPartner,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        try {
            gateClient.addresses().getAddressByExternalId(SaasValues.addressBusinessPartnerWithRelations1.externalId.toString())
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            Assertions.assertTrue(statusCodeValue in 500..599)
        }

    }

    /**
     * Given addresses exists in SaaS
     * When getting addresses page
     * Then addresses page mapped to the catena data model should be returned
     */


    @Test
    fun `get addresses`() {
        val addressesSaas = listOf(
            SaasValues.addressBusinessPartnerWithRelations1,
            SaasValues.addressBusinessPartnerWithRelations2
        )

        val parentsSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.siteBusinessPartner1
        )

        val expectedAddresses = listOf(
            ResponseValues.addressGateInputResponse1,
            ResponseValues.addressGateInputResponse2,
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 0

        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = addressesSaas
                                )
                            )
                        )
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = parentsSaas.size,
                                    startAfter = null,
                                    nextStartAfter = null,
                                    total = parentsSaas.size,
                                    values = parentsSaas
                                )
                            )
                        )
                )
        )

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.addresses().getAddresses(paginationValue)

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = invalidEntries
            )
        )
    }


    /**
     * Given addresses exists in SaaS
     * When getting addresses page based on external id list
     * Then addresses page mapped to the catena data model should be returned
     */
    @Test
    fun `get addresses filter by external ids`() {
        val addressesSaas = listOf(
            SaasValues.addressBusinessPartnerWithRelations1,
            SaasValues.addressBusinessPartnerWithRelations2
        )

        val parentsSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.siteBusinessPartner1
        )

        val expectedAddresses = listOf(
            ResponseValues.addressGateInputResponse1,
            ResponseValues.addressGateInputResponse2,
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 0



        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", matching(".*"))
                .withQueryParam("typeTechnicalKeys", matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = addressesSaas
                                )
                            )
                        )
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", matching(".*"))
                .withQueryParam("typeTechnicalKeys", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = parentsSaas.size,
                                    startAfter = null,
                                    nextStartAfter = null,
                                    total = parentsSaas.size,
                                    values = parentsSaas
                                )
                            )
                        )
                )
        )

        val listExternalIds = addressesSaas.mapNotNull { it.externalId }

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.addresses().getAddressesByExternalIds(paginationValue, listExternalIds)

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * Given invalid addresses in SaaS
     * When getting addresses page
     * Then only valid addresses on page returned
     */
    @Test
    fun `filter invalid addresses`() {
        val addressesSaas = listOf(
            SaasValues.addressBusinessPartnerWithRelations1,
            SaasValues.addressBusinessPartnerWithRelations2,
            SaasValues.addressBusinessPartnerWithRelations1.copy(addresses = emptyList()), // address without address data
        )

        val parentsSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.siteBusinessPartner1
        )

        val expectedAddresses = listOf(
            ResponseValues.addressGateInputResponse1,
            ResponseValues.addressGateInputResponse2,
        )

        val limit = 3
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 1

        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = addressesSaas
                                )
                            )
                        )
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = parentsSaas.size,
                                    startAfter = null,
                                    nextStartAfter = null,
                                    total = parentsSaas.size,
                                    values = parentsSaas
                                )
                            )
                        )
                )
        )

        val paginationValues = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.addresses().getAddresses(paginationValues)

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * When SaaS api responds with an error status code while getting addresses
     * Then an internal server error response should be sent
     */
    @Test
    fun `get addresses, SaaS error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try {
            gateClient.addresses().getAddresses(PaginationStartAfterRequest(""))
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            Assertions.assertTrue(statusCodeValue in 500..599)
        }

    }

    /**
     * When requesting too many addresses
     * Then a bad request response should be sent
     */
    @Test
    fun `get addresses, pagination limit exceeded`() {

        val paginationRequest = PaginationStartAfterRequest("", limit = 999999)

        try {
            gateClient.addresses().getAddresses(paginationRequest)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entities and sites in SaaS
     * When upserting addresses of legal entities and sites
     * Then upsert addresses and relations in SaaS api should be called with the address data mapped to the SaaS data model
     */
    @Test
    fun `upsert addresses`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val legalEntity = listOf(
            RequestValues.legalEntityGateInputRequest1,
        )

        val sites = listOf(
            RequestValues.siteGateInputRequest1,
        )

        val parentLegalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1
        )

        val parentSitesSaas = listOf(
            SaasValues.siteBusinessPartner1
        )

        val expectedAddresses = listOf(
            SaasValues.addressBusinessPartnerRequest1,
            SaasValues.addressBusinessPartnerRequest2,
        )

        val expectedRelations = listOf(
            SaasValues.relationAddress1ToLegalEntity,
            SaasValues.relationAddress2ToSite
        )

        val expectedDeletedRelations = listOf(
            DeleteRelationsRequestSaas.RelationToDeleteSaas(
                startNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.addressBusinessPartnerWithRelations1.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.addressBusinessPartnerWithRelations1.relations.first().endNode
                ),
            ),
            DeleteRelationsRequestSaas.RelationToDeleteSaas(
                startNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.addressBusinessPartnerWithRelations2.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.addressBusinessPartnerWithRelations2.relations.first().endNode
                ),
            ),
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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
        val stubMappingUpsertAddresses = wireMockServer.stubFor(
            put(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                UpsertResponse(
                                    emptyList(),
                                    emptyList(),
                                    2,
                                    0
                                )
                            )
                        )
                )
        )
        // mock "get addresses with relations"
        // this simulates the case that the address already had some relations
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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
        val stubMappingDeleteRelations = wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_DELETE_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                DeleteRelationsResponseSaas(2)
                            )
                        )
                )
        )
        val stubMappingUpsertRelations = wireMockServer.stubFor(
            put(urlPathMatching(SAAS_MOCK_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                UpsertRelationsResponseSaas(
                                    failures = emptyList(),
                                    numberOfFailed = 0,
                                    numberOfInserts = 2,
                                    numberOfProvidedRelations = 2,
                                    numberOfUpdates = 0
                                )
                            )
                        )
                )
        )


        try {
            gateClient.addresses().upsertAddresses(addresses)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted Address data
        val addressExternal1 = gateAddressRepository.findByExternalId("address-external-1")
        assertNotEquals(addressExternal1, null)

        val addressExternal2 = gateAddressRepository.findByExternalId("address-external-2")
        assertNotEquals(addressExternal2, null)

        // TODO
//        val upsertAddressesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertAddresses, objectMapper).single()
//        assertThat(upsertAddressesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedAddresses)

        // check that "delete relations" was called in SaaS as expected
        val deleteRelationsRequestSaas =
            wireMockServer.deserializeMatchedRequests<DeleteRelationsRequestSaas>(stubMappingDeleteRelations, objectMapper).single()
        assertThat(deleteRelationsRequestSaas.relations).containsExactlyInAnyOrderElementsOf(expectedDeletedRelations)

        val upsertRelationsRequest = wireMockServer.deserializeMatchedRequests<UpsertRelationsRequestSaas>(stubMappingUpsertRelations, objectMapper).single()
        assertThat(upsertRelationsRequest.relations).containsExactlyInAnyOrderElementsOf(expectedRelations)
    }

    /**
     * When upserting addresses of legal entities using a legal entity external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert addresses, legal entity parent not found`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 0,
                                    values = emptyList<BusinessPartnerSaas>()
                                )
                            )
                        )
                )
        )

        try {
            gateClient.addresses().upsertAddresses(addresses)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * When upserting addresses of sites using a site external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert addresses, site parent not found`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest2
        )

        // mock "get parent sites"
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.siteExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 0,
                                    values = emptyList<BusinessPartnerSaas>()
                                )
                            )
                        )
                )
        )

        try{
            gateClient.addresses().upsertAddresses(addresses)
        }catch (e: WebClientResponseException){
            assertEquals(HttpStatus.BAD_REQUEST,e.statusCode)
        }

    }

    /**
     * When upserting an address without reference to either a parent site or a parent legal entity
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert address without any parent`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1.copy(
                siteExternalId = null,
                legalEntityExternalId = null
            )
        )

        try {
            gateClient.addresses().upsertAddresses(addresses)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * When upserting an address without reference to both a parent site and a parent legal entity
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert address with site and legal entity parents`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1.copy(
                siteExternalId = CommonValues.externalIdSite1,
                legalEntityExternalId = CommonValues.externalId1
            )
        )

        try {
            gateClient.addresses().upsertAddresses(addresses)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given valid address partner
     * When validate that address partner
     * Then response is OK and no errors
     */
    @Test
    fun `validate a valid address partner`() {
        val address = RequestValues.addressGateInputRequest2

        val mockParent = SaasValues.siteBusinessPartner1
        val mockParentResponse = PagedResponseSaas(1, null, null, 1, listOf(mockParent))
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockParentResponse))
                )
        )

        val mockDefects = listOf(
            DataDefectSaas(ViolationLevel.INFO, "Info"),
            DataDefectSaas(ViolationLevel.NO_DEFECT, "No Defect"),
            DataDefectSaas(ViolationLevel.WARNING, "Warning"),
        )
        val mockResponse = ValidationResponseSaas(mockDefects)
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_DATA_VALIDATION_BUSINESSPARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))
                )
        )

        val actualResponse = gateClient.addresses().validateSite(address)

        val expectedResponse = ValidationResponse(ValidationStatus.OK, emptyList())

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    /**
     * Given invalid address partner
     * When validate that address partner
     * Then response is ERROR and contain error description
     */
    @Test
    fun `validate an invalid site`() {
        val address = RequestValues.addressGateInputRequest2


        val mockParent = SaasValues.siteBusinessPartner1
        val mockParentResponse = PagedResponseSaas(1, null, null, 1, listOf(mockParent))
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockParentResponse))
                )
        )


        val mockErrorMessage = "Validation error"
        val mockDefects = listOf(
            DataDefectSaas(ViolationLevel.ERROR, mockErrorMessage),
            DataDefectSaas(ViolationLevel.INFO, "Info"),
            DataDefectSaas(ViolationLevel.NO_DEFECT, "No Defect"),
            DataDefectSaas(ViolationLevel.WARNING, "Warning"),
        )

        val mockResponse = ValidationResponseSaas(mockDefects)
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_DATA_VALIDATION_BUSINESSPARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))
                )
        )

        val actualResponse = gateClient.addresses().validateSite(address)

        val expectedResponse = ValidationResponse(ValidationStatus.ERROR, listOf(mockErrorMessage))

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }
}