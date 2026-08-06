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

package org.eclipse.tractusx.bpdm.pool.v7.participation

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.DataSpaceParticipantDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.DataSpaceParticipantUpdateRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class DataSpaceParticipationV7IT : UnscheduledPoolTestBaseV7() {

    private val unknownBpnL = "BPNL0000000000XY"

    /**
     * GIVEN non-participant legal entity
     * WHEN operator sets legal entity participation to true
     * THEN legal entity participation is set to true
     */
    @Test
    fun `set legal entity as participant`() {
        //GIVEN
        val bpnL = testDataClient.createLegalEntity(testName).header.bpnl

        //WHEN
        val updatedParticipation = DataSpaceParticipantDto(bpnL, true)
        poolClient.participants.put(DataSpaceParticipantUpdateRequest(listOf(updatedParticipation)))

        //THEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnL), true), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(updatedParticipation))
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant legal entity
     * WHEN operator sets legal entity participation to false
     * THEN legal entity participation is set to false
     */
    @Test
    fun `set participant legal entity to non-participant`() {
        //GIVEN
        val bpnL = testDataClient.createParticipantLegalEntity(testName).header.bpnl

        //WHEN
        val updatedParticipation = DataSpaceParticipantDto(bpnL, false)
        poolClient.participants.put(DataSpaceParticipantUpdateRequest(listOf(updatedParticipation)))

        //THEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnL), false), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(updatedParticipation))
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator updates multiple legal entities' participation in a single request
     * THEN all updated participation values are persisted
     */
    @Test
    fun `update multiple legal entity participations at once`() {
        //GIVEN
        val bpnLA = testDataClient.createLegalEntity("$testName A").header.bpnl
        val bpnLB = testDataClient.createParticipantLegalEntity("$testName B").header.bpnl

        //WHEN
        val updatedParticipationA = DataSpaceParticipantDto(bpnLA, true)
        val updatedParticipationB = DataSpaceParticipantDto(bpnLB, false)
        poolClient.participants.put(DataSpaceParticipantUpdateRequest(listOf(updatedParticipationA, updatedParticipationB)))

        //THEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnLA, bpnLB), null), PaginationRequest())
        val expectedParticipations = listOf(updatedParticipationA, updatedParticipationB)
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator searches for all participations
     * THEN operator sees all participations as given
     */
    @Test
    fun `search participations`() {
        //GIVEN
        val bpnLA = testDataClient.createParticipantLegalEntity("$testName A").header.bpnl
        val bpnLB = testDataClient.createParticipantLegalEntity("$testName B").header.bpnl
        val bpnLC = testDataClient.createLegalEntity("$testName C").header.bpnl
        val bpnLD = testDataClient.createLegalEntity("$testName D").header.bpnl

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(null, null), PaginationRequest())

        //THEN
        val expectedParticipations = listOf(
            DataSpaceParticipantDto(bpnLA, true),
            DataSpaceParticipantDto(bpnLB, true),
            DataSpaceParticipantDto(bpnLC, false),
            DataSpaceParticipantDto(bpnLD, false),
        )
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator searches for only participant entries
     * THEN operator sees only participant entries
     */
    @Test
    fun `search participant entries`() {
        //GIVEN
        val bpnLA = testDataClient.createParticipantLegalEntity("$testName A").header.bpnl
        val bpnLB = testDataClient.createParticipantLegalEntity("$testName B").header.bpnl
        testDataClient.createLegalEntity("$testName C")
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(null, true), PaginationRequest())

        //THEN
        val expectedParticipations = listOf(
            DataSpaceParticipantDto(bpnLA, true),
            DataSpaceParticipantDto(bpnLB, true),
        )
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator searches for only non-participant entries
     * THEN operator sees only non-participant entries
     */
    @Test
    fun `search non-participant entries`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName A")
        testDataClient.createParticipantLegalEntity("$testName B")
        val bpnLC = testDataClient.createLegalEntity("$testName C").header.bpnl
        val bpnLD = testDataClient.createLegalEntity("$testName D").header.bpnl

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(null, false), PaginationRequest())

        //THEN
        val expectedParticipations = listOf(
            DataSpaceParticipantDto(bpnLC, false),
            DataSpaceParticipantDto(bpnLD, false),
        )
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator searches for participations by BPNLs
     * THEN operator sees only participations for the searched BPNLs
     */
    @Test
    fun `search participations by BPNLs`() {
        //GIVEN
        val bpnLA = testDataClient.createParticipantLegalEntity("$testName A").header.bpnl
        testDataClient.createParticipantLegalEntity("$testName B")
        val bpnLC = testDataClient.createLegalEntity("$testName C").header.bpnl
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnLA, bpnLC), null), PaginationRequest())

        //THEN
        val expectedParticipations = listOf(
            DataSpaceParticipantDto(bpnLA, true),
            DataSpaceParticipantDto(bpnLC, false),
        )
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entities
     * WHEN operator searches for participations by BPNLs filtered to participants only
     * THEN operator sees only participant entries for the searched BPNLs
     */
    @Test
    fun `search participations by BPNLs filtered to participants`() {
        //GIVEN
        val bpnLA = testDataClient.createParticipantLegalEntity("$testName A").header.bpnl
        testDataClient.createParticipantLegalEntity("$testName B")
        val bpnLC = testDataClient.createLegalEntity("$testName C").header.bpnl
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnLA, bpnLC), true), PaginationRequest())

        //THEN
        val expectedParticipations = listOf(DataSpaceParticipantDto(bpnLA, true))
        val expectedResponse = PageDto(expectedParticipations.size.toLong(), 1, 0, expectedParticipations.size, expectedParticipations)
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant legal entity
     * WHEN operator searches for participations by the BPNL in lower case
     * THEN operator sees the participation of that legal entity
     */
    @Test
    fun `search participations by lower case BPNL`() {
        //GIVEN
        val bpnL = testDataClient.createParticipantLegalEntity(testName).header.bpnl

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnL.lowercase()), null), PaginationRequest())

        //THEN
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(DataSpaceParticipantDto(bpnL, true)))
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant legal entity
     * WHEN operator searches for participations by a BPNL that is no valid BPNL
     * THEN operator sees no participations
     */
    @Test
    fun `search participations by malformed BPNL`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf("NO BPNL"), null), PaginationRequest())

        //THEN
        val expectedResponse = PageDto<DataSpaceParticipantDto>(0, 0, 0, 0, emptyList())
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity
     * WHEN operator updates its participation together with an unknown legal entity
     * THEN the update is rejected and the known legal entity keeps its participation
     */
    @Test
    fun `update participations naming an unknown legal entity`() {
        //GIVEN
        val bpnL = testDataClient.createLegalEntity(testName).header.bpnl

        //WHEN
        val updateRequest = DataSpaceParticipantUpdateRequest(
            listOf(DataSpaceParticipantDto(bpnL, true), DataSpaceParticipantDto(unknownBpnL, true))
        )
        Assertions.assertThatThrownBy { poolClient.participants.put(updateRequest) }
            .isInstanceOf(WebClientResponseException.NotFound::class.java)

        //THEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnL), null), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(DataSpaceParticipantDto(bpnL, false)))
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity
     * WHEN operator names that legal entity twice in one participation update
     * THEN the update is rejected and the legal entity keeps its participation
     */
    @Test
    fun `update participations naming a legal entity twice`() {
        //GIVEN
        val bpnL = testDataClient.createLegalEntity(testName).header.bpnl

        //WHEN
        val updateRequest = DataSpaceParticipantUpdateRequest(
            listOf(DataSpaceParticipantDto(bpnL, true), DataSpaceParticipantDto(bpnL, false))
        )
        Assertions.assertThatThrownBy { poolClient.participants.put(updateRequest) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)

        //THEN
        val searchResponse = poolClient.participants.get(DataSpaceParticipantSearchRequest(listOf(bpnL), null), PaginationRequest())
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(DataSpaceParticipantDto(bpnL, false)))
        Assertions.assertThat(searchResponse).isEqualTo(expectedResponse)
    }

    /**
     * GIVEN participant legal entity
     * WHEN operator sets its participation to the value it already holds
     * THEN no changelog entry is written for that legal entity
     */
    @Test
    fun `update participation to the value already held`() {
        //GIVEN
        val bpnL = testDataClient.createParticipantLegalEntity(testName).header.bpnl
        val changelogBefore = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(bpns = setOf(bpnL)), PaginationRequest())

        //WHEN
        poolClient.participants.put(DataSpaceParticipantUpdateRequest(listOf(DataSpaceParticipantDto(bpnL, true))))

        //THEN
        val changelogAfter = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(bpns = setOf(bpnL)), PaginationRequest())
        Assertions.assertThat(changelogAfter.content).isEqualTo(changelogBefore.content)
        Assertions.assertThat(changelogAfter.content.map { it.changelogType }).containsExactly(ChangelogType.CREATE)
    }

    /**
     * GIVEN non-participant legal entity
     * WHEN operator sets participation to true
     * THEN the legal entity's isParticipantData flag is updated accordingly
     */
    @Test
    fun `participation update reflects in legal entity isParticipantData`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val bpnL = legalEntityResponse.header.bpnl

        //WHEN
        poolClient.participants.put(DataSpaceParticipantUpdateRequest(listOf(DataSpaceParticipantDto(bpnL, true))))

        //THEN
        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        Assertions.assertThat(updatedLegalEntity.header.isParticipantData).isTrue
    }
}
