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

package org.eclipse.tractusx.bpdm.gate.v6.sharingstate

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.v6.GateUnscheduledInitialStartV6Test
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.junit.jupiter.api.Test

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
class FindSharingStatesV6IT: GateUnscheduledInitialStartV6Test() {


    /**
     * GIVEN business partner with external-ID
     * WHEN input consumer searches for sharing state by external ID
     * THEN input consumer sees business partner sharing state with given external-ID
     */
    @Test
    fun `find sharing state by external id`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput("IGNORED 1 $testName")
        testDataClient.createBusinessPartnerInput("IGNORED 2 $testName")
        testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), listOf(testName))

        //THEN
        val expectedSharingState = SharingStateDto(testName, SharingStateType.Initial, null, null, null, null)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partners
     * WHEN input consumer gets sharing states
     * THEN input consumer sees sharing states of business partners
     */
    @Test
    fun `find sharing states`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("1 $testName")
        val input2 = testDataClient.createBusinessPartnerInput("2 $testName")
        val input3 = testDataClient.createBusinessPartnerInput("3 $testName" )

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), listOf(input1.externalId, input2.externalId, input3.externalId))

        //THEN
        val expectedSharingStates = listOf(input1, input2, input3).map { SharingStateDto(it.externalId, SharingStateType.Initial, null, null, null, null) }
        val expectedResponse = PageDto(3, 1, 0, 3, expectedSharingStates)

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in initial state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find initial sharing state`(){
        //GIVEN
        val input = testDataClient.createBusinessPartnerInput(testName)

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateDto(input.externalId, SharingStateType.Initial, null, null, null, null)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in ready state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find ready sharing state`(){
        //GIVEN
        val input = testDataClient.createBusinessPartnerInput(testName)
        testDataClient.operatorClient.sharingStates.postSharingStateReady(PostSharingStateReadyRequest(listOf(input.externalId)))

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateDto(input.externalId, SharingStateType.Ready, null, null, null, null)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in pending state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find pending sharing state`(){
        //GIVEN
        val input = testDataClient.createBusinessPartnerInput(testName)
        val createdTask = testDataClient.setStateToPending(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateDto(input.externalId, SharingStateType.Pending, null, null, null, createdTask.taskId)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in success state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find success sharing state`(){
        //GIVEN
        val input = testDataClient.createBusinessPartnerInput(testName)
        val refinedTask = testDataClient.setStateToSuccess(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateDto(input.externalId, SharingStateType.Success, null, null, null, refinedTask.taskId)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in error state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find error sharing state`(){
        //GIVEN
        val input = testDataClient.createBusinessPartnerInput(testName)
        val errorTask = testDataClient.setStateToError(input.externalId, errorType =  TaskErrorType.NaturalPersonError)

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateDto(
            input.externalId,
            SharingStateType.Error,
            BusinessPartnerSharingError.NaturalPersonError,
            errorTask.processingState.errors.single().description,
            null,
            errorTask.taskId
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }
}