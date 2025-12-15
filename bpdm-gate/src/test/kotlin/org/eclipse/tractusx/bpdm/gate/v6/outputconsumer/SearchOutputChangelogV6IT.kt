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

package org.eclipse.tractusx.bpdm.gate.v6.outputconsumer

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.junit.jupiter.api.Test
import java.time.Instant

class SearchOutputChangelogV6IT: GateOutputConsumerV6Test() {

    private val anyTime = Instant.now()

    /**
     * GIVEN created output
     * WHEN output consumer searches for changelogs
     * THEN output consumer sees created changelog
     */
    @Test
    fun `find output create changelog`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        testDataClient.setStateToSuccess(testName)

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntry = ChangelogGateDto(testName, anyTime, ChangelogType.CREATE)
        val expected = PageChangeLogDto(1, 1, 0, 1, listOf(expectedEntry), 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN output updated
     * WHEN output consumer searches for changelogs
     * THEN output consumer sees updated changelog (after created)
     */
    @Test
    fun `find output update changelog`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        testDataClient.setStateToSuccess(externalId = testName)

        testDataClient.createBusinessPartnerInput(externalId = testName, seed = "Updated $testName")
        testDataClient.setStateToSuccess(externalId = testName, seed = "Updated $testName")

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(testName, anyTime, ChangelogType.CREATE),
            ChangelogGateDto(testName, anyTime, ChangelogType.UPDATE),
        )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN output with external-Id
     * WHEN output consumer searches for changelogs by external-ID
     * THEN output consumer sees only output with external-ID
     */
    @Test
    fun `find output changelogs by external-ID`(){
        //GIVEN
        val createdInput1 =   testDataClient.createBusinessPartnerInput("$testName 1")
        val createdInput2 =   testDataClient.createBusinessPartnerInput("$testName 2")
        val createdInput3 =   testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToSuccess(createdInput1.externalId)
        testDataClient.setStateToSuccess(createdInput2.externalId)
        testDataClient.setStateToSuccess(createdInput3.externalId)

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(createdInput1.externalId))
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(createdInput1.externalId, anyTime, ChangelogType.CREATE)
        )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN output created after time X
     * WHEN output consumer searches for changelogs after time X
     * THEN output consumer sees create changelog of that input
     */
    @Test
    fun `find output changelogs by timestamp after`(){
        //GIVEN
        val createdInput1 =   testDataClient.createBusinessPartnerInput("$testName 1")
        val createdInput2 =   testDataClient.createBusinessPartnerInput("$testName 2")
        val createdInput3 =   testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToSuccess(createdInput1.externalId)
        testDataClient.setStateToSuccess(createdInput2.externalId)

        val timeX = Instant.now()

        testDataClient.setStateToSuccess(createdInput3.externalId)

        //WHEN
        val request = ChangelogSearchRequest(timestampAfter = timeX)
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(createdInput3.externalId, anyTime, ChangelogType.CREATE)
        )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }
}