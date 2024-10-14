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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class CxMembershipControllerIT @Autowired constructor(
    private val poolClient: PoolApiClient,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers
) {
    private lateinit var testDataEnvironment: TestDataEnvironment

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
    }

    @Test
    fun `get cx memberships without filter`(){
        val legalEntityRequests = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )
        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntityRequests).entities
        val bpnLsToFind = getBpnLs(legalEntityRequests, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(), PaginationRequest(size = legalEntityRequests.size)).content

        val expectedMemberships = legalEntityRequests.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `get cx memberships paginated`(){
        val legalEntityRequests = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )
        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntityRequests).entities
        val bpnLsToFind = getBpnLs(legalEntityRequests, givenLegalEntities)

        val actualMembershipsPage1 = poolClient.memberships.get(CxMembershipSearchRequest(), PaginationRequest(size = 2)).content
        val actualMembershipsPage2 = poolClient.memberships.get(CxMembershipSearchRequest(), PaginationRequest(page = 1, size = 2)).content


        val expectedMembershipsTotal =  legalEntityRequests.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }

        val expectedMembershipsPage1 = expectedMembershipsTotal.take(2)
        val expectedMembershipsPage2 = expectedMembershipsTotal.drop(2).take(2)

        assertThat(actualMembershipsPage1).isEqualTo(expectedMembershipsPage1)
        assertThat(actualMembershipsPage2).isEqualTo(expectedMembershipsPage2)
    }

    @Test
    fun `get cx memberships with BPNL filter`(){
        val legalEntitiesToFind = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false)
        )

        val legalEntitiesToIgnore = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )

        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntitiesToFind + legalEntitiesToIgnore).entities
        val bpnLsToFind = getBpnLs(legalEntitiesToFind, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(bpnLsToFind), PaginationRequest()).content

        val expectedMemberships = legalEntitiesToFind.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `get cx memberships with membership true filter`(){
        val legalEntitiesToFind = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
        )

        val legalEntitiesToIgnore = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )

        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntitiesToFind + legalEntitiesToIgnore).entities
        val bpnLsToFind = getBpnLs(legalEntitiesToFind, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(isCatenaXMember = true), PaginationRequest()).content

        val expectedMemberships = legalEntitiesToFind.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `get cx memberships with membership false filter`(){

        val legalEntitiesToFind = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )

        val legalEntitiesToIgnore = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
        )

        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntitiesToFind + legalEntitiesToIgnore).entities
        val bpnLsToFind = getBpnLs(legalEntitiesToFind, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(isCatenaXMember = false), PaginationRequest()).content

        val expectedMemberships = legalEntitiesToFind.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `get cx memberships with BPNLs and membership true filter`(){
        val legalEntitiesToFind = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
        )

        val legalEntitiesToIgnore = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )

        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntitiesToFind + legalEntitiesToIgnore).entities
        val bpnLsToFind = getBpnLs(legalEntitiesToFind, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(bpnLs = bpnLsToFind, isCatenaXMember = true), PaginationRequest()).content

        val expectedMemberships = legalEntitiesToFind.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `get cx memberships with BPNLs and membership false filter`(){
        val legalEntitiesToFind = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
        )

        val legalEntitiesToIgnore = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )

        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntitiesToFind + legalEntitiesToIgnore).entities
        val bpnLsToFind = getBpnLs(legalEntitiesToFind, givenLegalEntities)

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(bpnLs = bpnLsToFind, isCatenaXMember = false), PaginationRequest()).content

        val expectedMemberships = legalEntitiesToFind.zip(bpnLsToFind)
            .map { (request, bpnL) -> CxMembershipDto(bpnL, request.legalEntity.isCatenaXMemberData) }


        assertThat(actualMemberships).isEqualTo(expectedMemberships)
    }

    @Test
    fun `update cx memberships`(){
        val legalEntityRequests = listOf(
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 1", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 1", isCatenaXMemberData = false),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Member 2", isCatenaXMemberData = true),
            testDataEnvironment.requestFactory.createLegalEntityRequest("Non-Member 2", isCatenaXMemberData = false),
        )
        val givenLegalEntities = poolClient.legalEntities.createBusinessPartners(legalEntityRequests).entities
        val givenBpnLs = getBpnLs(legalEntityRequests, givenLegalEntities)

        val membershipUpdates = legalEntityRequests.zip(givenBpnLs).map { (request, bpnL) -> CxMembershipDto(bpnL, !request.legalEntity.isCatenaXMemberData) }
        poolClient.memberships.put(CxMembershipUpdateRequest(membershipUpdates))

        val actualMemberships = poolClient.memberships.get(CxMembershipSearchRequest(), PaginationRequest()).content

        assertThat(actualMemberships).isEqualTo(membershipUpdates)
    }


    /**
     * Searches for BPNLs for the given [requests] by index in the overall [givenLegalEntities]
     * Orders the returned BPNLs by the order given in the [requests]
     */
    private fun getBpnLs(requests: List<LegalEntityPartnerCreateRequest>, givenLegalEntities: Collection<LegalEntityPartnerCreateVerboseDto>): List<String>{
        return givenLegalEntities
            .associate { Pair(it.index, it.legalEntity.bpnl) }
            .let { bpnLsByIndex ->  requests.map { bpnLsByIndex[it.index]!! } }
    }


}