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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.util.*
import org.junit.jupiter.api.Assertions
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
internal class AddressControllerOutputIT @Autowired constructor(
    val gateClient: GateClient,
    private val gateAddressRepository: GateAddressRepository,
    val testHelpers: DbTestHelpers
) {
    companion object {
        @RegisterExtension
        private val wireMockServerSaas: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @RegisterExtension
        private val wireMockServerBpdmPool: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServerSaas.baseUrl() }
            registry.add("bpdm.pool.base-url") { wireMockServerBpdmPool.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * If there is an Input Address persisted,
     * upsert the Output with same external id
     */
    @Test
    fun `upsert output addresses`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val addressesOutput = listOf(
            RequestValues.addressGateOutputRequest1,
            RequestValues.addressGateOutputRequest2
        )

        try {
            gateClient.addresses().upsertAddresses(addresses)
            gateClient.addresses().putAddressesOutput(addressesOutput)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted Address data
        val addressExternal1 = gateAddressRepository.findByExternalIdAndDataType("address-external-1", OutputInputEnum.Output)
        Assertions.assertNotEquals(addressExternal1, null)

        val addressExternal2 = gateAddressRepository.findByExternalIdAndDataType("address-external-2", OutputInputEnum.Output)
        Assertions.assertNotEquals(addressExternal2, null)

    }

    /**
     * If there isn't an Input Address persisted,
     * when upserting an output address, it should show an 400
     */
    @Test
    fun `upsert output addresses, no input persisted`() {
        val addresses = listOf(
            RequestValues.addressGateOutputRequest1,
            RequestValues.addressGateOutputRequest2
        )

        try {
            gateClient.addresses().putAddressesOutput(addresses)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given output addresses exists in the database
     * When getting addresses page via output route
     * Then addresses page should be returned
     */
    @Test
    fun `get output addresses`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val addressesOutput = listOf(
            RequestValues.addressGateOutputRequest1,
            RequestValues.addressGateOutputRequest2
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

        gateClient.addresses().upsertAddresses(addresses)
        gateClient.addresses().putAddressesOutput(addressesOutput)


        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.addresses().getAddressesOutput(paginationValue, emptyList())

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*processStartedAt*").isEqualTo(
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
     * Given addresses exists in the database
     * When getting addresses page via output route filtering by external ids
     * Then addresses page should be returned
     */
    @Test
    fun `get addresses, filter by external ids`() {
        val addresses = listOf(
            RequestValues.addressGateInputRequest1,
            RequestValues.addressGateInputRequest2
        )

        val addressesOutput = listOf(
            RequestValues.addressGateOutputRequest1,
            RequestValues.addressGateOutputRequest2
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

        gateClient.addresses().upsertAddresses(addresses)
        gateClient.addresses().putAddressesOutput(addressesOutput)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.addresses().getAddressesOutput(paginationValue, listOf(CommonValues.externalIdAddress1, CommonValues.externalIdAddress2))

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*processStartedAt*").isEqualTo(
            PageResponse(
                totalElements = totalElements,
                totalPages = totalPages,
                page = pageValue,
                contentSize = contentSize,
                content = expectedAddresses
            )
        )
    }
}