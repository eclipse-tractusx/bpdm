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

package org.eclipse.tractusx.bpdm.orchestrator.v6.util

import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV6
import org.eclipse.tractusx.bpdm.orchestrator.api.model.*
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.client.OrchestratorV6Client
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessPartnerV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskClientStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskCreateV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskErrorV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskErrorTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationV6Response
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepResultEntryV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepResultV6Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepV6

class OrchestratorTestDataClientV6(
    private val operatorClient: OrchestratorV6Client,
    private val requestFactory: OrchestratorRequestFactoryV6
) {

    fun createSharingMemberRecord(seed: String): String{
        return createTask(seed).recordId
    }

    fun createTask(seed: String, taskMode: TaskMode = TaskMode.UpdateFromSharingMember, recordId: String? = null): TaskClientStateV6Dto{
        val newTask = requestFactory.buildTaskCreate(seed).copy(recordId = recordId)
        val createRequest = TaskCreateV6Request(taskMode.toV6(), listOf(newTask))
        val createResult = operatorClient.goldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }

    fun reserveTasks(step: TaskStepV6): TaskStepReservationV6Response{
        val reservationRequest = TaskStepReservationV6Request(step = step)
        return operatorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun resolveTask(taskId: String, step: TaskStepV6, seed: String): TaskStepResultEntryV6Dto{
        reserveTasks(step)
        val businessPartnerResult = requestFactory.buildBusinessPartner(seed)
        val resultEntry = TaskStepResultEntryV6Dto(taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultV6Request(step, listOf(resultEntry))
        operatorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failTask(taskId: String, step: TaskStepV6): TaskStepResultEntryV6Dto{
        reserveTasks(step)

        val resultEntry = TaskStepResultEntryV6Dto(taskId, BusinessPartnerV6Dto.empty, listOf(TaskErrorV6Dto(TaskErrorTypeV6.Unspecified, "Error Description")))
        val resultRequest = TaskStepResultV6Request(step, listOf(resultEntry))
        operatorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

}