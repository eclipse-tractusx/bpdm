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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.TemporalUnitOffset
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.GateRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GateRecordRepository
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

val WITHIN_ALLOWED_TIME_OFFSET: TemporalUnitOffset = Assertions.within(10, ChronoUnit.SECONDS)

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false",
        "bpdm.task.taskPendingTimeout=3s",
        "bpdm.task.taskRetentionTimeout=5s"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class GoldenRecordTaskStateMachineIT @Autowired constructor(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val gateRecordRepository: GateRecordRepository,
    private val dbTestHelpers: DbTestHelpers
) {

    private val testDataFactory = BusinessPartnerTestDataFactory()

    private val businessPartnerFull = testDataFactory.createFullBusinessPartner("full")

    private lateinit var gateRecord: GateRecordDb

    @BeforeEach
    fun cleanUp() {
        dbTestHelpers.truncateDbTables()
        gateRecord = gateRecordRepository.save(GateRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID()))
    }


    /**
     * WHEN creating an initial TaskProcessingState
     *  THEN expect the correct content
     */
    @Test
    @Transactional
    fun `initial state`() {
        val now = Instant.now()
        val task = goldenRecordTaskStateMachine.initTask(TaskMode.UpdateFromSharingMember, businessPartnerFull, gateRecord)
        val state = task.processingState

        assertProcessingState(state, GoldenRecordTaskDb.ResultState.Pending, TaskStep.CleanAndSync,GoldenRecordTaskDb.StepState.Queued)
        assertThat(state.mode).isEqualTo(TaskMode.UpdateFromSharingMember)
        assertThat(state.errors.size).isEqualTo(0)
        assertThat(task.createdAt.instant).isCloseTo(now, WITHIN_ALLOWED_TIME_OFFSET)
        assertThat(task.updatedAt.instant).isCloseTo(now, WITHIN_ALLOWED_TIME_OFFSET)
        assertThat(state.pendingTimeout?.instant).isCloseTo(now.plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)
        assertThat(state.retentionTimeout).isNull()
    }

    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving
     *  THEN expect the TaskProcessingState to walk through all the steps/states until final state Success
     * WHEN trying to reserve or resolve twice
     *  THEN expect an error
     */
    @Test
    @Transactional
    fun `walk through all UpdateFromSharingMember steps`() {
        // new task
        val task = goldenRecordTaskStateMachine.initTask(TaskMode.UpdateFromSharingMember, businessPartnerFull, gateRecord)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.CleanAndSync, GoldenRecordTaskDb.StepState.Queued)
        // taskPendingTimeout has been set
        val taskPendingTimeout = task.processingState.pendingTimeout
        assertThat(task.processingState.pendingTimeout?.instant).isCloseTo(Instant.now().plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)

        // 1st reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.CleanAndSync, GoldenRecordTaskDb.StepState.Reserved)

        // Can't reserve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doReserve(task)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // 1st resolve
        goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.CleanAndSync, businessPartnerFull)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.PoolSync, GoldenRecordTaskDb.StepState.Queued)

        // Can't resolve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.CleanAndSync, businessPartnerFull)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // 2nd reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.PoolSync, GoldenRecordTaskDb.StepState.Reserved)

        // Can't resolve with wrong step (CleanAndSync)!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.CleanAndSync, businessPartnerFull)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // taskPendingTimeout is still the same
        assertThat(task.processingState.pendingTimeout).isEqualTo(taskPendingTimeout)

        // 2nd and final resolve
        goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.PoolSync, businessPartnerFull)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Success, TaskStep.PoolSync, GoldenRecordTaskDb.StepState.Success)

        // taskRetentionTimeout has been set; taskPendingTimeout has been reset
        assertThat(task.processingState.pendingTimeout).isNull()
        assertThat(task.processingState.retentionTimeout?.instant).isCloseTo(
            Instant.now().plus(taskConfigProperties.taskRetentionTimeout),
            WITHIN_ALLOWED_TIME_OFFSET
        )

        // Can't resolve again!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doResolveTaskToError(
                task,
                TaskStep.PoolSync,
                listOf(TaskErrorDto(TaskErrorType.Unspecified, "error"))
            )
        }.isInstanceOf(BpdmIllegalStateException::class.java)
    }


    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving
     *  THEN expect the TaskProcessingState to walk through all the steps/states and taskModifiedAt to be updated
     */
    @Test
    @Transactional
    fun `walk through all UpdateFromPool steps`() {
        // new task
        val task = goldenRecordTaskStateMachine.initTask(TaskMode.UpdateFromPool, businessPartnerFull, gateRecord)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.Clean, GoldenRecordTaskDb.StepState.Queued)
        val modified0 = task.updatedAt.instant
        // taskPendingTimeout has been set
        assertThat(task.processingState.pendingTimeout?.instant).isCloseTo(Instant.now().plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)

        Thread.sleep(10)

        // reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.Clean, GoldenRecordTaskDb.StepState.Reserved)
        val modified1 = task.updatedAt.instant
        assertThat(modified1).isAfter(modified0)

        Thread.sleep(10)

        // resolve
        goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.Clean, businessPartnerFull)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Success, TaskStep.Clean, GoldenRecordTaskDb.StepState.Success)
        val modified2 = task.updatedAt.instant
        assertThat(modified2).isAfter(modified1)

        // taskRetentionTimeout has been set; taskPendingTimeout was reset
        assertThat(task.processingState.pendingTimeout).isNull()
        assertThat(task.processingState.retentionTimeout?.instant).isCloseTo(
            Instant.now().plus(taskConfigProperties.taskRetentionTimeout),
            WITHIN_ALLOWED_TIME_OFFSET
        )
    }


    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving with an error
     *  THEN expect the TaskProcessingState to reach final state Error
     */
    @Test
    @Transactional
    fun `walk through steps and resolve with error`() {
        // new task
        val task = goldenRecordTaskStateMachine.initTask(TaskMode.UpdateFromPool, businessPartnerFull, gateRecord)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.Clean, GoldenRecordTaskDb.StepState.Queued)
        // taskPendingTimeout has been set
        assertThat(task.processingState.pendingTimeout?.instant).isCloseTo(Instant.now().plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)

        // reserve
        goldenRecordTaskStateMachine.doReserve(task)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Pending, TaskStep.Clean, GoldenRecordTaskDb.StepState.Reserved)

        // resolve with error
        val errors = listOf(
            TaskErrorDto(TaskErrorType.Unspecified, "Unspecific error"),
            TaskErrorDto(TaskErrorType.Timeout, "Timeout")
        )
        goldenRecordTaskStateMachine.doResolveTaskToError(task, TaskStep.Clean, errors)
        assertProcessingState(task.processingState, GoldenRecordTaskDb.ResultState.Error, TaskStep.Clean, GoldenRecordTaskDb.StepState.Error)
        assertThat(task.processingState.errors).usingRecursiveFieldByFieldElementComparator().isEqualTo(errors)

        // taskRetentionTimeout has been set; taskPendingTimeout was reset
        assertThat(task.processingState.pendingTimeout).isNull()
        assertThat(task.processingState.retentionTimeout?.instant).isCloseTo(
            Instant.now().plus(taskConfigProperties.taskRetentionTimeout),
            WITHIN_ALLOWED_TIME_OFFSET
        )

        // Can't reserve now!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.doReserve(task)
        }.isInstanceOf(BpdmIllegalStateException::class.java)

        // Can't resolve now!
        assertThatThrownBy {
            goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, TaskStep.Clean, businessPartnerFull)
        }.isInstanceOf(BpdmIllegalStateException::class.java)
    }

    private fun assertProcessingState(processingState: GoldenRecordTaskDb.ProcessingState,
                                      resultState: GoldenRecordTaskDb.ResultState,
                                      step: TaskStep,
                                      stepState: GoldenRecordTaskDb.StepState) {
        assertThat(processingState.resultState).isEqualTo(resultState)
        assertThat(processingState.step).isEqualTo(step)
        assertThat(processingState.stepState).isEqualTo(stepState)
    }
}
