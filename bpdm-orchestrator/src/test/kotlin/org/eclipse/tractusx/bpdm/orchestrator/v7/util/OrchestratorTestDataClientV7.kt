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

package org.eclipse.tractusx.bpdm.orchestrator.v7.util

import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV7
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*

class OrchestratorTestDataClientV7(
    private val orchestratorClient: OrchestrationApiClient,
    private val requestFactory: OrchestratorRequestFactoryV7
) {

    fun createBusinessPartnerTask(seed: String, taskMode: TaskMode = TaskMode.UpdateFromSharingMember, recordId: String? = null): TaskClientStateDto {
        val newTask = requestFactory.buildBusinessPartnerTaskCreateEntry(seed).copy(recordId = recordId)
        val createRequest = TaskCreateRequest(taskMode, listOf(newTask))
        val createResult = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }

    fun reserveBusinessPartnerTask(task: TaskClientStateDto): TaskStepReservationEntryDto {
        return reserveBusinessPartnerTasks(task.processingState.step).reservedTasks.single()
    }

    fun reserveBusinessPartnerTasks(step: TaskStep): TaskStepReservationResponse {
        val reservationRequest = TaskStepReservationRequest(step = step)
        return orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun reserveAndResolveBusinessPartnerTask(taskId: String, step: TaskStep, seed: String): TaskStepResultEntryDto{
        reserveBusinessPartnerTasks(step)
        return resolveBusinessPartnerTask(taskId, step, seed)
    }

    fun resolveBusinessPartnerTask(taskId: String, step: TaskStep, seed: String): TaskStepResultEntryDto{
        val businessPartnerResult = requestFactory.buildAdditionalAddressOfSiteBusinessPartner(seed)
        val resultEntry = TaskStepResultEntryDto(taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequest(step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failBusinessPartnerTask(taskId: String, step: TaskStep): TaskStepResultEntryDto {
        reserveBusinessPartnerTasks(step)

        val resultEntry = TaskStepResultEntryDto(taskId, BusinessPartner.empty, listOf(TaskErrorDto(TaskErrorType.Unspecified, "Error Description")))
        val resultRequest = TaskStepResultRequest(step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun createRelationTask(seed: String, taskMode: TaskMode = TaskMode.UpdateFromSharingMember, recordId: String? = null): TaskClientRelationsStateDto {
        val newTask = requestFactory.buildRelationTaskCreateEntry(seed, recordId)
        val createRequest = TaskCreateRelationsRequest(taskMode, listOf(newTask))
        val createResult = orchestratorClient.relationsGoldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }


    fun reserveRelationTask(relationTask: TaskClientRelationsStateDto): TaskRelationsStepReservationEntryDto {
        return reserveRelationTask(relationTask.processingState.step).reservedTasks.single()
    }

    fun reserveRelationTask(reservationStep: TaskStep): TaskRelationsStepReservationResponse {
        val reservationRequest = TaskStepReservationRequest(step = reservationStep)
        return orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun reserveAndResolveRelationTask(relationTask: TaskClientRelationsStateDto, seed: String): TaskRelationsStepResultEntryDto{
        reserveRelationTask(relationTask)
        return resolveRelationTask(relationTask, seed)
    }

    fun resolveRelationTask(relationTask: TaskClientRelationsStateDto, seed: String): TaskRelationsStepResultEntryDto{
        val relation = requestFactory.buildRelation(seed)
        val resultEntry = TaskRelationsStepResultEntryDto(relationTask.taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultRequest(relationTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun reserveAndResolveRelationTask(taskId: String, reservationStep: TaskStep, seed: String): TaskRelationsStepResultEntryDto{
        reserveRelationTask(reservationStep)
        val relation = requestFactory.buildRelation(seed)
        val resultEntry = TaskRelationsStepResultEntryDto(taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultRequest(reservationStep, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failRelationTask(relationTask: TaskClientRelationsStateDto): TaskRelationsStepResultEntryDto {
        reserveRelationTask(relationTask.processingState.step)

        val resultEntry = TaskRelationsStepResultEntryDto(
            relationTask.taskId,
            BusinessPartnerRelations.empty,
            listOf(TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, "Error Description"))
        )
        val resultRequest = TaskRelationsStepResultRequest(relationTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

}