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

import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.config.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for the search endpoint of the address controller
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class AddressControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers,
    val poolClient: PoolApiClient
) {

    val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate1,
        addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate3)
    )

    val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(
                site = RequestValues.siteCreate1,
                addresses = listOf(RequestValues.addressPartnerCreate2)
            )
        )
    )

    private lateinit var givenAddress1: AddressPartnerSearchResponse
    private lateinit var givenAddress2: AddressPartnerSearchResponse
    private lateinit var givenAddress3: AddressPartnerSearchResponse


    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()

        poolClient.opensearch().clear()
        testHelpers.createTestMetadata()

        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2))
        givenAddress1 = with(givenStructure[0].addresses[0]) {
            AddressPartnerSearchResponse(
                address = AddressPartnerResponse(bpn, properties),
                bpnLegalEntity = givenStructure[0].legalEntity.bpn
            )
        }
        givenAddress2 = with(givenStructure[0].addresses[1]) {
            AddressPartnerSearchResponse(
                address = AddressPartnerResponse(bpn, properties),
                bpnLegalEntity = givenStructure[0].legalEntity.bpn
            )
        }
        givenAddress3 = with(givenStructure[1].siteStructures[0].addresses[0]) {
            AddressPartnerSearchResponse(
                address = AddressPartnerResponse(bpn, properties),
                bpnSite = givenStructure[1].siteStructures[0].site.bpn
            )
        }

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by administrative area
     * Then the matching address is returned
     */
    @Test
    fun `search address via administrative area`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )
        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.administrativeArea = (RequestValues.addressPartnerCreate1.properties.administrativeAreas as List).first().value

        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by post code
     * Then the matching address is returned
     */
    @Test
    fun `search address via post code`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.postCode = RequestValues.addressPartnerCreate1.properties.postCodes.first().value

        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by locality
     * Then the matching address is returned
     */
    @Test
    fun `search address via locality`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.locality = RequestValues.addressPartnerCreate1.properties.localities.first().value
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by thoroughfare
     * Then the matching address is returned
     */
    @Test
    fun `search address via thoroughfare`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest(null,null,null,RequestValues.addressPartnerCreate1.properties.thoroughfares.first().value,
        null,null,null)
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by premise
     * Then the matching address is returned
     */
    @Test
    fun `search address via premise`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.premise = RequestValues.addressPartnerCreate1.properties.premises.first().value
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by postal delivery point
     * Then the matching address is returned
     */
    @Test
    fun `search address via postal delivery point`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.postalDeliveryPoint = RequestValues.addressPartnerCreate1.properties.postalDeliveryPoints.first().value
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by country code
     * Then the matching address is returned
     */
    @Test
    fun `search address via country code`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest(countryCode = RequestValues.addressPartnerCreate1.properties.country)

        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())


        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address by multiple search criteria
     * Then the matching address is returned
     */
    @Test
    fun `search address via multiple criteria`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress1)
            )
        )


        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.postalDeliveryPoint = RequestValues.addressPartnerCreate1.properties.postalDeliveryPoints.first().value
        addressSearchRequest.postCode = RequestValues.addressPartnerCreate1.properties.postCodes.first().value
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }


    /**
     * Given addresses in OpenSearch
     * When searching an address by multiple search criteria, one of which does not match any of the existing addresses
     * Then there should be no result
     */
    @Test
    fun `search address via multiple criteria, no match found`() {
        val expected = PageResponse(
            0, 0, 0, 0, emptyList<AddressMatchResponse>()
        )


        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.postCode = RequestValues.addressPartnerCreate1.properties.postCodes.first().value
        addressSearchRequest.administrativeArea = "someNonexistentValue"
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())


        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address via search terms that don't match any of the existing addresses
     * Then there should be no result
     */
    @Test
    fun `search address, no match found`() {
        val expected = PageResponse(
            0, 0, 0, 0, emptyList<AddressMatchResponse>()
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.administrativeArea = "someNonexistentValue"
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given addresses in OpenSearch
     * When searching an address that belongs to a site
     * Then the matching address is returned
     */
    @Test
    fun `search address of site`() {
        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                AddressMatchResponse(0f, givenAddress3)
            )
        )

        val addressSearchRequest = AddressPartnerSearchRequest()
        addressSearchRequest.postCode = RequestValues.addressPartnerCreate2.properties.postCodes.first().value
        val pageResponse = poolClient.addresses().getAddresses(addressSearchRequest, PaginationRequest())

        assertPageEquals(pageResponse, expected)
    }

    private fun assertPageEquals(actual: PageResponse<AddressMatchResponse>, expected: PageResponse<AddressMatchResponse>) {
        testHelpers.assertRecursively(actual)
            .ignoringFieldsMatchingRegexes(".*${AddressMatchResponse::score.name}")
            .isEqualTo(expected)
    }
}