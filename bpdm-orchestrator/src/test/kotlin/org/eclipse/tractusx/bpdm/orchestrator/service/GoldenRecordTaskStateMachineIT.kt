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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.TemporalUnitOffset
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTask
import org.eclipse.tractusx.bpdm.orchestrator.model.TaskProcessingState
import org.eclipse.tractusx.bpdm.orchestrator.testdata.BusinessPartnerTestValues
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

val WITHIN_ALLOWED_TIME_OFFSET: TemporalUnitOffset = Assertions.within(10, ChronoUnit.SECONDS)
val TASK_ID = "TASK-ID"

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.api.upsert-limit=3",
        "bpdm.task.task-timeout=12h"
    ]
)
class GoldenRecordTaskStateMachineIT @Autowired constructor(
    val taskConfigProperties: TaskConfigProperties,
    val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine
) {

    /**
     * WHEN creating an initial TaskProcessingState
     *  THEN expect the correct content
     */
    @Test
    fun `initial state`() {
        val now = Instant.now()
        val state = goldenRecordTaskStateMachine.initProcessingState(TaskMode.UpdateFromSharingMember)

        assertProcessingStateDto(state, ResultState.Pending, TaskStep.CleanAndSync, StepState.Queued)
        assertThat(state.mode).isEqualTo(TaskMode.UpdateFromSharingMember)
        assertThat(state.errors.size).isEqualTo(0)
        assertThat(state.reservationTimeout).isNull()
        assertThat(state.taskCreatedAt).isCloseTo(now, WITHIN_ALLOWED_TIME_OFFSET)
        assertThat(state.taskModifiedAt).isEqualTo(state.taskCreatedAt)
        assertThat(state.taskTimeout).isCloseTo(now.plus(taskConfigProperties.taskTimeout), WITHIN_ALLOWED_TIME_OFFSET)
    }

    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving
     *  THEN expect the TaskProcessingState to walk through all the steps/states until final state Success
     * WHEN trying to reserve or resolve twice
     *  THEN expect an error
     */
    @Test
    fun `walk through all UpdateFromSharingMember steps`() {
        // new task
        val task = initTask(TaskMode.UpdateFromSharingMember)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.CleanAndSync, StepState.Queued)

        // 1st reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.CleanAndSync, StepState.Reserved)

        // Can't reserve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doReserve(task)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // 1st resolve
        goldenRecordTaskStateMachine.doResolveSuccessful(task, BusinessPartnerTestValues.businessPartner1Full)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.PoolSync, StepState.Queued)
        assertThat(task.processingState.reservationTimeout).isNull()

        // Can't resolve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doResolveSuccessful(task, BusinessPartnerTestValues.businessPartner1Full)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // 2nd reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.PoolSync, StepState.Reserved)

        // 2nd resolve
        goldenRecordTaskStateMachine.doResolveSuccessful(task, BusinessPartnerTestValues.businessPartner1Full)
        assertProcessingStateDto(task.processingState, ResultState.Success, TaskStep.PoolSync, StepState.Success)

        // Can't resolve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doResolveFailed(task, listOf(TaskErrorDto(TaskErrorType.Unspecified, "error")))
        }.isInstanceOf(BpdmIllegalStateException::class.java)
    }


    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving
     *  THEN expect the TaskProcessingState to walk through all the steps/states and taskModifiedAt and reservationTimeout to be updated
     */
    @Test
    fun `walk through all UpdateFromPool steps`() {
        // new task
        val task = initTask(TaskMode.UpdateFromPool)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.Clean, StepState.Queued)
        val modified0 = task.processingState.taskModifiedAt

        Thread.sleep(10)

        // reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.Clean, StepState.Reserved)
        assertThat(task.processingState.reservationTimeout)
            .isCloseTo(Instant.now().plus(taskConfigProperties.taskReservationTimeout), WITHIN_ALLOWED_TIME_OFFSET)
        val modified1 = task.processingState.taskModifiedAt
        assertThat(modified1).isAfter(modified0)

        Thread.sleep(10)

        // resolve
        goldenRecordTaskStateMachine.doResolveSuccessful(task, BusinessPartnerTestValues.businessPartner1Full)
        assertProcessingStateDto(task.processingState, ResultState.Success, TaskStep.Clean, StepState.Success)
        val modified2 = task.processingState.taskModifiedAt
        assertThat(modified2).isAfter(modified1)
    }


    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving with an error
     *  THEN expect the TaskProcessingState to reach final state Error
     */
    @Test
    fun `walk through steps and resolve with error`() {
        // new task
        val task = initTask(TaskMode.UpdateFromPool)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.Clean, StepState.Queued)

        // reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingStateDto(task.processingState, ResultState.Pending, TaskStep.Clean, StepState.Reserved)

        // resolve with error
        val errors = listOf(
            TaskErrorDto(TaskErrorType.Unspecified, "Unspecific error"),
            TaskErrorDto(TaskErrorType.Timeout, "Timeout")
        )
        goldenRecordTaskStateMachine.doResolveFailed(task, errors)
        assertProcessingStateDto(task.processingState, ResultState.Error, TaskStep.Clean, StepState.Error)
        assertThat(task.processingState.errors).isEqualTo(errors)

        // Can't reserve now!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doReserve(task)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // Can't reserve now!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doResolveSuccessful(task, BusinessPartnerTestValues.businessPartner1Full)
        }.isInstanceOf(BpdmIllegalStateException::class.java)
    }

    private fun assertProcessingStateDto(processingState: TaskProcessingState, resultState: ResultState, step: TaskStep, stepState: StepState) {
        assertThat(processingState.resultState).isEqualTo(resultState)
        assertThat(processingState.step).isEqualTo(step)
        assertThat(processingState.stepState).isEqualTo(stepState)
    }

    private fun initTask(mode: TaskMode = TaskMode.UpdateFromSharingMember) =
        GoldenRecordTask(
            taskId = TASK_ID,
            businessPartner = BusinessPartnerFullDto(
                generic = BusinessPartnerTestValues.businessPartner1
            ),
            processingState = goldenRecordTaskStateMachine.initProcessingState(mode)
        )
}
