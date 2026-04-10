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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class RelationTaskReservationV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN relation task in queue in step
     * WHEN user requests reservation of new tasks for that step
     * THEN user sees reserved task
     */
    @Test
    fun `reserve queued relation tasks`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedEntry = TaskRelationsStepReservationEntryDto(
            taskId = createdTask.taskId,
            recordId = createdTask.recordId,
            businessPartnerRelations = createdTask.businessPartnerRelationsResult
        )
        val expectedResult = TaskRelationsStepReservationResponse(listOf(expectedEntry), Instant.now().plus(resultFactory.pendingTimeout))
        assertRepo.assertRelationTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = true)
    }

    /**
     * GIVEN no relation tasks in queue of step
     * WHEN user requests reservation of new tasks in that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve no queued relation tasks`(){
        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = TaskStep.CleanAndSync)
        val reservedTasks = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskRelationsStepReservationResponse(emptyList(), Instant.now())
        assertRepo.assertRelationTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }

    /**
     * GIVEN relation tasks queued in step
     * WHEN user requests reservation of new tasks of different step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve different step relation tasks`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = getDifferentStep(createdTask.processingState.step))
        val reservedTasks = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskRelationsStepReservationResponse(emptyList(), Instant.now())
        assertRepo.assertRelationTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }

    /**
     * GIVEN relation task reserved in step
     * WHEN user requests reservation of tasks for that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve reserved step relation tasks`(){
        //GIVEN
        val createdTask = testDataClient.createRelationTask(testName)
        testDataClient.reserveRelationTask(createdTask)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskRelationsStepReservationResponse(emptyList(), Instant.now())
        assertRepo.assertRelationTaskReservationResponseEqual(reservedTasks, expectedResult, ignoreRecordId = false)
    }


    /**
     * WHEN reserving too many relation tasks
     * THEN throw 400 BAD REQUEST
     */
    @Test
    fun `try requesting too many relation tasks`() {
        //WHEN
        val request: () -> Unit = {
            orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(200, TaskStep.CleanAndSync))
        }

        //THEN
        Assertions.assertThatThrownBy(request).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun getDifferentStep(step: TaskStep): TaskStep {
        return TaskStep.entries.find { it != step }!!
    }
}