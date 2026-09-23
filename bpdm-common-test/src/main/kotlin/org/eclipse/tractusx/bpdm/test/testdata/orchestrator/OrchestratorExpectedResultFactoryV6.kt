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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ResultStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StepStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskProcessingStateDtoV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationEntryDtoV6
import java.time.Duration
import java.time.Instant

class OrchestratorExpectedResultFactoryV6(
    val pendingTimeout: Duration,
    val retentionTimeout: Duration,
    val taskStepTransitions: Map<TaskMode, List<TaskStep>>
) {

    fun buildCreatedTaskClientState(
        businessPartner: BusinessPartnerV6,
        taskMode: TaskMode,
        taskId: String = "any UUID",
        recordId: String = "any UUID",
        modifiedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now()
    ): TaskClientStateDtoV6{

        return TaskClientStateDtoV6(
            taskId = taskId,
            recordId = recordId,
            businessPartnerResult = businessPartner,
            processingState = TaskProcessingStateDtoV6(
                resultState = ResultStateV6.Pending,
                step = when (taskStepTransitions[taskMode]!!.first()) {
                    TaskStep.CleanAndSync -> org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6.CleanAndSync
                    TaskStep.PoolSync -> org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6.PoolSync
                    TaskStep.Clean -> org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6.Clean
                },
                stepState = StepStateV6.Queued,
                errors = emptyList(),
                modifiedAt = modifiedAt,
                createdAt = createdAt,
                timeout = createdAt.plus(retentionTimeout)
            )
        )
    }

    fun buildTaskStepReservationEntry(
        businessPartner: BusinessPartnerV6,
        recordId: String =  "any UUID",
        taskId: String = "any UUID"
    ): TaskStepReservationEntryDtoV6{
        return TaskStepReservationEntryDtoV6(
            taskId = taskId,
            recordId = recordId,
            businessPartner = businessPartner
        )
    }

}