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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestratorV7Client
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepResultV7Request

class OrchestratorTestDataClientV7(
    private val orchestratorClient: OrchestratorV7Client,
    private val requestFactory: OrchestratorRequestFactoryV7
) {

    fun createBusinessPartnerTask(seed: String, taskMode: TaskModeV7 = TaskModeV7.UpdateFromSharingMember, recordId: String? = null): TaskClientStateV7Dto {
        val newTask = requestFactory.buildBusinessPartnerTaskCreateEntry(seed).copy(recordId = recordId)
        val createRequest = TaskCreateV7Request(taskMode, listOf(newTask))
        val createResult = orchestratorClient.goldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }

    fun reserveBusinessPartnerTask(task: TaskClientStateV7Dto): TaskStepReservationEntryV7Dto {
        return reserveBusinessPartnerTasks(task.processingState.step).reservedTasks.single()
    }

    fun reserveBusinessPartnerTasks(step: TaskStepV7): TaskStepReservationV7Response {
        val reservationRequest = TaskStepReservationV7Request(step = step)
        return orchestratorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun reserveAndResolveBusinessPartnerTask(taskId: String, step: TaskStepV7, seed: String): TaskStepResultEntryV7Dto{
        reserveBusinessPartnerTasks(step)
        return resolveBusinessPartnerTask(taskId, step, seed)
    }

    fun resolveBusinessPartnerTask(taskId: String, step: TaskStepV7, seed: String): TaskStepResultEntryV7Dto{
        val businessPartnerResult = requestFactory.buildAdditionalAddressOfSiteBusinessPartner(seed)
        val resultEntry = TaskStepResultEntryV7Dto(taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultV7Request(step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failBusinessPartnerTask(taskId: String, step: TaskStepV7): TaskStepResultEntryV7Dto {
        reserveBusinessPartnerTasks(step)

        val resultEntry = TaskStepResultEntryV7Dto(taskId, BusinessPartnerV7Dto.empty, listOf(TaskErrorV7Dto(TaskErrorTypeV7.Unspecified, "Error Description")))
        val resultRequest = TaskStepResultV7Request(step, listOf(resultEntry))
        orchestratorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun createRelationTask(seed: String, taskMode: TaskModeV7 = TaskModeV7.UpdateFromSharingMember, recordId: String? = null): TaskClientRelationsStateV7Dto {
        val newTask = requestFactory.buildRelationTaskCreateEntry(seed, recordId)
        val createRequest = TaskCreateRelationsV7Request(taskMode, listOf(newTask))
        val createResult = orchestratorClient.relationsGoldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }


    fun reserveRelationTask(relationTask: TaskClientRelationsStateV7Dto): TaskRelationsStepReservationEntryV7Dto {
        return reserveRelationTask(relationTask.processingState.step).reservedTasks.single()
    }

    fun reserveRelationTask(reservationStep: TaskStepV7): TaskRelationsStepReservationV7Response {
        val reservationRequest = TaskStepReservationV7Request(step = reservationStep)
        return orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun reserveAndResolveRelationTask(relationTask: TaskClientRelationsStateV7Dto, seed: String): TaskRelationsStepResultEntryV7Dto{
        reserveRelationTask(relationTask)
        return resolveRelationTask(relationTask, seed)
    }

    fun resolveRelationTask(relationTask: TaskClientRelationsStateV7Dto, seed: String): TaskRelationsStepResultEntryV7Dto{
        val relation = requestFactory.buildRelation(seed)
        val resultEntry = TaskRelationsStepResultEntryV7Dto(relationTask.taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(relationTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun reserveAndResolveRelationTask(taskId: String, reservationStep: TaskStepV7, seed: String): TaskRelationsStepResultEntryV7Dto{
        reserveRelationTask(reservationStep)
        val relation = requestFactory.buildRelation(seed)
        val resultEntry = TaskRelationsStepResultEntryV7Dto(taskId, relation, emptyList())
        val resultRequest = TaskRelationsStepResultV7Request(reservationStep, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failRelationTask(relationTask: TaskClientRelationsStateV7Dto): TaskRelationsStepResultEntryV7Dto {
        reserveRelationTask(relationTask.processingState.step)

        val resultEntry = TaskRelationsStepResultEntryV7Dto(
            relationTask.taskId,
            BusinessPartnerRelationsV7Dto.empty,
            listOf(TaskRelationsErrorV7Dto(TaskRelationsErrorTypeV7.Unspecified, "Error Description"))
        )
        val resultRequest = TaskRelationsStepResultV7Request(relationTask.processingState.step, listOf(resultEntry))
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

}