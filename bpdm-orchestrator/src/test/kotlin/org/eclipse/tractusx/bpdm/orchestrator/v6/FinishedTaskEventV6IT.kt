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

package org.eclipse.tractusx.bpdm.orchestrator.v6

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.v6.model.FinishedTaskEventsResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ResultStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6
import org.junit.jupiter.api.Test
import java.time.Instant

class FinishedTaskEventV6IT: UnscheduledOrchestratorTestBaseV6() {

    /**
     * GIVEN task has been finished successfully
     * WHEN user searches for new events
     * THEN user sees event for finished task
     */
    @Test
    fun `get successfully finished task event`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName, TaskMode.UpdateFromPool)
        testDataClient.resolveTask(createdTask.taskId, TaskStepV6.Clean, testName)

        //WHEN
        val response = orchestratorClient.finishedTaskEvents.getEvents(createdTask.processingState.createdAt, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsResponseV6(1, 1, 0, 1, listOf(
            FinishedTaskEventsResponseV6.Event(Instant.now(), ResultStateV6.Success, createdTask.taskId)
        ))

        assertRepository.assertFinishedTasksResponse(response, expectedResponse)
    }

    /**
     * GIVEN task has failed with an error
     * WHEN user searches for new events
     * THEN user sees event for finished task
     */
    @Test
    fun `get failed finished task event`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName, TaskMode.UpdateFromPool)
        testDataClient.failTask(createdTask.taskId, TaskStepV6.Clean)

        //WHEN
        val response = orchestratorClient.finishedTaskEvents.getEvents(createdTask.processingState.createdAt, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsResponseV6(1, 1, 0, 1, listOf(
            FinishedTaskEventsResponseV6.Event(Instant.now(), ResultStateV6.Error, createdTask.taskId)
        ))

        assertRepository.assertFinishedTasksResponse(response, expectedResponse)
    }

    /**
     * GIVEN several tasks finished and one task finished after time X
     * WHEN user searches for new events after time X
     * THEN user sees event for that finished task
     */
    @Test
    fun `get task event after time`(){
        //GIVEN
        val createdTask1 = testDataClient.createTask("$testName 1", TaskMode.UpdateFromPool)
        val createdTask2 = testDataClient.createTask("$testName 1", TaskMode.UpdateFromPool)

        testDataClient.failTask(createdTask1.taskId, TaskStepV6.Clean)
        val timeX = Instant.now()
        testDataClient.failTask(createdTask2.taskId, TaskStepV6.Clean)

        //WHEN
        val response = orchestratorClient.finishedTaskEvents.getEvents(timeX, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsResponseV6(1, 1, 0, 1, listOf(
            FinishedTaskEventsResponseV6.Event(Instant.now(), ResultStateV6.Error, createdTask2.taskId)
        ))

        assertRepository.assertFinishedTasksResponse(response, expectedResponse)
    }

    /**
     * GIVEN no tasks finished
     * WHEN user searches for new events
     * THEN user sees no new tasks
     */
    @Test
    fun `get empty task events`(){
        //WHEN
        val response = orchestratorClient.finishedTaskEvents.getEvents(Instant.now(), PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsResponseV6(0, 0, 0, 0, emptyList())

        assertRepository.assertFinishedTasksResponse(response, expectedResponse)
    }
}