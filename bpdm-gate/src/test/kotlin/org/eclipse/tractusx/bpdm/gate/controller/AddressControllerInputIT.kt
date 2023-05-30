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
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationStatus
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
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
    val gateClient: GateClient,
    private val gateAddressRepository: GateAddressRepository,
    val testHelpers: DbTestHelpers,
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
        testHelpers.truncateDbTables()
    }

    /**
     * Given address exists in the persistence database
     * When getting address by external id
     * Then address mapped to the catena data model should be returned
     */
    @Test
    fun `get address by external id`() {

        val externalIdToQuery = CommonValues.externalIdAddress2
        val expectedAddress = ResponseValues.logisticAddressGateInputResponse2

        val addresses = listOf(
            RequestValues.addressGateInputRequest2
        )

        gateClient.addresses().upsertAddresses(addresses)

        val valueResponse = gateClient.addresses().getAddressByExternalId(externalIdToQuery)

        assertThat(valueResponse).usingRecursiveComparison().isEqualTo(expectedAddress)
    }

    /**
     * Given address does not exist in the persistence database
     * When getting address by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get address by external id, not found`() {

        try {
            gateClient.addresses().getAddressByExternalId("NONEXISTENT_BPN")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }
    }

    /**
     * Given addresses exists in the persistence database
     * When getting addresses page
     * Then addresses page mapped to the catena data model should be returned
     */

    @Test
    fun `get addresses`() {

        val expectedAddresses = listOf(
            ResponseValues.logisticAddressGateInputResponse1,
            ResponseValues.logisticAddressGateInputResponse2,
        )

        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        gateClient.addresses().upsertAddresses(addresses)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.addresses().getAddresses(paginationValue)

        assertThat(pageResponse).isEqualTo(
            PageResponse(
                totalElements = totalElements,
                totalPages = totalPages,
                page = pageValue,
                contentSize = contentSize,
                content = expectedAddresses
            )
        )
    }


    /**
     * Given addresses exists in the Persistence Database
     * When getting addresses page based on external id list
     * Then addresses should be returned
     */
    @Test
    fun `get addresses filter by external ids`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val expectedAddresses = listOf(
            ResponseValues.logisticAddressGateInputResponse1,
            ResponseValues.logisticAddressGateInputResponse2,
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        val listExternalIds = addresses.map { it.externalId }

        gateClient.addresses().upsertAddresses(addresses)

        val pagination = PaginationRequest(page, size)
        val pageResponse = gateClient.addresses().getAddressesByExternalIds(pagination, listExternalIds)

        assertThat(pageResponse).isEqualTo(
            PageResponse(
                totalElements = totalElements,
                totalPages = totalPages,
                page = pageValue,
                contentSize = contentSize,
                content = expectedAddresses
            )
        )
    }

    /**
     * When requesting too many addresses
     * Then a bad request response should be sent
     */
    @Test
    fun `get addresses, pagination limit exceeded`() {

        val page = 0
        val size = 999999

        val paginationRequest = PaginationRequest(page, size)

        try {
            gateClient.addresses().getAddresses(paginationRequest)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entities and sites
     * When upserting addresses of legal entities and sites or a new one
     * Then upsert addresses should be persisted on the database
     */
    @Test
    fun `upsert addresses`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
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

    }

    /**
     * When upserting addresses
     * if both have both have the same externalId, "bad request" should show
     */
    @Test
    fun `upsert addresses, same externalid`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest1
        )

        try {
            gateClient.addresses().upsertAddresses(addresses)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

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