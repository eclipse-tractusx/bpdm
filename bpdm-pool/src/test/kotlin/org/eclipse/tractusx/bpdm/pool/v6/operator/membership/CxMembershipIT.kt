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

package org.eclipse.tractusx.bpdm.pool.v6.operator.membership

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequest
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.junit.jupiter.api.Test

class CxMembershipIT: OperatorTest() {

    /**
     * GIVEN non-member legal entity
     * WHEN operator sets legal entity membership to true
     * THEN legal entity membership is set to true
     */
    @Test
    fun `set legal entity member`(){
        //GIVEN
        val bpnL = createLegalEntity(testName, false)

        //WHEN
        val updatedMembership =  CxMembershipDto(bpnL, true)
        val updateRequest = CxMembershipUpdateRequest(listOf(updatedMembership))
        poolClient.memberships.put(updateRequest)

        //THEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(listOf(bpnL), true), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(updatedMembership))

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN member legal entity
     * WHEN operator sets legal entity membership to false
     * THEN legal entity membership is set to false
     */
    @Test
    fun `set member legal entity to non-member`(){
        //GIVEN
        val bpnL = createLegalEntity(testName, true)

        //WHEN
        val updatedMembership =  CxMembershipDto(bpnL, false)
        val updateRequest = CxMembershipUpdateRequest(listOf(updatedMembership))
        poolClient.memberships.put(updateRequest)

        //THEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(listOf(bpnL), false), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(updatedMembership))

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entities
     * WHEN operator searches for all memberships
     * THEN operator sees memberships as given
     */
    @Test
    fun `search memberships`(){
        //GIVEN
        val memberBpnlA = createLegalEntity("$testName A", true)
        val memberBpnlB = createLegalEntity("$testName B", true)
        val nonMemberBpnlC = createLegalEntity("$testName C", false)
        val nonMemberBpnlD = createLegalEntity("$testName D", false)

        //WHEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(null, null), PaginationRequest())

        //THEN
        val expectedMemberships = listOf(
            CxMembershipDto(memberBpnlA, true),
            CxMembershipDto(memberBpnlB, true),
            CxMembershipDto(nonMemberBpnlC, false),
            CxMembershipDto(nonMemberBpnlD, false),
        )
        val expectedResponse = PageDto(expectedMemberships.size.toLong(), 1, 0, expectedMemberships.size, expectedMemberships)

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entities
     * WHEN operator searches for only true memberships
     * THEN operator sees only true memberships as given
     */
    @Test
    fun `search true memberships`(){
        //GIVEN
        val memberBpnlA = createLegalEntity("$testName A", true)
        val memberBpnlB = createLegalEntity("$testName B", true)
        createLegalEntity("$testName C", false)
        createLegalEntity("$testName D", false)

        //WHEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(null, true), PaginationRequest())

        //THEN
        val expectedMemberships = listOf(
            CxMembershipDto(memberBpnlA, true),
            CxMembershipDto(memberBpnlB, true),
        )
        val expectedResponse = PageDto(expectedMemberships.size.toLong(), 1, 0, expectedMemberships.size, expectedMemberships)

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entities
     * WHEN operator searches for only non-member memberships
     * THEN operator sees only non-member memberships as given
     */
    @Test
    fun `search non-member memberships`(){
        //GIVEN
        createLegalEntity("$testName A", true)
        createLegalEntity("$testName B", true)
        val nonMemberBpnlC = createLegalEntity("$testName C", false)
        val nonMemberBpnlD = createLegalEntity("$testName D", false)

        //WHEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(null, false), PaginationRequest())

        //THEN
        val expectedMemberships = listOf(
            CxMembershipDto(nonMemberBpnlC, false),
            CxMembershipDto(nonMemberBpnlD, false),
        )
        val expectedResponse = PageDto(expectedMemberships.size.toLong(), 1, 0, expectedMemberships.size, expectedMemberships)

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entities
     * WHEN operator searches for memberships by BPNLs
     * THEN operator sees only memberships with the searched BPNLs
     */
    @Test
    fun `search memberships by BpnLs`(){
        //GIVEN
        val memberBpnlA = createLegalEntity("$testName A", true)
        createLegalEntity("$testName B", true)
        val nonMemberBpnlC = createLegalEntity("$testName C", false)
        createLegalEntity("$testName D", false)

        //WHEN
        val searchResponse = poolClient.memberships.get(CxMembershipSearchRequest(listOf(memberBpnlA, nonMemberBpnlC), null), PaginationRequest())

        //THEN
        val expectedMemberships = listOf(
            CxMembershipDto(memberBpnlA, true),
            CxMembershipDto(nonMemberBpnlC, false),
        )
        val expectedResponse = PageDto(expectedMemberships.size.toLong(), 1, 0, expectedMemberships.size, expectedMemberships)

        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }


    private fun createLegalEntity(seed: String, isMember: Boolean): String{
        val legalEntityRequest = with(testDataFactory.request.buildLegalEntityCreateRequest(seed)){
            copy(legalEntity = legalEntity.copy(isCatenaXMemberData = isMember))
        }
        return poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity.bpnl
    }
}