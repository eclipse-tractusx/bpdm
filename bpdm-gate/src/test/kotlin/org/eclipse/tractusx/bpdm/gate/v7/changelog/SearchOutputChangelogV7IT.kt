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
import org.eclipse.tractusx.bpdm.gate.api.exception.ChangeLogOutputError
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import java.time.Instant

class SearchOutputChangelogV7IT: UnscheduledGateTestBaseV7() {

    private val anyTime = Instant.now()

    /**
     * GIVEN created output
     * WHEN output consumer searches for changelogs
     * THEN output consumer sees created changelog
     */
    @Test
    fun `find output create changelog`(){
        //GIVEN
        testDataClient.businessPartner.createOutput(testName)

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntry = ChangelogGateDto(testName, anyTime, ChangelogType.CREATE)
        val expected = testData.changelog.ofOnePage(expectedEntry)

        assertRepo.assertChangelog(response, expected)
    }


    /**
     * GIVEN updated output
     * WHEN output consumer searches for changelogs
     * THEN output consumer sees updated changelog
     */
    @Test
    fun `find output update changelog`(){
        //GIVEN
        val output = testDataClient.businessPartner.createOutput(testName)

        testDataClient.businessPartner.updateOutput(output, "Updated $testName")

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(testName, anyTime, ChangelogType.CREATE),
            ChangelogGateDto(testName, anyTime, ChangelogType.UPDATE)
        )

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN outputs with distinct external-IDs
     * WHEN output consumer searches for changelogs by external-ID
     * THEN output consumer sees only changelog for that external-ID
     */
    @Test
    fun `find output changelogs by external-ID`() {
        //GIVEN
        val outputToBeFound = testDataClient.businessPartner.createOutput("$testName 1")
        testDataClient.businessPartner.createOutput("$testName 2")
        testDataClient.businessPartner.createOutput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(outputToBeFound.externalId))
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(outputToBeFound.externalId, anyTime, ChangelogType.CREATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN output created after time X
     * WHEN output consumer searches for changelogs after time X
     * THEN output consumer sees only the changelog entry created after time X
     */
    @Test
    fun `find output changelogs by timestamp after`() {
        //GIVEN
        testDataClient.businessPartner.createOutput("$testName 1")
        testDataClient.businessPartner.createOutput("$testName 2")

        val timeX = Instant.now()

        val outputAfterTimeX = testDataClient.businessPartner.createOutput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(timestampAfter = timeX)
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(outputAfterTimeX.externalId, anyTime, ChangelogType.CREATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN output A created before time X, then updated after time X, and output B created after time X
     * WHEN output consumer searches for changelogs by external-ID of A and after time X
     * THEN output consumer sees only the update changelog entry for A
     */
    @Test
    fun `find output changelogs combining external-ID and timestamp filters`() {
        //GIVEN
        val outputA = testDataClient.businessPartner.createOutput("$testName A")

        val timeX = Instant.now()

        testDataClient.businessPartner.updateOutput(outputA, "Updated $testName A")
        testDataClient.businessPartner.createOutput("$testName B")

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(outputA.externalId), timestampAfter = timeX)
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expected = testData.changelog.ofOnePage(
            ChangelogGateDto(outputA.externalId, anyTime, ChangelogType.UPDATE)
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN no changelog entry exists for a given external-ID
     * WHEN output consumer searches for changelogs by that external-ID
     * THEN output consumer gets an empty page with an error entry for the unknown external-ID
     */
    @Test
    fun `return error for non-existing external-ID in output changelog`() {
        //GIVEN
        testDataClient.businessPartner.createOutput(testName)
        val unknownExternalId = "non-existing-external-id"

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(unknownExternalId))
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expected = PageChangeLogDto(
            totalElements = 0L,
            totalPages = 0,
            page = 0,
            contentSize = 0,
            content = emptyList<ChangelogGateDto>(),
            invalidEntries = 1,
            errors = listOf(ErrorInfo(ChangeLogOutputError.ExternalIdNotFound, "$unknownExternalId not found", unknownExternalId))
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN multiple output changelog entries
     * WHEN output consumer requests the first page with a page size smaller than the total
     * THEN output consumer sees only the entries for that page and correct pagination metadata
     */
    @Test
    fun `paginate output changelogs - first page`() {
        //GIVEN
        testDataClient.businessPartner.createOutput("$testName 1")
        testDataClient.businessPartner.createOutput("$testName 2")
        testDataClient.businessPartner.createOutput("$testName 3")

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(page = 0, size = 2), ChangelogSearchRequest())

        //THEN
        val expected = PageChangeLogDto(
            totalElements = 3L,
            totalPages = 2,
            page = 0,
            contentSize = 2,
            content = listOf(
                ChangelogGateDto("$testName 1", anyTime, ChangelogType.CREATE),
                ChangelogGateDto("$testName 2", anyTime, ChangelogType.CREATE)
            ),
            invalidEntries = 0,
            errors = emptyList<ErrorInfo<ChangeLogOutputError>>()
        )
        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN multiple output changelog entries
     * WHEN output consumer requests the second page with a page size smaller than the total
     * THEN output consumer sees the remaining entries and correct pagination metadata
     */
    @Test
    fun `paginate output changelogs - second page`() {
        //GIVEN
        testDataClient.businessPartner.createOutput("$testName 1")
        testDataClient.businessPartner.createOutput("$testName 2")
        testDataClient.businessPartner.createOutput("$testName 3")

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(page = 1, size = 2), ChangelogSearchRequest())

        //THEN
        val expected = PageChangeLogDto(
            totalElements = 3L,
            totalPages = 2,
            page = 1,
            contentSize = 1,
            content = listOf(
                ChangelogGateDto("$testName 3", anyTime, ChangelogType.CREATE)
            ),
            invalidEntries = 0,
            errors = emptyList<ErrorInfo<ChangeLogOutputError>>()
        )
        assertRepo.assertChangelog(response, expected)
    }
}