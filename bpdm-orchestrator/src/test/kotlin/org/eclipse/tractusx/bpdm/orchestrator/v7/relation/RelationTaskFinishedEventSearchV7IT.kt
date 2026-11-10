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

package org.eclipse.tractusx.bpdm.orchestrator.v7.relation

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.FinishedTaskEventsV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.junit.jupiter.api.Test
import java.time.Instant

class RelationTaskFinishedEventSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN relation task has been finished successfully
     * WHEN user searches for new events
     * THEN user sees event for finished task
     */
    @Test
    fun `get successfully finished relation task event`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName, TaskModeV7.UpdateFromPool)
        testDataClient.reserveAndResolveRelationTask(createdTask, testName)

        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(createdTask.processingState.createdAt, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
            FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Success, createdTask.taskId)
        ))

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }

    /**
     * GIVEN relation task has failed with an error
     * WHEN user searches for new events
     * THEN user sees event for finished task
     */
    @Test
    fun `get failed finished relation task event`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName, TaskModeV7.UpdateFromPool)
        testDataClient.failRelationTask(createdTask)

        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(createdTask.processingState.createdAt, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
            FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Error, createdTask.taskId)
        ))

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }

    /**
     * GIVEN several relation tasks finished and one task finished after time X
     * WHEN user searches for new events after time X
     * THEN user sees event for that finished task
     */
    @Test
    fun `get relation task event after time`(){
        //GIVEN
        val createdTask1 = testDataClient.createRelationTask("$testName 1", TaskModeV7.UpdateFromPool)
        val createdTask2 = testDataClient.createRelationTask("$testName 2", TaskModeV7.UpdateFromPool)

        testDataClient.failRelationTask(createdTask1)
        val timeX = Instant.now()
        testDataClient.failRelationTask(createdTask2)

        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(timeX, PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(1, 1, 0, 1, listOf(
            FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Error, createdTask2.taskId)
        ))

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }

    /**
     * GIVEN no relation tasks finished
     * WHEN user searches for new events
     * THEN user sees no new tasks
     */
    @Test
    fun `get empty relation task events`(){
        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(Instant.now(), PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(0, 0, 0, 0, emptyList())

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }

    /**
     * GIVEN no finished relation tasks
     * WHEN requesting finished tasks event
     * THEN empty
     */
    @Test
    fun `get empty finished relation task events no finished tasks`(){
        //GIVEN
        testDataClient.createRelationTask("testName 1")
        testDataClient.createRelationTask("testName 2")
        testDataClient.createRelationTask("testName 3")

        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(Instant.now(), PaginationRequest())

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(0, 0, 0, 0, emptyList())

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }

    /**
     * GIVEN finished relation tasks
     * WHEN requesting paginated finished tasks
     * THEN return paginated
     */
    @Test
    fun `get paginated finished relation task events`(){
        //GIVEN
        val createdTasks = (1 .. 6).map { testDataClient.createRelationTask("$testName $it", TaskModeV7.UpdateFromPool) }
        orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(step = TaskStepV7.Clean))
        createdTasks.forEach { testDataClient.resolveRelationTask(it, "Resolved $testName ${it.taskId}") }

        //WHEN
        val response = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(createdTasks.first().processingState.createdAt, PaginationRequest(1, 3))

        //THEN
        val expectedResponse = FinishedTaskEventsV7Response(6, 2, 1, 3, createdTasks.drop(3).map {
            FinishedTaskEventsV7Response.Event(Instant.now(), ResultStateV7.Success, it.taskId)
        }
        )

        assertRepo.assertFinishedTaskEventsResponseEqual(response, expectedResponse)
    }
}