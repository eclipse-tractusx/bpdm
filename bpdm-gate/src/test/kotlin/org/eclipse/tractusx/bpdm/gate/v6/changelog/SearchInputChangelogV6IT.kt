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

package org.eclipse.tractusx.bpdm.gate.v6.changelog

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.v6.GateUnscheduledInitialStartV6Test
import org.junit.jupiter.api.Test
import java.time.Instant

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
class SearchInputChangelogV6IT: GateUnscheduledInitialStartV6Test() {

    private val anyTime = Instant.now()

    /**
     * GIVEN created input
     * WHEN input consumer searches for changelogs
     * THEN input consumer sees created changelog
     */
    @Test
    fun `find input create changelog`(){
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
     * THEN input consumer sees updated changelog (after created)
     */
    @Test
    fun `find input update changelog`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        testDataClient.createBusinessPartnerInput(externalId = testName, seed = "Updated $testName")

        //WHEN
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(testName, anyTime, ChangelogType.CREATE),
            ChangelogGateDto(testName, anyTime, ChangelogType.UPDATE),
             )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN input with external-Id
     * WHEN input consumer searches for changelogs by external-ID
     * THEN input consumer sees only input with external-ID
     */
    @Test
    fun `find input changelogs by external-ID`(){
        //GIVEN
        val createdInputToBeFound = testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")
        testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(externalIds = setOf(createdInputToBeFound.externalId))
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(createdInputToBeFound.externalId, anyTime, ChangelogType.CREATE)
        )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }

    /**
     * GIVEN input created after time X
     * WHEN input consumer searches for changelogs after time X
     * THEN input consumer sees create changelog of that input
     */
    @Test
    fun `find input changelogs by timestamp after`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")

        val timeX = Instant.now()

        val createdInputToBeFound = testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val request = ChangelogSearchRequest(timestampAfter = timeX)
        val response = gateClient.changelog.getInputChangelog(PaginationRequest(), request)

        //THEN
        val expectedEntries = listOf(
            ChangelogGateDto(createdInputToBeFound.externalId, anyTime, ChangelogType.CREATE)
        )

        val expected = PageChangeLogDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries, 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }


}