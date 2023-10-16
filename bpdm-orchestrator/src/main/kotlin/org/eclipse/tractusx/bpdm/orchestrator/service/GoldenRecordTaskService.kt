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

import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmEmptyResultException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTask
import org.eclipse.tractusx.bpdm.orchestrator.model.TaskProcessingState
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class GoldenRecordTaskService(
    val taskStorage: GoldenRecordTaskStorage,
    val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine
) {

    @Synchronized
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        return createRequest.businessPartners
            .map { businessPartnerGeneric -> taskStorage.addTask(initTask(createRequest, businessPartnerGeneric)) }
            .map(::toTaskClientStateDto)
            .let { TaskCreateResponse(createdTasks = it) }
    }

    @Synchronized
    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        return stateRequest.taskIds
            .mapNotNull { taskId -> taskStorage.getTask(taskId) }       // skip missing tasks
            .map(::toTaskClientStateDto)
            .let { TaskStateResponse(tasks = it) }
    }

    @Synchronized
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse {
        val now = Instant.now()

        val tasks = taskStorage.getQueuedTasksByStep(reservationRequest.step, reservationRequest.amount)
        tasks.forEach { task -> goldenRecordTaskStateMachine.doReserve(task) }

        val reservationTimeout = tasks
            .mapNotNull { it.processingState.reservationTimeout }
            .minOrNull()
            ?: now

        val taskEntries = tasks.map { task ->
            TaskStepReservationEntryDto(
                taskId = task.taskId,
                businessPartner = task.businessPartner
            )
        }

        return TaskStepReservationResponse(
            reservedTasks = taskEntries,
            timeout = reservationTimeout
        )
    }

    @Synchronized
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        resultRequest.results
            .forEach { resultEntry ->
                val task = taskStorage.getTask(resultEntry.taskId)
                    ?: throw BpdmTaskNotFoundException(resultEntry.taskId)

                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner
                if (errors.isNotEmpty()) {
                    goldenRecordTaskStateMachine.doResolveFailed(task, errors)
                } else if (resultBusinessPartner != null) {
                    goldenRecordTaskStateMachine.doResolveSuccessful(task, resultBusinessPartner)
                } else {
                    throw BpdmEmptyResultException(resultEntry.taskId)
                }
            }
    }

    private fun initTask(
        createRequest: TaskCreateRequest,
        businessPartnerGeneric: BusinessPartnerGenericDto
    ) = GoldenRecordTask(
        taskId = UUID.randomUUID().toString(),
        businessPartner = BusinessPartnerFullDto(
            generic = businessPartnerGeneric
        ),
        processingState = goldenRecordTaskStateMachine.initProcessingState(createRequest.mode)
    )

    private fun toTaskClientStateDto(task: GoldenRecordTask): TaskClientStateDto {
        val businessPartnerResult = when (task.processingState.resultState) {
            ResultState.Success -> task.businessPartner.generic
            else -> null
        }
        return TaskClientStateDto(
            taskId = task.taskId,
            processingState = toTaskProcessingStateDto(task.processingState),
            businessPartnerResult = businessPartnerResult
        )
    }

    private fun toTaskProcessingStateDto(processingState: TaskProcessingState): TaskProcessingStateDto {
        return TaskProcessingStateDto(
            resultState = processingState.resultState,
            step = processingState.step,
            stepState = processingState.stepState,
            errors = processingState.errors,
            createdAt = processingState.taskCreatedAt,
            modifiedAt = processingState.taskModifiedAt,
            timeout = processingState.taskTimeout
        )
    }
}
