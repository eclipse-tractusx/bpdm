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
import org.eclipse.tractusx.orchestrator.api.model.CleaningReservationRequest
import org.eclipse.tractusx.orchestrator.api.model.CleaningStep
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
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

}