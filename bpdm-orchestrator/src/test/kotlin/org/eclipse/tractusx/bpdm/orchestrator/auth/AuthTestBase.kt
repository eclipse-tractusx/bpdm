/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.auth

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test

abstract class AuthTestBase(
    private val orchestratorClient: OrchestrationApiClient,
    private val authAssertions: AuthAssertionHelper,
    private val orchAuthExpectations: OrchestratorAuthExpectations
) {

    private val originId = "test-origin"
    @Test
    fun `POST Golden Record Task`() {
        val payload = TaskCreateRequest(TaskMode.entries.first(), listOf(), originId)
        authAssertions.assert(orchAuthExpectations.tasks.postTask) { orchestratorClient.goldenRecordTasks.createTasks(payload) }
    }

    @Test
    fun `POST Task State Search`() {
        val payload = TaskStateRequest(listOf())
        authAssertions.assert(orchAuthExpectations.tasks.postStateSearch) { orchestratorClient.goldenRecordTasks.searchTaskStates(payload) }
    }

    @Test
    fun `POST Task Step Reservation`() {
        TaskStep.entries.forEach { step ->
            val authExpectations = orchAuthExpectations.steps[step]
            assertThat(authExpectations).isNotNull

            val payload = TaskStepReservationRequest(step = step)
            authAssertions.assert(authExpectations!!.postReservation) { orchestratorClient.goldenRecordTasks.reserveTasksForStep(payload) }
        }
     }

    @Test
    fun `POST Task Step Result`() {
        TaskStep.entries.forEach { step ->
            val authExpectations = orchAuthExpectations.steps[step]
            assertThat(authExpectations).isNotNull

            val payload = TaskStepResultRequest(step, listOf())
            authAssertions.assert(authExpectations!!.postResult) { orchestratorClient.goldenRecordTasks.resolveStepResults(payload) }
        }
    }

    data class OrchestratorAuthExpectations(
        val tasks: TaskAuthExpectations,
        val steps: Map<TaskStep, TaskStepAuthExpectations>
    )

    data class TaskAuthExpectations(
        val postTask: AuthExpectationType,
        val postStateSearch: AuthExpectationType,
    )

    data class TaskStepAuthExpectations(
        val postReservation: AuthExpectationType,
        val postResult: AuthExpectationType
    )
}





