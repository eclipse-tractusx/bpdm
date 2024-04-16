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
    @Test
    fun `POST Golden Record Task`() {
        val payload = TaskCreateRequest(TaskMode.UpdateFromSharingMember, listOf())
        authAssertions.assert(orchAuthExpectations.tasks.postTask) { orchestratorClient.goldenRecordTasks.createTasks(payload) }
    }

    @Test
    fun `POST Task State Search`() {
        val payload = TaskStateRequest(listOf())
        authAssertions.assert(orchAuthExpectations.tasks.postStateSearch) { orchestratorClient.goldenRecordTasks.searchTaskStates(payload) }
    }

    @Test
    fun `POST Task Step 'Clean' Reservation`() {
        val payload = TaskStepReservationRequest(step = TaskStep.Clean)
        authAssertions.assert(orchAuthExpectations.stepClean.postReservation) { orchestratorClient.goldenRecordTasks.reserveTasksForStep(payload) }
    }

    @Test
    fun `POST Task Step 'Clean' Result`() {
        val payload = TaskStepResultRequest(TaskStep.Clean, listOf())
        authAssertions.assert(orchAuthExpectations.stepClean.postResult) { orchestratorClient.goldenRecordTasks.resolveStepResults(payload) }
    }

    @Test
    fun `POST Task Step 'CleanAndSync' Reservation`() {
        val payload = TaskStepReservationRequest(step = TaskStep.CleanAndSync)
        authAssertions.assert(orchAuthExpectations.stepCleanAndSync.postReservation) { orchestratorClient.goldenRecordTasks.reserveTasksForStep(payload) }
    }

    @Test
    fun `POST Task Step 'CleanAndSync' Result`() {
        val payload = TaskStepResultRequest(TaskStep.CleanAndSync, listOf())
        authAssertions.assert(orchAuthExpectations.stepCleanAndSync.postResult) { orchestratorClient.goldenRecordTasks.resolveStepResults(payload) }
    }

    @Test
    fun `POST Task Step 'Pool' Reservation`() {
        val payload = TaskStepReservationRequest(step = TaskStep.PoolSync)
        authAssertions.assert(orchAuthExpectations.stepPoolSync.postReservation) { orchestratorClient.goldenRecordTasks.reserveTasksForStep(payload) }
    }

    @Test
    fun `POST Task Step 'Pool' Result`() {
        val payload = TaskStepResultRequest(TaskStep.PoolSync, listOf())
        authAssertions.assert(orchAuthExpectations.stepPoolSync.postResult) { orchestratorClient.goldenRecordTasks.resolveStepResults(payload) }
    }

    data class OrchestratorAuthExpectations(
        val tasks: TaskAuthExpectations,
        val stepClean: TaskStepAuthExpectations,
        val stepCleanAndSync: TaskStepAuthExpectations,
        val stepPoolSync: TaskStepAuthExpectations
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





