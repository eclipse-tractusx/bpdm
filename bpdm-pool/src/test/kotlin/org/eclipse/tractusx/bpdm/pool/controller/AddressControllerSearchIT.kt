/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.LegalEntityStructureRequest
import org.eclipse.tractusx.bpdm.test.testdata.pool.SiteStructureRequest
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

/**
 * Integration tests for the search endpoint of the address controller
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerSearchIT @Autowired constructor(
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelpers: PoolDataHelpers,
    val assertHelpers: AssertHelpers,
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {

    // TODO Improve and reorganize our testdata
    //  Currently our testdata is very limited, e.g. the same physical address (postalAddress1) is shared between regular address addressPartnerCreate1,
    //  the legal address from legalEntityCreate1 and the main address from siteCreate1. This leads to unexpected results in the test cases.
    //  Furthermore, it's not transparent in the test cases why some text search query should lead to some results. For that you need to dig through the
    //  shared test data. Probably it would be better if each test case created its own test data with explicit values specific to the test case and
    //  the expected result, ideally with minimal effort utilizing helper functions or probably Kotlin's powerful builders.

    val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
        addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1, BusinessPartnerNonVerboseValues.addressPartnerCreate3)
    )


    val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(
                site = BusinessPartnerNonVerboseValues.siteCreate1,
                addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate2)
            )
        )
    )

    val partnerStructure3 = LegalEntityStructureRequest(
        legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
        addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate4)
    )

    private lateinit var givenAddress1: LogisticAddressVerboseDto


    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()


        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure3))
        givenAddress1 = givenStructure[0].addresses[0].address                      // addressPartnerCreate1
    }


    /**
     * Given addresses
     * When searching an address by name of BPN search criteria
     * Then the matching address is returned
     */
    @Test
    fun `search address via name`() {
        val expected = PageDto(
            1, 1, 0, 1, listOf(
                givenAddress1
            )
        )


        val addressSearchRequest = AddressSearchRequest(name = BusinessPartnerNonVerboseValues.addressPartnerCreate4.address.name)
        val pageResponse = poolClient.addresses.getAddresses(addressSearchRequest, PaginationRequest())

        assertHelpers.assertRecursively(pageResponse).isEqualTo(expected)
    }

    /**
     * Given addresses
     * When searching an address by name of BPN that not exists in search criteria
     * Then the matching address is not found
     */
    @Test
    fun `search address via name not found`() {
        val expected = PageDto(
            0, 0, 0, 0, emptyList<LogisticAddressVerboseDto>()
        )

        val addressSearchRequest = AddressSearchRequest(name =  "NONEXISTENT")
        val pageResponse = poolClient.addresses.getAddresses(addressSearchRequest, PaginationRequest())

        assertHelpers.assertRecursively(pageResponse).isEqualTo(expected)
    }

}