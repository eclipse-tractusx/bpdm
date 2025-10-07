/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.*
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolAssertHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
@ActiveProfiles("test-no-auth")
class MembersControllerIT @Autowired constructor(
    //Client for performing Pool API operations that exceed creating business partner data
    private val poolClient: PoolApiClient,
    //For creating environment to support the creation of business partner data
    private val dataHelper: PoolDataHelper,
    //Used for cleaning the database between tests
    private val dbTestHelpers: DbTestHelpers,
    //Used for performing specialized asserts on business partner data
    private val assertHelper: PoolAssertHelper
) {

    //Hold the current active test data environment for the current test
    private lateinit var testDataEnvironment: TestDataEnvironment

    @BeforeEach
    fun beforeEach() {
        //Clean the database between each test
        dbTestHelpers.truncateDbTables()
        //Create a new test data environment after cleaning the database
        //This will give access to supporting functions for creating business partner requests and expected results
        testDataEnvironment = dataHelper.createTestDataEnvironment()
    }

    @Test
    fun `only return Catena-X member legal entities`() {
        /* Test Structure
        1. Specify data to be created
        2. Create data
        3. Create expected result from specified data
        4. Get the actual result
        5. Compare actual result with the expected result
         */
        //First specify which business partners should be created
        //Use the test environment's request factory for easy creation
        val membersToCreate = listOf(
            createLegalEntity("Member 1"),
            createLegalEntity("Member 2")
        )
        val nonMembersToCreate = listOf(
            createLegalEntity("Non-Member 1", isCatenaXMemberData = false),
            createLegalEntity("Non-Member 2", isCatenaXMemberData = false)
        )
        //Use the Data Helper to create the specified business partner in the database
        //The Data Helper will also give you back the requests with resolved BPNs
        //This can be used for creating expected results
        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val createdResponse = dataHelper.createBusinessPartnerHierarchies(membersToCreate)
        //Here we use a part of the resolved requests to automatically create expected results
        val expected = createdResponse.hierarchiesWithBpns.getAllLegalEntities().map { testDataEnvironment.expectFactory.mapToExpectedLegalEntity(it) }
        //Now this is the actual method under test which gives us a result
        val actualResponse = poolClient.members.searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest()).content
        //We compare the result with the expected result we constructed above
        assertHelper.assertLegalEntityResponse(actualResponse, expected, createdResponse.creationTimeframe)
    }

    @Test
    fun `only return Catena-X member sites`() {
        val membersToCreate = listOf(
            //Use hierarchies to easily specify legal entities with sites and addresses in a quick way
            createLegalEntity("Member 1").with(createSite("Site 1")),
            createLegalEntity("Member 2").with(createSite("Site 2"))
        )

        val nonMembersToCreate = listOf(
            createLegalEntity("Member 3", false).with(createSite("Site 3")),
            createLegalEntity("Member 4", false).with(createSite("Site 4"))
        )
        //It helps to separate creating business partners by whether they are expected in the result
        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val creationResponse = dataHelper.createBusinessPartnerHierarchies(membersToCreate)

        val expected = creationResponse.hierarchiesWithBpns.flatMap { testDataEnvironment.expectFactory.mapToExpectedSites(it) }

        val actualResponse = poolClient.members.postSiteSearch(SiteSearchRequest(), PaginationRequest()).content

        assertHelper.assertSiteResponse(actualResponse, expected, creationResponse.creationTimeframe)
    }

    @Test
    fun `only return Catena-X member addresses`() {
        val membersToCreate = listOf(
            createLegalEntity("Member 1").with(createAddress("Add Address 1"))
                .with(createSite("Site 1").with(createAddress("Add Address 2")))
        )

        val nonMembersToCreate = listOf(
            createLegalEntity("Member 2", false).with(createAddress("Add Address 3"))
                .with(createSite("Site 2").with(createAddress("Add Address 4")))
        )

        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val creationResponse = dataHelper.createBusinessPartnerHierarchies(membersToCreate)

        val expected = creationResponse.hierarchiesWithBpns.flatMap { testDataEnvironment.expectFactory.mapToExpectedAddresses(it) }

        val actualResponse = poolClient.members.searchAddresses(AddressSearchRequest(), PaginationRequest()).content

        assertHelper.assertAddressResponse(actualResponse, expected, creationResponse.creationTimeframe)
    }

    //Simple shortcut functions for the test class which makes reading the business partner to create easier
    fun createLegalEntity(seed: String, isCatenaXMemberData: Boolean = true) =
        LegalEntityHierarchy(testDataEnvironment.requestFactory.createLegalEntityRequest(seed, isCatenaXMemberData))

    fun createSite(seed: String) = SiteHierarchy(testDataEnvironment.requestFactory.buildSiteCreateRequest(seed, ""))
    fun createAddress(seed: String) = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, "")
}