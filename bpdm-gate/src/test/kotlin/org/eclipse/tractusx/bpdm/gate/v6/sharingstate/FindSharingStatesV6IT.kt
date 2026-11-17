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
import org.eclipse.tractusx.bpdm.gate.api.v6.exception.BusinessPartnerSharingErrorV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.PostSharingStateReadyV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.SharingStateTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.SharingStateV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.v6.UnscheduledGateV6TestBase
import org.eclipse.tractusx.bpdm.gate.v7.util.GateTestClientProviderV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
class FindSharingStatesV6IT: UnscheduledGateV6TestBase() {

    @Autowired
    lateinit var testClientProviderV7: GateTestClientProviderV7

    /**
     * GIVEN golden and alternative-process business partners
     * WHEN input consumer searches sharing states via the v6 API
     * THEN only golden-process partners are returned
     */
    @Test
    fun `v6 only returns golden process sharing states`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput("$testName golden")
        testClientProviderV7.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
            .businessParters.upsertBusinessPartnersInput(
                listOf(BusinessPartnerInputV7Request(externalId = "$testName alternative", process = "provider"))
            )

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        org.assertj.core.api.Assertions.assertThat(actualResponse.content.map { it.externalId })
            .containsExactly("$testName golden")
    }

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
        val expectedSharingState = SharingStateV6Dto(testName, SharingStateTypeV6.Initial, null, null, null, null)
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
        val expectedSharingStates = listOf(input1, input2, input3).map { SharingStateV6Dto(it.externalId, SharingStateTypeV6.Initial, null, null, null, null) }
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
        val expectedSharingState = SharingStateV6Dto(input.externalId, SharingStateTypeV6.Initial, null, null, null, null)
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
        testDataClient.operatorClient.sharingStates.postSharingStateReady(PostSharingStateReadyV6Request(listOf(input.externalId)))

        //WHEN
        val actualResponse = gateClient.sharingStates.getSharingStates(PaginationRequest(), emptyList())

        //THEN
        val expectedSharingState = SharingStateV6Dto(input.externalId, SharingStateTypeV6.Ready, null, null, null, null)
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
        val expectedSharingState = SharingStateV6Dto(input.externalId, SharingStateTypeV6.Pending, null, null, null, createdTask.taskId)
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
        val expectedSharingState = SharingStateV6Dto(input.externalId, SharingStateTypeV6.Success, null, null, null, refinedTask.taskId)
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
        val expectedSharingState = SharingStateV6Dto(
            input.externalId,
            SharingStateTypeV6.Error,
            BusinessPartnerSharingErrorV6.NaturalPersonError,
            errorTask.processingState.errors.single().description,
            null,
            errorTask.taskId
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }
}