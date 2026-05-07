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
        testDataClient.createBusinessPartnerOutput(testName)

        //WHEN
        val response = gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())

        //THEN
        val expectedEntry = ChangelogGateDto(testName, anyTime, ChangelogType.CREATE)
        val expected = PageChangeLogDto(1, 1, 0, 1, listOf(expectedEntry), 0, emptyList())

        assertRepo.assertChangelog(response, expected)
    }
}