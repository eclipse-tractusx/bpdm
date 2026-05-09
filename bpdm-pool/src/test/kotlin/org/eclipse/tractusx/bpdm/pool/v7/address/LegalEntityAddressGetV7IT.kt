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

package org.eclipse.tractusx.bpdm.pool.v7.address

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class LegalEntityAddressGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN addresses directly belonging to legal entity
     * WHEN sharing member fetches all direct addresses of legal entity
     * THEN sharing member sees all direct addresses of that legal entity
     */
    @Test
    fun `fetch all direct addresses of a legal entity`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createParticipantLegalEntity("$testName A")
        testDataClient.createParticipantLegalEntity("$testName B")

        val siteResponse = testDataClient.createSite(legalEntityResponseA, testName)

        val additionalAddress = testDataClient.createAdditionalAddress(legalEntityResponseA, "Additional Address $testName")
        testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val fetchResponse = poolClient.legalEntities.getAddresses(legalEntityResponseA.header.bpnl, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponse(legalEntityResponseA),
            resultFactory.buildAddressSearchResponse(additionalAddress)
        )
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(fetchResponse, expectedResponse)
    }

    /**
     * GIVEN addresses directly belonging to legal entity
     * WHEN sharing member fetches page of direct addresses of legal entity
     * THEN sharing member sees only a page of direct addresses of that legal entity
     */
    @Test
    fun `fetch direct address page of a legal entity`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createParticipantLegalEntity("$testName A")
        testDataClient.createParticipantLegalEntity("$testName B")

        val siteResponse = testDataClient.createSite(legalEntityResponseA, testName)

        testDataClient.createAdditionalAddress(legalEntityResponseA, "Additional Address $testName")
        testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val fetchResponse = poolClient.legalEntities.getAddresses(legalEntityResponseA.header.bpnl, PaginationRequest(0, 1))

        //THEN
        val expectedAddresses = listOf(resultFactory.buildAddressSearchResponse(legalEntityResponseA))
        val expectedResponse = PageDto(2, 2, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(fetchResponse, expectedResponse)
    }

    /**
     * WHEN sharing member tries fetching direct addresses of unknown legal entity
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try fetch direct address of unknown legal entity`() {
        //WHEN
        val unknownGet = { poolClient.legalEntities.getAddresses("UNKNOWN", PaginationRequest()); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }
}
