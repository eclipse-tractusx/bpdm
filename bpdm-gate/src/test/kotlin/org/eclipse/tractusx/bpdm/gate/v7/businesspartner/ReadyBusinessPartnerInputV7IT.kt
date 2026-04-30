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

package org.eclipse.tractusx.bpdm.gate.v7.businesspartner

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class ReadyBusinessPartnerInputV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN business partner not ready to be shared
     * WHEN input manager makes business partner ready to be shared
     * THEN input manager sees that business partner is ready
     */
    @Test
    fun `ready uploaded input for sharing`() {
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(testName)))

        //THEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(testName))

        val expectedState = SharingStateDto(externalId = testName, sharingStateType = SharingStateType.Ready, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedState))
        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner ready to be shared
     * WHEN input manager tries to make business partner ready again
     * THEN input manager sees 400 BAD REQUEST error
     */
    @Test
    fun `try ready already ready business partner`() {
        //GIVEN
        testDataClient.createBusinessPartnerInput(testName)
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(testName)))

        //WHEN THEN
        Assertions.assertThatThrownBy {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(testName)))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}
