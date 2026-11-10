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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.orchestrator.v7.ScheduledTimeoutOrchestratorTestBaseV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource


class BusinessPartnerTaskTimeoutScheduledIT: ScheduledTimeoutOrchestratorTestBaseV7(){

    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `wait for task pending and retention timeout`(taskMode: TaskModeV7) {
        // create tasks
        val createdTask1 = testDataClient.createBusinessPartnerTask("$testName 1", taskMode)
        val createdTask2 = testDataClient.createBusinessPartnerTask("$testName 2", taskMode)
        val taskIds = listOf(createdTask1, createdTask2).map { TaskStateV7Request.Entry(it.taskId, it.recordId) }

        // check for state Pending
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Pending)
        }

        // wait for 1/2 pending time -> should still be pending
        Thread.sleep(taskConfigProperties.taskPendingTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Pending)
        }

        // wait for another 1/2 pending time plus 1sec -> should be in state Error / Timeout
        Thread.sleep(taskConfigProperties.taskPendingTimeout.dividedBy(2).plusSeconds(1).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Error)
            assertThat(it.errors.first().type).isEqualTo(TaskErrorTypeV7.Timeout)
        }

        // wait for 1/2 retention time -> should still be in state Error / Timeout
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(taskIds) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Error)
        }

        // wait for 1/2 retention time plus 1sec -> should be removed now
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = searchTaskStates(taskIds).tasks
        assertThat(foundTasks.size).isZero()
    }

    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `wait for task retention timeout after success`(taskMode: TaskModeV7) {
        // create single task in UpdateFromPool mode (only one step)
        val createdTask = testDataClient.createBusinessPartnerTask("$testName 1", taskMode)
        val createdTasks = listOf(createdTask)
        val taskId =  createdTask.taskId


        val allSteps = stateMachineConfigProperties.modeSteps[taskMode]!!
        allSteps.forEach { step ->
            reserveTasks(step).reservedTasks.single()
            // resolve with success
            val cleaningResult = TaskStepResultEntryV7Dto(
                taskId = taskId,
                businessPartner = createdTask.businessPartnerResult
            )
            resolveTasks(step, listOf(cleaningResult))
        }

        // should be in state Success now
        createdTasks.searchTaskStates(listOf(taskId)).tasks.forEach {
            assertThat(it.processingState.resultState).isEqualTo(ResultStateV7.Success)
        }

        // wait for 1/2 retention time -> should still be in state Success
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        createdTasks.searchTaskStates(listOf(taskId)).tasks.forEach {
            assertThat(it.processingState.resultState).isEqualTo(ResultStateV7.Success)
        }

        // wait for 1/2 retention time -> should still be removed
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = createdTasks.searchTaskStates(listOf(taskId)).tasks
        assertThat(foundTasks.size).isZero()
    }

    @ParameterizedTest
    @EnumSource(TaskModeV7::class)
    fun `wait for task retention timeout after error`(taskMode: TaskModeV7) {
        // create single task
        val createdTask = testDataClient.createBusinessPartnerTask("$testName 1", taskMode)
        val createdTasks = listOf(createdTask)
        val firstStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()

        // reserve task
        val reservedTask = reserveTasks(firstStep).reservedTasks.single()
        val taskId = reservedTask.taskId
        val searchIdentity = reservedTask.toTaskSearchIdentity(createdTasks)

        // resolve with error
        val cleaningResult = TaskStepResultEntryV7Dto(
            taskId = taskId,
            businessPartner = reservedTask.businessPartner,
            errors = listOf(TaskErrorV7Dto(TaskErrorTypeV7.Unspecified, "Unfortunate event"))
        )
        resolveTasks(firstStep, listOf(cleaningResult))

        // should be in state Success now
        checkStateForAllTasks(listOf(searchIdentity)) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Error)
        }

        // wait for 1/2 retention time -> should still be in state Success
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).toMillis())
        checkStateForAllTasks(listOf(searchIdentity)) {
            assertThat(it.resultState).isEqualTo(ResultStateV7.Error)
        }

        // wait for 1/2 retention time -> should still be removed
        Thread.sleep(taskConfigProperties.taskRetentionTimeout.dividedBy(2).plusSeconds(1).toMillis())
        val foundTasks = searchTaskStates(listOf(searchIdentity)).tasks
        assertThat(foundTasks.size).isZero()
    }



    private fun reserveTasks(step: TaskStepV7, amount: Int = 3) =
        orchestratorClient.goldenRecordTasks.reserveTasksForStep(
            TaskStepReservationV7Request(
                step = step,
                amount = amount
            )
        )

    private fun resolveTasks(step: TaskStepV7, results: List<TaskStepResultEntryV7Dto>) =
        orchestratorClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultV7Request(step, results)
        )

    private fun searchTaskStates(taskIds: List<TaskStateV7Request.Entry>) =
        orchestratorClient.goldenRecordTasks.searchTaskStates(
            TaskStateV7Request(taskIds)
        )

    private fun checkStateForAllTasks(taskIds: List<TaskStateV7Request.Entry>, checkFunc: (TaskProcessingStateV7Dto) -> Unit) {
        searchTaskStates(taskIds).tasks
            .also { assertThat(it.size).isEqualTo(taskIds.size) }
            .forEach { stateDto -> checkFunc(stateDto.processingState) }
    }

    private fun TaskClientStateV7Dto.toTaskSearchIdentity() = TaskStateV7Request.Entry(taskId, recordId)

    private fun TaskStepReservationEntryV7Dto.toTaskSearchIdentity(createdTasks: List<TaskClientStateV7Dto>) =
        TaskStateV7Request.Entry(taskId, createdTasks.find { it.taskId == taskId }!!.recordId)

    private fun List<TaskClientStateV7Dto>.searchTaskStates(taskIds: List<String>) =
        this.searchTaskStates(taskIds.toSet())

    private fun List<TaskClientStateV7Dto>.searchTaskStates(taskIds: Set<String>) =
            this.filter { taskIds.contains(it.taskId) }
                .map { it.toTaskSearchIdentity() }
                .let { searchTaskStates(it) }


}
