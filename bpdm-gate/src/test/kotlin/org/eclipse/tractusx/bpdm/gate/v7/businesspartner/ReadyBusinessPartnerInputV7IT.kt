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
import org.eclipse.tractusx.bpdm.gate.api.v7.model.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.request.PostSharingStateReadyV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.GateErrorV7Response
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.SharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        testDataClient.businessPartner.upsertInput(testName)

        //WHEN
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(testName)))

        //THEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(testName))

        val expectedState = SharingStateV7Dto(externalId = testName, sharingStateType = SharingStateTypeV7.Ready, updatedAt = Instant.MIN)
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
        testDataClient.businessPartner.upsertInput(testName)
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(testName)))

        //WHEN THEN
        Assertions.assertThatThrownBy {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(testName)))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN no business partner with an external-ID
     * WHEN input manager tries to make that external-ID ready to be shared
     * THEN input manager sees 400 BAD REQUEST error
     */
    @Test
    fun `try ready unknown business partner`() {
        //WHEN THEN
        Assertions.assertThatThrownBy {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(testName)))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    /**
     * GIVEN business partner ready to be shared and an unknown external-ID
     * WHEN input manager tries to make both ready to be shared
     * THEN input manager sees one error for each of them
     */
    @Test
    fun `try ready already ready and unknown business partner`() {
        //GIVEN
        val readyExternalId = testDataClient.businessPartner.upsertInput(testName).externalId
        testDataClient.businessPartner.setStateToReady(readyExternalId)
        val unknownExternalId = "$testName unknown"

        //WHEN
        val response = assertThrows<WebClientResponseException.BadRequest> {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(readyExternalId, unknownExternalId)))
        }

        //THEN
        assertRepo.assertErrorPerExternalId(response.getResponseBodyAs(GateErrorV7Response::class.java)!!, listOf(readyExternalId, unknownExternalId))
    }

    /**
     * GIVEN business partner not ready to be shared and an unknown external-ID
     * WHEN input manager tries to make both ready to be shared
     * THEN input manager sees that the known business partner is still not ready
     */
    @Test
    fun `ready nothing when any business partner can not be made ready`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput(testName)

        //WHEN
        assertThrows<WebClientResponseException.BadRequest> {
            gateClient.sharingState.postSharingStateReady(PostSharingStateReadyV7Request(listOf(testName, "$testName unknown")))
        }

        //THEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(testName))

        val expectedState = SharingStateV7Dto(externalId = testName, sharingStateType = SharingStateTypeV7.Initial, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedState))
        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }
}
