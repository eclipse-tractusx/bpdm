/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.util.BusinessPartnerTestValues
import org.eclipse.tractusx.bpdm.orchestrator.util.DummyValues
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClientResponseException


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=3"])
class CleaningTaskControllerIT @Autowired constructor(
    val orchestratorClient: OrchestrationApiClient
) {

    /**
     * Validate create cleaning task endpoint is invokable with request body and returns dummy response
     */
    @Test
    fun `request cleaning task and expect dummy response`() {
        val request = TaskCreateRequest(
            mode = TaskMode.UpdateFromSharingMember,
            businessPartners = listOf(BusinessPartnerTestValues.businessPartner1, BusinessPartnerTestValues.businessPartner2)
        )

        val expected = DummyValues.dummyResponseCreateTask

        val response = orchestratorClient.cleaningTasks.createCleaningTasks(request)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * Validate reserve cleaning task endpoint is invokable with clean and sync step and returns dummy response
     */
    @Test
    fun `request reservation for clean and sync and expect dummy response`() {
        val request = CleaningReservationRequest(
            amount = 2,
            step = CleaningStep.CleanAndSync
        )

        val expected = DummyValues.dummyCleaningReservationResponse

        val response = orchestratorClient.cleaningTasks.reserveCleaningTasks(request)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * Validate reserve cleaning task endpoint is invokable with pool sync step and returns dummy response
     */
    @Test
    fun `request reservation for pool sync and expect dummy response`() {
        val request = CleaningReservationRequest(
            amount = 2,
            step = CleaningStep.PoolSync
        )

        val expected = DummyValues.dummyPoolSyncResponse

        val response = orchestratorClient.cleaningTasks.reserveCleaningTasks(request)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * Validate reserve cleaning task endpoint is invokable with cleaning step and returns dummy response
     */
    @Test
    fun `request reservation for clean and expect dummy response`() {
        val request = CleaningReservationRequest(
            amount = 2,
            step = CleaningStep.Clean
        )

        val expected = DummyValues.dummyCleaningReservationResponse

        val response = orchestratorClient.cleaningTasks.reserveCleaningTasks(request)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * Validate post cleaning result endpoint is invokable
     */
    @Test
    fun `post cleaning result is invokable`() {
        val request = CleaningResultRequest(
            results = listOf(
                CleaningResultEntry(
                    taskId = "0",
                    result = BusinessPartnerFull(
                        generic = BusinessPartnerTestValues.businessPartner1,
                        legalEntity = BusinessPartnerTestValues.legalEntity1,
                        site = BusinessPartnerTestValues.site1,
                        address = BusinessPartnerTestValues.logisticAddress1
                    ),
                    errors = emptyList()
                ),
                CleaningResultEntry(
                    taskId = "1",
                    result = BusinessPartnerFull(
                        generic = BusinessPartnerTestValues.businessPartner2,
                        legalEntity = BusinessPartnerTestValues.legalEntity2,
                        site = BusinessPartnerTestValues.site2,
                        address = BusinessPartnerTestValues.logisticAddress2
                    ),
                    errors = emptyList()
                ),
                CleaningResultEntry(
                    taskId = "2",
                    result = null,
                    errors = listOf(
                        TaskError(type = TaskErrorType.Unspecified, "Error Description")
                    )
                ),
            )
        )

        orchestratorClient.cleaningTasks.resolveCleaningTasks(request)
    }

    /**
     * When requesting cleaning of too many business partners (over the upsert limit)
     * Then throw exception
     */
    @Test
    fun `expect exception on requesting too many cleaning tasks`() {

        //Create entries above the upsert limit of 3
        val request = TaskCreateRequest(
            mode = TaskMode.UpdateFromPool,
            businessPartners = listOf(
                BusinessPartnerTestValues.businessPartner1,
                BusinessPartnerTestValues.businessPartner1,
                BusinessPartnerTestValues.businessPartner1,
                BusinessPartnerTestValues.businessPartner1
            )
        )

        Assertions.assertThatThrownBy {
            orchestratorClient.cleaningTasks.createCleaningTasks(request)
        }.isInstanceOf(WebClientResponseException::class.java)
    }

    /**
     * When reserving too many cleaning tasks (over the upsert limit)
     * Then throw exception
     */
    @Test
    fun `expect exception on requesting too many reservations`() {

        //Create entries above the upsert limit of 3
        val request = CleaningReservationRequest(
            amount = 200,
            step = CleaningStep.CleanAndSync
        )

        Assertions.assertThatThrownBy {
            orchestratorClient.cleaningTasks.reserveCleaningTasks(request)
        }.isInstanceOf(WebClientResponseException::class.java)
    }

    /**
     * When posting too many cleaning results (over the upsert limit)
     * Then throw exception
     */
    @Test
    fun `expect exception on posting too many cleaning results`() {

        val validCleaningResultEntry = CleaningResultEntry(
            taskId = "0",
            result = null,
            errors = listOf(TaskError(type = TaskErrorType.Unspecified, description = "Description"))
        )

        //Create entries above the upsert limit of 3
        val request = CleaningResultRequest(
            results = listOf(
                validCleaningResultEntry.copy(taskId = "0"),
                validCleaningResultEntry.copy(taskId = "1"),
                validCleaningResultEntry.copy(taskId = "2"),
                validCleaningResultEntry.copy(taskId = "3"),
            )
        )

        Assertions.assertThatThrownBy {
            orchestratorClient.cleaningTasks.resolveCleaningTasks(request)
        }.isInstanceOf(WebClientResponseException::class.java)
    }

    /**
     * Search for taskId and get dummy response on the test
     */

    @Test
    fun `search cleaning task state and expect dummy response`() {

        val request = TaskStateRequest(listOf("0", "1"))


        val expected = DummyValues.dummyResponseTaskState


        val response = orchestratorClient.cleaningTasks.searchCleaningTaskState(request)

        // Assert that the response matches the expected value
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * When posting cleaning result without business partner data and no errors
     * Then throw exception
     */
    @Test
    fun `expect exception on posting empty cleaning result`() {
        val request = CleaningResultRequest(
            results = listOf(
                CleaningResultEntry(
                    taskId = "0",
                    result = null,
                    errors = emptyList()
                )
            )
        )

        Assertions.assertThatThrownBy {
            orchestratorClient.cleaningTasks.resolveCleaningTasks(request)
        }.isInstanceOf(WebClientResponseException::class.java)
    }


}