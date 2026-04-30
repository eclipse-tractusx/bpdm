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
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import java.time.Instant

class SearchInputChangelogV7IT : UnscheduledGateTestBaseV7() {

    private val anyTime = Instant.now()

    /**
     * GIVEN created input
     * WHEN input consumer searches for changelogs
     * THEN input consumer sees created changelog
     */
    @Test
    fun `find input create changelog`() {
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntry = ChangelogGateDto(testName, anyTime, ChangelogType.CREATE)
        val expected = PageChangeLogDto(1, 1, 0, 1, listOf(expectedEntry), 0, emptyList())
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN input updated
     * WHEN input consumer searches for changelogs
     * THEN input consumer sees update changelog after create changelog
     */
    @Test
    fun `find input update changelog`() {
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        testDataClient.createBusinessPartnerInput(
            businessPartnerInputRequestFactory.fromSeed("Updated $testName").copy(externalId = testName)
        )

        //WHEN
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(testName, anyTime, ChangelogType.CREATE),
            ChangelogGateDto(testName, anyTime, ChangelogType.UPDATE)
        )
        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN inputs with distinct external-IDs
     * WHEN input consumer searches for changelogs by external-ID
     * THEN input consumer sees only changelog for that external-ID
     */
    @Test
    fun `find input changelogs by external-ID`() {
        //GIVEN
        val createdToBeFound = testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")
        testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(createdToBeFound.externalId))
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(ChangelogGateDto(createdToBeFound.externalId, anyTime, ChangelogType.CREATE))
        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN input created after time X
     * WHEN input consumer searches for changelogs after time X
     * THEN input consumer sees only the changelog entry created after time X
     */
    @Test
    fun `find input changelogs by timestamp after`() {
        //GIVEN
        testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")

        val timeX = Instant.now()

        val createdToBeFound = testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(timestampAfter = timeX)
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(ChangelogGateDto(createdToBeFound.externalId, anyTime, ChangelogType.CREATE))
        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())
        assertRepo.assertChangelog(response, expected)
    }
}
