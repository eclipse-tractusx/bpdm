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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.exception.RelationsIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationsGoldenRecordTaskStateMachine(
    private val taskConfigProperties: TaskConfigProperties,
    private val relationsTaskRepository: RelationsGoldenRecordTaskRepository,
    private val relationsRequestMapper: RelationsRequestMapper,
    private val stateMachineConfigProperties: StateMachineConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun initTask(mode: TaskMode, initBusinessPartnerRelations: BusinessPartnerRelations, record: SharingMemberRecordDb): RelationsGoldenRecordTaskDb {
        logger.debug { "Executing initProcessingState() with parameters mode: $mode and business partner relations data: $initBusinessPartnerRelations" }

        val initialStep = getInitialStep(mode)
        val initProcessingState = RelationsGoldenRecordTaskDb.ProcessingState(
            mode = mode,
            resultState = RelationsGoldenRecordTaskDb.ResultState.Pending,
            step = initialStep,
            errors = mutableListOf(),
            stepState = RelationsGoldenRecordTaskDb.StepState.Queued,
            pendingTimeout =  Instant.now().plus(taskConfigProperties.taskPendingTimeout).toTimestamp(),
            retentionTimeout = null
        )

        val initialTask = DbTimestamp.now().let { nowTime ->
            RelationsGoldenRecordTaskDb(
                gateRecord = record,
                processingState = initProcessingState,
                businessPartnerRelations = relationsRequestMapper.toBusinessPartnerRelations(initBusinessPartnerRelations),
                createdAt = nowTime,
                updatedAt = nowTime
            )
        }

        return relationsTaskRepository.save(initialTask)
    }

    fun doReserve(task: RelationsGoldenRecordTaskDb): RelationsGoldenRecordTaskDb {
        logger.debug { "Executing doReserve() with parameters $task" }
        val state = task.processingState

        if (state.resultState != RelationsGoldenRecordTaskDb.ResultState.Pending || state.stepState != RelationsGoldenRecordTaskDb.StepState.Queued) {
            throw RelationsIllegalStateException(task.uuid, state)
        }

        // reserved for current step
        task.processingState.stepState = RelationsGoldenRecordTaskDb.StepState.Reserved
        task.updatedAt = DbTimestamp(Instant.now())

        return relationsTaskRepository.save(task)
    }

    fun doAbortTask(task: RelationsGoldenRecordTaskDb): RelationsGoldenRecordTaskDb{
        if(task.processingState.resultState != RelationsGoldenRecordTaskDb.ResultState.Pending)
            throw RelationsIllegalStateException(task.uuid, task.processingState)

        task.processingState.toAborted()
        task.updatedAt = DbTimestamp(Instant.now())

        return relationsTaskRepository.save(task)
    }

    fun doResolveTaskToError(task: RelationsGoldenRecordTaskDb, step: TaskStep, errors: List<TaskRelationsErrorDto>): RelationsGoldenRecordTaskDb {
        logger.debug { "Executing doResolveTaskToError() with parameters $task // $step and $errors" }
        val state = task.processingState

        if (!isResolvableForStep(state, step)) {
            if(hasAlreadyResolvedStep(state, step))
            {
                logger.debug { "Task ${task.uuid} has already been processed for step $step. Result is ignored" }
                return task
            }else{
                throw RelationsIllegalStateException(task.uuid, state)
            }
        }

        task.processingState.toError(errors.map { relationsRequestMapper.toTaskError(it) })
        task.updatedAt =  DbTimestamp(Instant.now())

        return relationsTaskRepository.save(task)
    }

    fun resolveTaskStepToSuccess(
        task: RelationsGoldenRecordTaskDb,
        step: TaskStep,
        resultBusinessPartnerRelaitons: BusinessPartnerRelations
    ): RelationsGoldenRecordTaskDb {
        logger.debug { "Executing doResolveTaskToSuccess() with parameters $task // $step and $resultBusinessPartnerRelaitons" }
        val state = task.processingState

        if (!isResolvableForStep(state, step)) {
            if(hasAlreadyResolvedStep(state, step))
            {
                logger.debug { "Task ${task.uuid} has already been processed for step $step. Result is ignored" }
                return task
            }else{
                throw RelationsIllegalStateException(task.uuid, state)
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

        task.updateBusinessPartnerRelations(relationsRequestMapper.toBusinessPartnerRelations(resultBusinessPartnerRelaitons))
        task.updatedAt =  DbTimestamp(Instant.now())

        return relationsTaskRepository.save(task)
    }

    private fun getInitialStep(mode: TaskMode): TaskStep {
        return stateMachineConfigProperties.modeSteps[mode]!!.first()
    }

    private fun RelationsGoldenRecordTaskDb.ProcessingState.toAborted() {
        resultState = RelationsGoldenRecordTaskDb.ResultState.Aborted
        stepState = RelationsGoldenRecordTaskDb.StepState.Aborted
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
    }

    private fun RelationsGoldenRecordTaskDb.ProcessingState.toError(newErrors: List<RelationsTaskErrorDb>) {
        resultState = RelationsGoldenRecordTaskDb.ResultState.Error
        stepState = RelationsGoldenRecordTaskDb.StepState.Error
        errors.replace(newErrors)
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
    }

    private fun isResolvableForStep(state: RelationsGoldenRecordTaskDb.ProcessingState, step: TaskStep): Boolean{
        return state.resultState == RelationsGoldenRecordTaskDb.ResultState.Pending
                && state.stepState == RelationsGoldenRecordTaskDb.StepState.Reserved
                && state.step == step
    }

    private fun hasAlreadyResolvedStep(state: RelationsGoldenRecordTaskDb.ProcessingState, step: TaskStep): Boolean{
        if(state.step == step) return state.stepState != RelationsGoldenRecordTaskDb.StepState.Reserved
        return isStepBefore(step, state.step, state.mode)
    }

    private fun isStepBefore(stepBefore: TaskStep, stepAfter: TaskStep, mode: TaskMode): Boolean{
        val modeSteps = stateMachineConfigProperties.modeSteps[mode]!!
        return modeSteps.contains(stepBefore) && modeSteps.indexOf(stepBefore) <= modeSteps.indexOf(stepAfter)
    }

    private fun getNextStep(mode: TaskMode, currentStep: TaskStep): TaskStep? {
        return stateMachineConfigProperties.modeSteps[mode]!!
            .dropWhile { it != currentStep }        // drop steps before currentStep
            .drop(1)                             // then drop currentStep
            .firstOrNull()                          // return next step
    }

    private fun RelationsGoldenRecordTaskDb.ProcessingState.toStep(nextStep: TaskStep) {
        step = nextStep
        stepState = RelationsGoldenRecordTaskDb.StepState.Queued
    }

    private fun RelationsGoldenRecordTaskDb.ProcessingState.toSuccess() {
        resultState = RelationsGoldenRecordTaskDb.ResultState.Success
        stepState = RelationsGoldenRecordTaskDb.StepState.Success
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()

    }
}