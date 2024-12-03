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
            createMember("Member 1"),
            createMember("Member 2")
        )
        val nonMembersToCreate = listOf(
            createMemberOwned("Non-Member 1"),
            createNonMember("Non-Member 2")
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
            createMember("Member 1").with(createSite("Site 1")),
            createMember("Member 2").with(createSite("Site 2"))
        )

        val nonMembersToCreate = listOf(
            createMemberOwned("Member 3").with(createSite("Site 3")),
            createNonMember("Member 4").with(createSite("Site 4"))
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
            createMember("Member 1").with(createAddress("Add Address 1"))
                .with(createSite("Site 1").with(createAddress("Add Address 2")))
        )

        val nonMembersToCreate = listOf(
            createNonMember("Member 2").with(createAddress("Add Address 3"))
                .with(createSite("Site 2").with(createAddress("Add Address 4"))),
            createMemberOwned("Member 3").with(createAddress("Add Address 5"))
                .with(createSite("Site 3").with(createAddress("Add Address 6")))
        )

        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val creationResponse = dataHelper.createBusinessPartnerHierarchies(membersToCreate)

        val expected = creationResponse.hierarchiesWithBpns.flatMap { testDataEnvironment.expectFactory.mapToExpectedAddresses(it) }

        val actualResponse = poolClient.members.searchAddresses(AddressSearchRequest(), PaginationRequest()).content

        assertHelper.assertAddressResponse(actualResponse, expected, creationResponse.creationTimeframe)
    }

    @Test
    fun `only return Catena-X member owned legal entities`() {
        val memberOwnedToCreate = listOf(
            createMember("Member 1"),
            createMember("Member 2"),
            createMemberOwned("Member Owned 1"),
            createMemberOwned("Member Owned 2")
        )
        val nonMembersToCreate = listOf(
            createNonMember("Non-Member 1"),
            createNonMember("Non-Member 2")
        )
        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val createdResponse = dataHelper.createBusinessPartnerHierarchies(memberOwnedToCreate)
        val expected = createdResponse.hierarchiesWithBpns.getAllLegalEntities().map { testDataEnvironment.expectFactory.mapToExpectedLegalEntity(it) }

        val actualResponse = poolClient.memberOwned.searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest()).content

        assertHelper.assertLegalEntityResponse(actualResponse, expected, createdResponse.creationTimeframe)
    }

    @Test
    fun `only return Catena-X member owned sites`() {
        val memberOwnedToCreate = listOf(
            createMember("Member 1").with(createSite("Site 1")),
            createMember("Member 2").with(createSite("Site 2")),
            createMemberOwned("Member Owned 1").with(createSite("Site 3")),
            createMemberOwned("Member Owned 2").with(createSite("Site 4"))
        )

        val nonMembersToCreate = listOf(
            createNonMember("Non-Member 1").with(createSite("Site 5")),
            createNonMember("Non-Member 2").with(createSite("Site 6"))
        )
        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val creationResponse = dataHelper.createBusinessPartnerHierarchies(memberOwnedToCreate)

        val expected = creationResponse.hierarchiesWithBpns.flatMap { testDataEnvironment.expectFactory.mapToExpectedSites(it) }

        val actualResponse = poolClient.memberOwned.postSiteSearch(SiteSearchRequest(), PaginationRequest()).content

        assertHelper.assertSiteResponse(actualResponse, expected, creationResponse.creationTimeframe)
    }

    @Test
    fun `only return Catena-X member owned addresses`() {
        val membersToCreate = listOf(
            createMember("Member").with(createAddress("Add Address 1"))
                .with(createSite("Site 1").with(createAddress("Add Address 2"))),
            createMemberOwned("Member Owned").with(createAddress("Add Address 3"))
                .with(createSite("Site 2").with(createAddress("Add Address 4")))

        )

        val nonMembersToCreate = listOf(
            createNonMember("Non-Member").with(createAddress("Add Address 5"))
                .with(createSite("Site 3").with(createAddress("Add Address 6")))
        )

        dataHelper.createBusinessPartnerHierarchies(nonMembersToCreate)
        val creationResponse = dataHelper.createBusinessPartnerHierarchies(membersToCreate)

        val expected = creationResponse.hierarchiesWithBpns.flatMap { testDataEnvironment.expectFactory.mapToExpectedAddresses(it) }

        val actualResponse = poolClient.memberOwned.searchAddresses(AddressSearchRequest(), PaginationRequest()).content

        assertHelper.assertAddressResponse(actualResponse, expected, creationResponse.creationTimeframe)
    }

    //Is a Catena-X member
    private fun createMember(seed: String) = createLegalEntity(seed, isCatenaXMember = true, isSharedByOwner = true)
    //Not a member but owned by a member
    private fun createMemberOwned(seed: String) = createLegalEntity(seed, isCatenaXMember = false, isSharedByOwner = true)
    //Not a member and not owned by a member
    private fun createNonMember(seed: String) = createLegalEntity(seed, isCatenaXMember = false, isSharedByOwner = false)

    //Simple shortcut functions for the test class which makes reading the business partner to create easier
    private fun createLegalEntity(seed: String, isCatenaXMember: Boolean, isSharedByOwner: Boolean) =
        LegalEntityHierarchy(
            with(testDataEnvironment.requestFactory.createLegalEntityRequest(seed, isCatenaXMember)){
                copy(legalEntity.copy(confidenceCriteria = legalEntity.confidenceCriteria.copy(sharedByOwner = isSharedByOwner)))
            }
        )

    private fun createSite(seed: String) = SiteHierarchy(testDataEnvironment.requestFactory.createSiteRequest(seed, ""))
    private fun createAddress(seed: String) = testDataEnvironment.requestFactory.createAddressRequest(seed, "")
}