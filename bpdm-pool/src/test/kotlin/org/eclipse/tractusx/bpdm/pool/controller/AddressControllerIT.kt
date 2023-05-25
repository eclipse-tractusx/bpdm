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
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerIT @Autowired constructor(
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
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate2)
                )
            )
        )

        val importedPartner = createdStructures.single().legalEntity
        val addressesByBpnL = importedPartner.legalEntity.bpnl
            .let { bpnL -> requestAddressesOfLegalEntity(bpnL).content }
        // 1 legal address, 1 regular address
        assertThat(addressesByBpnL.size).isEqualTo(2)
        assertThat(addressesByBpnL.count { it.isLegalAddress }).isEqualTo(1)

        // Same address if we use the address-by-BPNA method
        addressesByBpnL
            .forEach { address ->
                val addressByBpnA = requestAddress(address.bpna)
                assertThat(addressByBpnA.bpnLegalEntity).isEqualTo(importedPartner.legalEntity.bpnl)
                assertThat(addressByBpnA).isEqualTo(address)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1)
                )
            )
        )

        testHelpers.`get address by bpn-a, not found`("NONEXISTENT_BPN")

    }

    /**
     * Given multiple address partners
     * When searching addresses with BPNA
     * Then return those addresses
     */
    @Test
    fun `search addresses by BPNA`() {

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2, RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = createdStructures[0].addresses[0].address.bpna
        val bpnA2 = createdStructures[0].addresses[1].address.bpna

        val searchRequest = AddressPartnerBpnSearchRequest(addresses = listOf(bpnA1, bpnA2))
        val searchResult =
            poolClient.addresses().searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            ResponseValues.addressPartner1,
            ResponseValues.addressPartner2
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNL
     * Then return addresses belonging to those legal entities (including legal addresses!)
     */
    @Test
    fun `search addresses by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    // no additional addresses
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val searchRequest = AddressPartnerBpnSearchRequest(legalEntities = listOf(bpnL2))
        val searchResult = poolClient.addresses().searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            ResponseValues.addressPartner2.copy(isLegalAddress = true),
            ResponseValues.addressPartner3
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNS
     * Then return addresses belonging to those sites (including main addresses!)
     */
    @Test
    fun `search addresses by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate2,
                            addresses = listOf(RequestValues.addressPartnerCreate3)
                        )
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[1].siteStructures[0].site.site.bpns

        // search for site1 -> main address and 2 regular addresses
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS1))
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        ResponseValues.addressPartner1.copy(isMainAddress = true),
                        ResponseValues.addressPartner1,
                        ResponseValues.addressPartner2,
                    )
                )
            }

        // search for site2 -> main address and 1 regular address
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS2))       // search for site2
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        ResponseValues.addressPartner2.copy(isMainAddress = true),
                        ResponseValues.addressPartner3,
                    )
                )
            }

        // search for site1 and site2 -> 2 main addresses and 3 regular addresses
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS2, bpnS1))    // search for site1 and site2
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        // site1
                        ResponseValues.addressPartner1.copy(isMainAddress = true),
                        ResponseValues.addressPartner1,
                        ResponseValues.addressPartner2,
                        // site2
                        ResponseValues.addressPartner2.copy(isMainAddress = true),
                        ResponseValues.addressPartner3,
                    )
                )
            }
    }

    /**
     * Given sites and legal entities
     * When creating addresses for sites and legal entities
     * Then new addresses created and returned
     */
    @Test
    fun `create new addresses`() {

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
            )
        )

        val bpnL = givenStructure[0].legalEntity.legalEntity.bpnl
        val bpnS = givenStructure[0].siteStructures[0].site.site.bpns

        val expected = listOf(
            ResponseValues.addressPartnerCreate1,
            ResponseValues.addressPartnerCreate2,
            ResponseValues.addressPartnerCreate3
        )

        val toCreate = listOf(
            RequestValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate2.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate3.copy(bpnParent = bpnS)
        )

        val response = poolClient.addresses().createAddresses(toCreate)

        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        testHelpers.assertRecursively(response.entities)
//            .ignoringFields(LogisticAddressResponse::bpn.name)
//            .isEqualTo(expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given sites and legal entities
     * When creating addresses with some having non-existent parents
     * Then only addresses with existing parents created and returned
     */
    @Test
    fun `don't create addresses with non-existent parent`() {
        val bpnL = poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1))
            .entities.single().legalEntity.bpnl

        val expected = listOf(
            ResponseValues.addressPartnerCreate1,
        )

        val invalidSiteBpn = "BPNSXXXXXXXXXX"
        val invalidLegalEntityBpn = "BPNLXXXXXXXXXX"
        val completelyInvalidBpn = "XYZ"
        val toCreate = listOf(
            RequestValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate1.copy(bpnParent = invalidSiteBpn),
            RequestValues.addressPartnerCreate2.copy(bpnParent = invalidLegalEntityBpn),
            RequestValues.addressPartnerCreate3.copy(bpnParent = completelyInvalidBpn),
        )

        val response = poolClient.addresses().createAddresses(toCreate)
        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        testHelpers.assertRecursively(response.entities).ignoringFields(LogisticAddressResponse::bpn.name).isEqualTo(expected)

        assertThat(response.errorCount).isEqualTo(3)
        testHelpers.assertErrorResponse(response.errors.elementAt(0), AddressCreateError.BpnNotValid, CommonValues.index3)   // BPN validity check always first
        testHelpers.assertErrorResponse(response.errors.elementAt(1), AddressCreateError.SiteNotFound, CommonValues.index1)
        testHelpers.assertErrorResponse(response.errors.elementAt(2), AddressCreateError.LegalEntityNotFound, CommonValues.index2)
    }

    /**
     * Given addresses
     * When updating addresses via BPNs
     * Then update and return those addresses
     */
    @Test
    fun `update addresses`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna
        val bpnA2 = givenStructure[0].siteStructures[0].addresses[1].address.bpna
        val bpnA3 = givenStructure[1].addresses[0].address.bpna

        val expected = listOf(
            ResponseValues.addressPartner1.copy(bpna = bpnA2),
            ResponseValues.addressPartner2.copy(bpna = bpnA3),
            ResponseValues.addressPartner3.copy(bpna = bpnA1)
        )

        val toUpdate = listOf(
            RequestValues.addressPartnerUpdate1.copy(bpn = bpnA2),
            RequestValues.addressPartnerUpdate2.copy(bpn = bpnA3),
            RequestValues.addressPartnerUpdate3.copy(bpn = bpnA1)
        )

        val response = poolClient.addresses().updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given addresses
     * When updating addresses with some having non-existent BPNs
     * Then only update and return addresses with existent BPNs
     */
    @Test
    fun `updates addresses, ignore non-existent`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1)
                        )
                    )
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna

        val expected = listOf(
            ResponseValues.addressPartner2.copy(bpna = bpnA1)
        )

        val firstInvalidBpn = "BPNLXXXXXXXX"
        val secondInvalidBpn = "BPNAXXXXXXXX"
        val toUpdate = listOf(
            RequestValues.addressPartnerUpdate2.copy(bpn = bpnA1),
            RequestValues.addressPartnerUpdate2.copy(bpn = firstInvalidBpn),
            RequestValues.addressPartnerUpdate3.copy(bpn = secondInvalidBpn)
        )

        val response = poolClient.addresses().updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)

        assertThat(response.errorCount).isEqualTo(2)
        testHelpers.assertErrorResponse(response.errors.first(), AddressUpdateError.AddressNotFound, firstInvalidBpn)
        testHelpers.assertErrorResponse(response.errors.last(), AddressUpdateError.AddressNotFound, secondInvalidBpn)
    }

    private fun assertCreatedAddressesAreEqual(actuals: Collection<AddressPartnerCreateResponse>, expected: Collection<AddressPartnerCreateResponse>) {
        actuals.forEach { assertThat(it.address.bpna).matches(testHelpers.bpnAPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields("address.bpn", "address.bpnLegalEntity", "address.bpnSite")
            .isEqualTo(expected)
    }

    private fun assertAddressesAreEqual(actuals: Collection<LogisticAddressResponse>, expected: Collection<LogisticAddressResponse>) {
        actuals.forEach { assertThat(it.bpna).matches(testHelpers.bpnAPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields("bpn", "bpnLegalEntity", "bpnSite")
            .isEqualTo(expected)
    }

    private fun requestAddress(bpnAddress: String) = poolClient.addresses().getAddress(bpnAddress)

    private fun requestAddressesOfLegalEntity(bpn: String) =
        poolClient.legalEntities().getAddresses(bpn, PaginationRequest())

}