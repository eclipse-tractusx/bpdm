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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.common.model.toTimestamp
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoldenRecordTaskStateMachine(
    private val taskConfigProperties: TaskConfigProperties,
    private val taskRepository: GoldenRecordTaskRepository,
    private val requestMapper: RequestMapper,
    private val stateMachineConfigProperties: StateMachineConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun initTask(mode: TaskModeV7, initBusinessPartner: BusinessPartnerRequest, record: SharingMemberRecordDb): GoldenRecordTaskDb {
        logger.debug { "Executing initProcessingState() with parameters mode: $mode and business partner data: $initBusinessPartner" }

        val initialStep = getInitialStep(mode)
        val initProcessingState = GoldenRecordTaskDb.ProcessingState(
            mode = mode,
            resultState = GoldenRecordTaskDb.ResultState.Pending,
            step = initialStep,
            errors = mutableListOf(),
            stepState = GoldenRecordTaskDb.StepState.Queued,
            pendingTimeout =  Instant.now().plus(taskConfigProperties.taskPendingTimeout).toTimestamp(),
            retentionTimeout = null
        )

        val initialTask = DbTimestamp.now().let { nowTime ->
            GoldenRecordTaskDb(
                gateRecord = record,
                processingState = initProcessingState,
                businessPartner = requestMapper.toBusinessPartner(initBusinessPartner),
                createdAt = nowTime,
                updatedAt = nowTime
            )
        }

        return taskRepository.save(initialTask)
    }

    fun doReserve(task: GoldenRecordTaskDb): GoldenRecordTaskDb {
        logger.debug { "Executing doReserve() with parameters $task" }
        val state = task.processingState

        if (state.resultState != GoldenRecordTaskDb.ResultState.Pending || state.stepState != GoldenRecordTaskDb.StepState.Queued) {
            throw BpdmIllegalStateException(task.uuid, state)
        }

        // reserved for current step
        task.processingState.stepState = GoldenRecordTaskDb.StepState.Reserved
        task.updatedAt = DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    /**
     * Resolves the given step of the task as successful, moving the task to its next step or to overall success, and
     * answers with null where the step was already resolved and the result is therefore ignored.
     */
    fun resolveTaskStepToSuccess(
        task: GoldenRecordTaskDb,
        step: TaskStepV7,
        resultBusinessPartner: BusinessPartnerV7Dto
    ): GoldenRecordTaskDb? {
        logger.debug { "Executing doResolveTaskToSuccess() with parameters $task // $step and $resultBusinessPartner" }
        val state = task.processingState

        if (!isResolvableForStep(state, step)) {
            if(hasAlreadyResolvedStep(state, step))
            {
                logger.debug { "Task ${task.uuid} has already been processed for step $step. Result is ignored" }
                return null
            }else{
                throw BpdmIllegalStateException(task.uuid, state)
            }
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

    /**
     * Resolves the given step of the task as failed and answers with null where the step was already resolved and the
     * errors are therefore ignored.
     */
    fun doResolveTaskToError(task: GoldenRecordTaskDb, step: TaskStepV7, errors: List<TaskErrorV7Dto>): GoldenRecordTaskDb? {
        logger.debug { "Executing doResolveTaskToError() with parameters $task // $step and $errors" }
        val state = task.processingState

        if (!isResolvableForStep(state, step)) {
            if(hasAlreadyResolvedStep(state, step))
            {
                logger.debug { "Task ${task.uuid} has already been processed for step $step. Result is ignored" }
                return null
            }else{
                throw BpdmIllegalStateException(task.uuid, state)
            }
        }

        task.processingState.toError(errors.map { requestMapper.toTaskError(it) })
        task.updatedAt =  DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    fun doResolveTaskToTimeout(task: GoldenRecordTaskDb): GoldenRecordTaskDb {
        val state = task.processingState

        if (state.resultState != GoldenRecordTaskDb.ResultState.Pending) {
            throw BpdmIllegalStateException(task.uuid, state)
        }

        val errors = listOf(TaskErrorDb(TaskErrorTypeV7.Timeout, "Timeout reached"))
        task.processingState.toError(errors)
        task.updatedAt = DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    fun doAbortTask(task: GoldenRecordTaskDb): GoldenRecordTaskDb{
        if(task.processingState.resultState != GoldenRecordTaskDb.ResultState.Pending)
            throw BpdmIllegalStateException(task.uuid, task.processingState)

        task.processingState.toAborted()
        task.updatedAt = DbTimestamp(Instant.now())

        return taskRepository.save(task)
    }

    private fun GoldenRecordTaskDb.ProcessingState.toStep(nextStep: TaskStepV7) {
        step = nextStep
        stepState = GoldenRecordTaskDb.StepState.Queued
    }

    private fun GoldenRecordTaskDb.ProcessingState.toSuccess() {
            resultState = GoldenRecordTaskDb.ResultState.Success
            stepState = GoldenRecordTaskDb.StepState.Success
            pendingTimeout = null
            retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()

    }

    private fun GoldenRecordTaskDb.ProcessingState.toError(newErrors: List<TaskErrorDb>) {
            resultState = GoldenRecordTaskDb.ResultState.Error
            stepState = GoldenRecordTaskDb.StepState.Error
            errors.replace(newErrors)
            pendingTimeout = null
            retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
    }

    private fun GoldenRecordTaskDb.ProcessingState.toAborted() {
        resultState = GoldenRecordTaskDb.ResultState.Aborted
        stepState = GoldenRecordTaskDb.StepState.Aborted
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
    }

    private fun getInitialStep(mode: TaskModeV7): TaskStepV7 {
        return stateMachineConfigProperties.modeSteps[mode]!!.first()
    }

    private fun getNextStep(mode: TaskModeV7, currentStep: TaskStepV7): TaskStepV7? {
        return stateMachineConfigProperties.modeSteps[mode]!!
            .dropWhile { it != currentStep }        // drop steps before currentStep
            .drop(1)                             // then drop currentStep
            .firstOrNull()                          // return next step
    }

    private fun hasAlreadyResolvedStep(state: GoldenRecordTaskDb.ProcessingState, step: TaskStepV7): Boolean{
        if(state.step == step) return state.stepState != GoldenRecordTaskDb.StepState.Reserved && state.stepState != GoldenRecordTaskDb.StepState.Queued
        return isStepBefore(step, state.step, state.mode)
    }

    private fun isStepBefore(stepBefore: TaskStepV7, stepAfter: TaskStepV7, mode: TaskModeV7): Boolean{
        val modeSteps = stateMachineConfigProperties.modeSteps[mode]!!
        return modeSteps.contains(stepBefore) && modeSteps.indexOf(stepBefore) <= modeSteps.indexOf(stepAfter)
    }

    private fun isResolvableForStep(state: GoldenRecordTaskDb.ProcessingState, step: TaskStepV7): Boolean{
        return state.resultState == GoldenRecordTaskDb.ResultState.Pending
                && state.stepState == GoldenRecordTaskDb.StepState.Reserved
                && state.step == step
    }


}
