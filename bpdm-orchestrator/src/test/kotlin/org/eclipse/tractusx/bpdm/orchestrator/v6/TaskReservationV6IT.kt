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

import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationResponse
import org.junit.jupiter.api.Test
import java.time.Instant

class TaskReservationV6IT: UnscheduledOrchestratorTestV6() {


    /**
     * GIVEN task in queue in step
     * WHEN user requests reservation of new tasks for that step
     * THEN user sees reserved task
     */
    @Test
    fun `reserve queued tasks`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedEntry = expectedResultFactory.buildTaskStepReservationEntry(createdTask.businessPartnerResult)
        val expectedResult = TaskStepReservationResponse(listOf(expectedEntry), Instant.now().plus(expectedResultFactory.pendingTimeout))
        assertRepository.assertTaskReservationResponse(reservedTasks, expectedResult)
    }

    /**
     * GIVEN no tasks in queue of step
     * WHEN user requests reservation of new tasks in that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve no queued tasks`(){
        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = TaskStep.CleanAndSync)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationResponse(emptyList(), Instant.now())
        assertRepository.assertTaskReservationResponse(reservedTasks, expectedResult)
    }

    /**
     * GIVEN tasks queued in step
     * WHEN user requests reservation of new tasks of different step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve different step tasks`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = getDifferentStep(createdTask.processingState.step))
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationResponse(emptyList(), Instant.now())
        assertRepository.assertTaskReservationResponse(reservedTasks, expectedResult)
    }

    /**
     * GIVEN task reserved in step
     * WHEN user requests reservation of tasks for that step
     * THEN user sees no reserved tasks
     */
    @Test
    fun `reserve reserved step tasks`(){
        //GIVEN
        val createdTask = testDataClient.createTask(testName)
        testDataClient.reserveTasks(createdTask.processingState.step)

        //WHEN
        val reservationRequest = TaskStepReservationRequest(step = createdTask.processingState.step)
        val reservedTasks = orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)

        //THEN
        val expectedResult = TaskStepReservationResponse(emptyList(), Instant.now())
        assertRepository.assertTaskReservationResponse(reservedTasks, expectedResult)
    }

    private fun getDifferentStep(step: TaskStep): TaskStep {
        return TaskStep.entries.find { it != step }!!
    }
}