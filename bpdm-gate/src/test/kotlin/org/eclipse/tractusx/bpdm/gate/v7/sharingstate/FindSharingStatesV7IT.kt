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

package org.eclipse.tractusx.bpdm.gate.v7.sharingstate

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.exception.BusinessPartnerSharingErrorV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.junit.jupiter.api.Test
import java.time.Instant

class FindSharingStatesV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN business partner with external-ID
     * WHEN input consumer searches for sharing state by external ID
     * THEN input consumer sees business partner sharing state with given external-ID
     */
    @Test
    fun `find sharing state by external id`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput("IGNORED 1 $testName")
        testDataClient.businessPartner.upsertInput("IGNORED 2 $testName")
        testDataClient.businessPartner.upsertInput(testName)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(testName))

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = testName, sharingStateType = SharingStateTypeV7.Initial, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partners
     * WHEN input consumer gets sharing states
     * THEN input consumer sees sharing states of business partners
     */
    @Test
    fun `find sharing states`() {
        //GIVEN
        val input1 = testDataClient.businessPartner.upsertInput("1 $testName")
        val input2 = testDataClient.businessPartner.upsertInput("2 $testName")
        val input3 = testDataClient.businessPartner.upsertInput("3 $testName")

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input1.externalId, input2.externalId, input3.externalId))

        //THEN
        val expectedSharingStates = listOf(input1, input2, input3).map {
            SharingStateV7Dto(externalId = it.externalId, sharingStateType = SharingStateTypeV7.Initial, updatedAt = Instant.MIN)
        }
        val expectedResponse = PageDto(3, 1, 0, 3, expectedSharingStates)

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in initial state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find initial sharing state`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Initial, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in ready state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find ready sharing state`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        testDataClient.businessPartner.setStateToReady(input.externalId)
        val stamped = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId))
            .content.single().updatedAt

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Ready, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
        assertRepo.assertSharingStateTimestamp(actualResponse.content.single(), stamped)
        assertRepo.assertSharingStates(
            gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(input.externalId), updatedAfter = stamped),
            PageDto(0, 0, 0, 0, emptyList())
        )
    }

    /**
     * GIVEN business partner in pending state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find pending sharing state`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val createdTask = testDataClient.businessPartner.setStateToPending(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Pending, taskId = createdTask.taskId, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in success state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find success sharing state`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val refinedTask = testDataClient.businessPartner.setStateToSuccess(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Success, taskId = refinedTask.taskId, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner in error state
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state
     */
    @Test
    fun `find error sharing state`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val errorTask = testDataClient.businessPartner.setStateToError(input.externalId, errorType = TaskErrorType.NaturalPersonError)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(
            externalId = input.externalId,
            sharingStateType = SharingStateTypeV7.Error,
            sharingErrorCode = BusinessPartnerSharingErrorV7.NaturalPersonError,
            sharingErrorMessage = errorTask.processingState.errors.single().description,
            taskId = errorTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner whose finished task the Orchestrator no longer knows
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state failed with a missing task
     */
    @Test
    fun `find sharing state of missing task`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val missingTask = testDataClient.businessPartner.setStateToTaskMissing(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(
            externalId = input.externalId,
            sharingStateType = SharingStateTypeV7.Error,
            sharingErrorCode = BusinessPartnerSharingErrorV7.MissingTaskID,
            sharingErrorMessage = "Missing Task in Orchestrator",
            taskId = missingTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner whose task result lacks the country of its address
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state failed naming the missing value
     */
    @Test
    fun `find sharing state of task result without country`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val incompleteTask = testDataClient.businessPartner.setStateToResultWithoutCountry(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(
            externalId = input.externalId,
            sharingStateType = SharingStateTypeV7.Error,
            sharingErrorCode = BusinessPartnerSharingErrorV7.SharingProcessError,
            sharingErrorMessage = "Task result lacks mandatory values: physicalPostalAddress.country",
            taskId = incompleteTask.taskId,
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner whose pending task the Orchestrator lost
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state failed with a missing task
     */
    @Test
    fun `find sharing state of task lost by Orchestrator`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        testDataClient.businessPartner.setStateToTaskLost(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(
            externalId = input.externalId,
            sharingStateType = SharingStateTypeV7.Error,
            sharingErrorCode = BusinessPartnerSharingErrorV7.MissingTaskID,
            sharingErrorMessage = "Missing Task in Orchestrator",
            updatedAt = Instant.MIN
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partners whose pending tasks the Orchestrator lost, more than the health check covers in one page
     * WHEN input consumer searches sharing states
     * THEN input consumer sees all of those sharing states failed with a missing task
     */
    @Test
    fun `find sharing states of tasks lost by Orchestrator across health check pages`() {
        //GIVEN
        val inputs = listOf("1 $testName", "2 $testName", "3 $testName").map { testDataClient.businessPartner.upsertInput(it) }
        testDataClient.businessPartner.setStatesToTaskLost(inputs.map { it.externalId })

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), inputs.map { it.externalId })

        //THEN
        val expectedSharingStates = inputs.map {
            SharingStateV7Dto(
                externalId = it.externalId,
                sharingStateType = SharingStateTypeV7.Error,
                sharingErrorCode = BusinessPartnerSharingErrorV7.MissingTaskID,
                sharingErrorMessage = "Missing Task in Orchestrator",
                updatedAt = Instant.MIN
            )
        }
        val expectedResponse = PageDto(3, 1, 0, 3, expectedSharingStates)

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN business partner whose pending task the Orchestrator still knows
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state still pending
     */
    @Test
    fun `find pending sharing state after health check`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        testDataClient.businessPartner.setStateToPendingAfterHealthCheck(input.externalId)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Pending, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN shared business partner whose golden records the Pool deleted
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state failed with the golden records missing in the Pool
     */
    @Test
    fun `find sharing state of golden record lost by Pool`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.setStateToGoldenRecordLost(input)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(goldenRecordLostSharingState(input.externalId, goldenRecord)))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN shared business partners whose golden records the Pool deleted, more than the consistency check covers in one page
     * WHEN input consumer searches sharing states
     * THEN input consumer sees all of those sharing states failed with the golden records missing in the Pool
     */
    @Test
    fun `find sharing states of golden records lost by Pool across pages`() {
        //GIVEN
        val inputs = listOf("1 $testName", "2 $testName", "3 $testName").map { testDataClient.businessPartner.upsertInput(it) }
        val goldenRecords = testDataClient.businessPartner.setStatesToGoldenRecordLost(inputs)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), inputs.map { it.externalId })

        //THEN
        val expectedSharingStates = inputs.zip(goldenRecords) { input, goldenRecord -> goldenRecordLostSharingState(input.externalId, goldenRecord) }
        val expectedResponse = PageDto(3, 1, 0, 3, expectedSharingStates)

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    /**
     * GIVEN shared business partner whose golden records the Pool still knows
     * WHEN input consumer searches sharing states
     * THEN input consumer sees that sharing state still successful
     */
    @Test
    fun `find successful sharing state after consistency check`() {
        //GIVEN
        val input = testDataClient.businessPartner.upsertInput(testName)
        testDataClient.businessPartner.setStateToSuccessAfterConsistencyCheck(input)

        //WHEN
        val actualResponse = gateClient.sharingState.getSharingStates(PaginationRequest(), null)

        //THEN
        val expectedSharingState = SharingStateV7Dto(externalId = input.externalId, sharingStateType = SharingStateTypeV7.Success, updatedAt = Instant.MIN)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSharingState))

        assertRepo.assertSharingStates(actualResponse, expectedResponse)
    }

    private fun goldenRecordLostSharingState(externalId: String, goldenRecord: LegalEntityWithLegalAddressVerboseV7Dto) =
        SharingStateV7Dto(
            externalId = externalId,
            sharingStateType = SharingStateTypeV7.Error,
            sharingErrorCode = BusinessPartnerSharingErrorV7.BpnNotInPool,
            sharingErrorMessage = "Business partner with external-ID $externalId references not existing golden record '${goldenRecord.header.bpnl}, ${goldenRecord.legalAddress.bpna}'",
            updatedAt = Instant.MIN
        )
}
