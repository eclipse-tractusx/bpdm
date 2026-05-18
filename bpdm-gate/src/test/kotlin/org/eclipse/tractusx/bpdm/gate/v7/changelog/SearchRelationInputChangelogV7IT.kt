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

package org.eclipse.tractusx.bpdm.gate.v7.changelog

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.junit.jupiter.api.Test
import java.time.Instant

class SearchRelationInputChangelogV7IT : UnscheduledGateTestBaseV7() {

    private val anyTime = Instant.now()

    /**
     * GIVEN created relation input
     * WHEN input consumer searches for relation input changelogs
     * THEN input consumer sees created changelog
     */
    @Test
    fun `find relation input create changelog`() {
        //GIVEN
        val relation = testDataClient.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)

        //WHEN
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(relation.externalId, anyTime, ChangelogType.CREATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN relation input updated
     * WHEN input consumer searches for relation input changelogs
     * THEN input consumer sees update changelog after create changelog
     */
    @Test
    fun `find relation input update changelog`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners(testName, RelationType.IsManagedBy)
        val updateEntry = testData.relation.input.request.fromSeed(testName).withRelationType(RelationType.IsOwnedBy)
        testDataClient.upsertRelationInput(updateEntry)

        //WHEN
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(testName, anyTime, ChangelogType.CREATE),
            ChangelogGateDto(testName, anyTime, ChangelogType.UPDATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN relation inputs with distinct external-IDs
     * WHEN input consumer searches for relation input changelogs by external-ID
     * THEN input consumer sees only changelog for that external-ID
     */
    @Test
    fun `find relation input changelogs by external-ID`() {
        //GIVEN
        val rel1 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", RelationType.IsOwnedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", RelationType.IsManagedBy)

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(rel1.externalId))
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(rel1.externalId, anyTime, ChangelogType.CREATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN relation input created after time X
     * WHEN input consumer searches for relation input changelogs after time X
     * THEN input consumer sees only the changelog entry created after time X
     */
    @Test
    fun `find relation input changelogs by timestamp after`() {
        //GIVEN
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 1", RelationType.IsManagedBy)
        testDataClient.upsertRelationInputWithBusinessPartners("$testName 2", RelationType.IsOwnedBy)

        val timeX = Instant.now()

        val rel3 = testDataClient.upsertRelationInputWithBusinessPartners("$testName 3", RelationType.IsManagedBy)

        //WHEN
        val request = ChangelogSearchRequest(timestampAfter = timeX)
        val response = gateClient.relationChangelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(rel3.externalId, anyTime, ChangelogType.CREATE)
        )
        assertRepo.assertChangelog(response, expected)
    }
}
