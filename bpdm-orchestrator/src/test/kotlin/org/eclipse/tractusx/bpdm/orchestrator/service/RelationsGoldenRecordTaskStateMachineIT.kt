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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.GateRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.RelationsIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GateRecordRepository
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false",
        "bpdm.task.taskPendingTimeout=3s",
        "bpdm.task.taskRetentionTimeout=5s"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class RelationsGoldenRecordTaskStateMachineIT  @Autowired constructor(
    private val relationsGoldenRecordTaskStateMachine: RelationsGoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val gateRecordRepository: GateRecordRepository,
    private val dbTestHelpers: DbTestHelpers,
    private val stateMachineConfigProperties: StateMachineConfigProperties
) {

    private val businessPartnerRelations = BusinessPartnerRelations(relationType = RelationType.IsAlternativeHeadquarterFor, businessPartnerSourceBpnl = "BPNL1", businessPartnerTargetBpnl = "BPNL2")
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
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    @Transactional
    fun `initial state`(taskMode: TaskMode) {
        val now = Instant.now()
        val task = relationsGoldenRecordTaskStateMachine.initTask(taskMode, businessPartnerRelations, gateRecord)
        val expectedStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()
        val state = task.processingState

        assertProcessingState(state, RelationsGoldenRecordTaskDb.ResultState.Pending, expectedStep, RelationsGoldenRecordTaskDb.StepState.Queued)
        assertThat(state.mode).isEqualTo(taskMode)
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
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    @Transactional
    fun `walk through all UpdateFromSharingMember steps`(taskMode: TaskMode) {
        // new task
        val task = relationsGoldenRecordTaskStateMachine.initTask(taskMode, businessPartnerRelations, gateRecord)

        val allSteps = stateMachineConfigProperties.modeSteps[taskMode]!!
        allSteps.forEach { step ->
            assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Pending, step, RelationsGoldenRecordTaskDb.StepState.Queued)
            // taskPendingTimeout has been set
            assertThat(task.processingState.pendingTimeout?.instant).isCloseTo(Instant.now().plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)

            // reserve
            relationsGoldenRecordTaskStateMachine.doReserve(task)
            assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Pending, step, RelationsGoldenRecordTaskDb.StepState.Reserved)

            // Can't reserve again!
            assertThatThrownBy {
                relationsGoldenRecordTaskStateMachine.doReserve(task)
            }.isInstanceOf(RelationsIllegalStateException::class.java)

            // resolve
            relationsGoldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, businessPartnerRelations)

            // resolve again ignored
           relationsGoldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, businessPartnerRelations)
        }

        val finalStep = stateMachineConfigProperties.modeSteps[taskMode]!!.last()
        assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Success, finalStep, RelationsGoldenRecordTaskDb.StepState.Success)

        // taskRetentionTimeout has been set; taskPendingTimeout has been reset
        assertThat(task.processingState.pendingTimeout).isNull()
        assertThat(task.processingState.retentionTimeout?.instant).isCloseTo(
            Instant.now().plus(taskConfigProperties.taskRetentionTimeout),
            WITHIN_ALLOWED_TIME_OFFSET
        )

        // Second resolve ignored
        relationsGoldenRecordTaskStateMachine.doResolveTaskToError(
            task,
            finalStep,
            listOf(TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, "error"))
        )
    }

    /**
     * GIVEN a task with initial TaskProcessingState
     * WHEN reserving and resolving with an error
     *  THEN expect the TaskProcessingState to reach final state Error
     */
    @ParameterizedTest
    @EnumSource(TaskMode::class)
    @Transactional
    fun `walk through steps and resolve with error`(taskMode: TaskMode) {
        // new task
        val task = relationsGoldenRecordTaskStateMachine.initTask(taskMode, businessPartnerRelations, gateRecord)
        val expectedStep = stateMachineConfigProperties.modeSteps[taskMode]!!.first()
        assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Pending, expectedStep, RelationsGoldenRecordTaskDb.StepState.Queued)
        // taskPendingTimeout has been set
        assertThat(task.processingState.pendingTimeout?.instant).isCloseTo(Instant.now().plus(taskConfigProperties.taskPendingTimeout), WITHIN_ALLOWED_TIME_OFFSET)

        // reserve
        relationsGoldenRecordTaskStateMachine.doReserve(task)
        assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Pending, expectedStep, RelationsGoldenRecordTaskDb.StepState.Reserved)

        // resolve with error
        val errors = listOf(
            TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, "Unspecific error"),
            TaskRelationsErrorDto(TaskRelationsErrorType.Timeout, "Timeout")
        )
        relationsGoldenRecordTaskStateMachine.doResolveTaskToError(task, expectedStep, errors)
        assertProcessingState(task.processingState, RelationsGoldenRecordTaskDb.ResultState.Error, expectedStep, RelationsGoldenRecordTaskDb.StepState.Error)
        assertThat(task.processingState.errors).usingRecursiveFieldByFieldElementComparator().isEqualTo(errors)

        // taskRetentionTimeout has been set; taskPendingTimeout was reset
        assertThat(task.processingState.pendingTimeout).isNull()
        assertThat(task.processingState.retentionTimeout?.instant).isCloseTo(
            Instant.now().plus(taskConfigProperties.taskRetentionTimeout),
            WITHIN_ALLOWED_TIME_OFFSET
        )

        // Can't reserve now!
        assertThatThrownBy {
            relationsGoldenRecordTaskStateMachine.doReserve(task)
        }.isInstanceOf(RelationsIllegalStateException::class.java)

        // Resolve again ignored
        relationsGoldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, expectedStep, businessPartnerRelations)
    }

    private fun assertProcessingState(processingState: RelationsGoldenRecordTaskDb.ProcessingState,
                                      resultState: RelationsGoldenRecordTaskDb.ResultState,
                                      step: TaskStep,
                                      stepState: RelationsGoldenRecordTaskDb.StepState) {
        assertThat(processingState.resultState).isEqualTo(resultState)
        assertThat(processingState.step).isEqualTo(step)
        assertThat(processingState.stepState).isEqualTo(stepState)
    }
}