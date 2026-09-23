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
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.v6.client.OrchestratorApiClientV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskErrorDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskErrorTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationResponseV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepResultEntryDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepResultRequestV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6

class OrchestratorTestDataClientV6(
    private val operatorClient: OrchestratorApiClientV6,
    private val requestFactory: OrchestratorRequestFactoryV6
) {

    fun createSharingMemberRecord(seed: String): String{
        return createTask(seed).recordId
    }

    fun createTask(seed: String, taskMode: TaskMode = TaskMode.UpdateFromSharingMember, recordId: String? = null): TaskClientStateDtoV6{
        val newTask = requestFactory.buildTaskCreate(seed).copy(recordId = recordId)
        val createRequest = TaskCreateRequestV6(taskMode.toV6(), listOf(newTask))
        val createResult = operatorClient.goldenRecordTasks.createTasks(createRequest)

        return createResult.createdTasks.single()
    }

    fun reserveTasks(step: TaskStepV6): TaskStepReservationResponseV6{
        val reservationRequest = TaskStepReservationRequestV6(step = step)
        return operatorClient.goldenRecordTasks.reserveTasksForStep(reservationRequest)
    }

    fun resolveTask(taskId: String, step: TaskStepV6, seed: String): TaskStepResultEntryDtoV6{
        reserveTasks(step)
        val businessPartnerResult = requestFactory.buildBusinessPartner(seed)
        val resultEntry = TaskStepResultEntryDtoV6(taskId, businessPartnerResult, emptyList())
        val resultRequest = TaskStepResultRequestV6(step, listOf(resultEntry))
        operatorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

    fun failTask(taskId: String, step: TaskStepV6): TaskStepResultEntryDtoV6{
        reserveTasks(step)

        val resultEntry = TaskStepResultEntryDtoV6(taskId, BusinessPartnerV6.empty, listOf(TaskErrorDtoV6(TaskErrorTypeV6.Unspecified, "Error Description")))
        val resultRequest = TaskStepResultRequestV6(step, listOf(resultEntry))
        operatorClient.goldenRecordTasks.resolveStepResults(resultRequest)

        return resultEntry
    }

}