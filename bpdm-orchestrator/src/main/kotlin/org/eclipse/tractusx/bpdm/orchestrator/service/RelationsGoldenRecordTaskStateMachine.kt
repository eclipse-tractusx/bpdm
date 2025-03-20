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
import org.eclipse.tractusx.bpdm.orchestrator.config.StateMachineConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.*
import org.eclipse.tractusx.bpdm.orchestrator.exception.RelationsIllegalStateException
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
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

    fun initTask(mode: TaskMode, initBusinessPartnerRelations: BusinessPartnerRelations, record: GateRecordDb): RelationsGoldenRecordTaskDb {
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

    private fun getInitialStep(mode: TaskMode): TaskStep {
        return stateMachineConfigProperties.modeSteps[mode]!!.first()
    }

    fun doAbortTask(task: RelationsGoldenRecordTaskDb): RelationsGoldenRecordTaskDb{
        if(task.processingState.resultState != RelationsGoldenRecordTaskDb.ResultState.Pending)
            throw RelationsIllegalStateException(task.uuid, task.processingState)

        task.processingState.toAborted()
        task.updatedAt = DbTimestamp(Instant.now())

        return relationsTaskRepository.save(task)
    }

    private fun RelationsGoldenRecordTaskDb.ProcessingState.toAborted() {
        resultState = RelationsGoldenRecordTaskDb.ResultState.Aborted
        stepState = RelationsGoldenRecordTaskDb.StepState.Aborted
        pendingTimeout = null
        retentionTimeout = Instant.now().plus(taskConfigProperties.taskRetentionTimeout).toTimestamp()
    }
}