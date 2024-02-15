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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTask
import org.eclipse.tractusx.bpdm.orchestrator.model.TaskProcessingState
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoldenRecordTaskStateMachine(
    private val taskConfigProperties: TaskConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun initProcessingState(mode: TaskMode): TaskProcessingState {
        logger.debug { "Executing initProcessingState() with parameters $mode" }
        val now = Instant.now()

        val initialStep = getInitialStep(mode)

        return TaskProcessingState(
            mode = mode,
            resultState = ResultState.Pending,

            step = initialStep,
            stepState = StepState.Queued,

            taskCreatedAt = now,
            taskModifiedAt = now,

            taskPendingTimeout = now.plus(taskConfigProperties.taskPendingTimeout),
            taskRetentionTimeout = null
        )
    }

    fun doReserve(task: GoldenRecordTask) {
        logger.debug { "Executing doReserve() with parameters $task" }
        val state = task.processingState
        val now = Instant.now()

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Queued) {
            throw BpdmIllegalStateException(task.taskId, state)
        }

        // reserved for current step
        state.stepState = StepState.Reserved
        state.taskModifiedAt = now
    }

    fun doResolveTaskToSuccess(task: GoldenRecordTask, step: TaskStep, resultBusinessPartner: BusinessPartnerFullDto) {
        logger.debug { "Executing doResolveTaskToSuccess() with parameters $task // $step and $resultBusinessPartner" }
        val state = task.processingState
        val now = Instant.now()

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Reserved || state.step != step) {
            throw BpdmIllegalStateException(task.taskId, state)
        }

        val nextStep = getNextStep(state.mode, state.step)

        if (nextStep != null) {
            // still steps left to process -> queued for next step
            state.step = nextStep
            state.stepState = StepState.Queued
            state.taskModifiedAt = now
        } else {
            // last step finished -> set resultState and stepState to success
            resolveStateToSuccess(state)
        }

        task.businessPartner = resultBusinessPartner
    }

    fun doResolveTaskToError(task: GoldenRecordTask, step: TaskStep, errors: List<TaskErrorDto>) {
        logger.debug { "Executing doResolveTaskToError() with parameters $task // $step and $errors" }
        val state = task.processingState

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Reserved || state.step != step) {
            throw BpdmIllegalStateException(task.taskId, state)
        }

        resolveStateToError(state, errors)
    }

    fun doResolveTaskToTimeout(task: GoldenRecordTask) {
        val state = task.processingState

        if (state.resultState != ResultState.Pending) {
            throw BpdmIllegalStateException(task.taskId, state)
        }

        val errors = listOf(TaskErrorDto(TaskErrorType.Timeout, "Timeout reached"))
        resolveStateToError(state, errors)
    }

    private fun resolveStateToSuccess(state: TaskProcessingState) {
        val now = Instant.now()
        state.resultState = ResultState.Success
        state.stepState = StepState.Success
        state.taskModifiedAt = now
        state.taskPendingTimeout = null
        state.taskRetentionTimeout = now.plus(taskConfigProperties.taskRetentionTimeout)
    }

    private fun resolveStateToError(state: TaskProcessingState, errors: List<TaskErrorDto>) {
        val now = Instant.now()
        state.resultState = ResultState.Error
        state.errors = errors
        state.stepState = StepState.Error
        state.taskModifiedAt = now
        state.taskPendingTimeout = null
        state.taskRetentionTimeout = now.plus(taskConfigProperties.taskRetentionTimeout)
    }

    private fun getInitialStep(mode: TaskMode): TaskStep {
        return getStepsForMode(mode).first()
    }

    private fun getNextStep(mode: TaskMode, currentStep: TaskStep): TaskStep? {
        return getStepsForMode(mode)
            .dropWhile { it != currentStep }        // drop steps before currentStep
            .drop(1)                             // then drop currentStep
            .firstOrNull()                          // return next step
    }

    private fun getStepsForMode(mode: TaskMode): List<TaskStep> =
        when (mode) {
            TaskMode.UpdateFromSharingMember -> listOf(TaskStep.CleanAndSync, TaskStep.PoolSync)
            TaskMode.UpdateFromPool -> listOf(TaskStep.Clean)
        }
}
