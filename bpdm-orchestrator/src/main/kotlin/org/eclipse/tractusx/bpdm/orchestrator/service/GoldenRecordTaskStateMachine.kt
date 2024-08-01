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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoldenRecordTaskStateMachine(
    private val taskConfigProperties: TaskConfigProperties,
    private val taskRepository: GoldenRecordTaskRepository,
    private val requestMapper: RequestMapper
) {

    private val logger = KotlinLogging.logger { }

    fun initTask(mode: TaskMode, initBusinessPartner: BusinessPartner, record: GateRecordDb): GoldenRecordTaskDb {
        logger.debug { "Executing initProcessingState() with parameters mode: $mode and business partner data: $initBusinessPartner" }

        val initialStep = getInitialStep(mode)
        val initProcessingState = GoldenRecordTaskDb.ProcessingState(
            mode = mode,
            resultState = ResultState.Pending,
            step = initialStep,
            errors = mutableListOf(),
            stepState = StepState.Queued,
            pendingTimeout =  Instant.now().plus(taskConfigProperties.taskPendingTimeout).toTimestamp(),
            retentionTimeout = null
        )

        val initialTask = GoldenRecordTaskDb(
            gateRecord = record,
            processingState = initProcessingState,
            businessPartner = requestMapper.toBusinessPartner(initBusinessPartner)
        )

        return taskRepository.save(initialTask)
    }

    fun doReserve(task: GoldenRecordTaskDb): GoldenRecordTaskDb {
        logger.debug { "Executing doReserve() with parameters $task" }
        val state = task.processingState

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Queued) {
            throw BpdmIllegalStateException(task.uuid, state)
        }

        // reserved for current step
        task.processingState.stepState = StepState.Reserved
        task.updatedAt = DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    fun resolveTaskStepToSuccess(
        task: GoldenRecordTaskDb,
        step: TaskStep,
        resultBusinessPartner: BusinessPartner
    ): GoldenRecordTaskDb {
        logger.debug { "Executing doResolveTaskToSuccess() with parameters $task // $step and $resultBusinessPartner" }
        val state = task.processingState

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Reserved || state.step != step) {
            throw BpdmIllegalStateException(task.uuid, state)
        }

        val nextStep = getNextStep(state.mode, state.step)

        if (nextStep != null) {
            // still steps left to process -> queued for next step
            task.processingState.toStep(nextStep)
        } else {
            // last step finished -> set resultState and stepState to success
            task.processingState.toSuccess()
        }

        task.updateBusinessPartner(requestMapper.toBusinessPartner(resultBusinessPartner))
        task.updatedAt =  DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    fun doResolveTaskToError(task: GoldenRecordTaskDb, step: TaskStep, errors: List<TaskErrorDto>): GoldenRecordTaskDb {
        logger.debug { "Executing doResolveTaskToError() with parameters $task // $step and $errors" }
        val state = task.processingState

        if (state.resultState != ResultState.Pending || state.stepState != StepState.Reserved || state.step != step) {
            throw BpdmIllegalStateException(task.uuid, state)
        }
        task.processingState.toError(errors.map { requestMapper.toTaskError(it) })
        task.updatedAt =  DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    fun doResolveTaskToTimeout(task: GoldenRecordTaskDb): GoldenRecordTaskDb {
        val state = task.processingState

        if (state.resultState != ResultState.Pending) {
            throw BpdmIllegalStateException(task.uuid, state)
        }

        val errors = listOf(TaskErrorDb(TaskErrorType.Timeout, "Timeout reached"))
        task.processingState.toError(errors)
        task.updatedAt = DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    private fun GoldenRecordTaskDb.ProcessingState.toStep(nextStep: TaskStep) {
        step = nextStep
        stepState = StepState.Queued
    }

    private fun GoldenRecordTaskDb.ProcessingState.toSuccess() {
            resultState = ResultState.Success
            stepState = StepState.Success
            pendingTimeout = null
            retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()

    }

    private fun GoldenRecordTaskDb.ProcessingState.toError(newErrors: List<TaskErrorDb>) {
            resultState = ResultState.Error
            stepState = StepState.Error
            errors.replace(newErrors)
            pendingTimeout = null
            retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
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
