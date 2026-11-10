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

import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskMode
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStep
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.businesspartner.BusinessPartnerV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.ResultStateV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.StepStateV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskClientStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskProcessingStateV6Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepReservationEntryV6Dto
import java.time.Duration
import java.time.Instant

class OrchestratorExpectedResultFactoryV6(
    val pendingTimeout: Duration,
    val retentionTimeout: Duration,
    val taskStepTransitions: Map<TaskMode, List<TaskStep>>
) {

    fun buildCreatedTaskClientState(
        businessPartner: BusinessPartnerV6Dto,
        taskMode: TaskMode,
        taskId: String = "any UUID",
        recordId: String = "any UUID",
        modifiedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now()
    ): TaskClientStateV6Dto{

        return TaskClientStateV6Dto(
            taskId = taskId,
            recordId = recordId,
            businessPartnerResult = businessPartner,
            processingState = TaskProcessingStateV6Dto(
                resultState = ResultStateV6.Pending,
                step = when (taskStepTransitions[taskMode]!!.first()) {
                    TaskStep.CleanAndSync -> org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepV6.CleanAndSync
                    TaskStep.PoolSync -> org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepV6.PoolSync
                    TaskStep.Clean -> org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepV6.Clean
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
        businessPartner: BusinessPartnerV6Dto,
        recordId: String =  "any UUID",
        taskId: String = "any UUID"
    ): TaskStepReservationEntryV6Dto{
        return TaskStepReservationEntryV6Dto(
            taskId = taskId,
            recordId = recordId,
            businessPartner = businessPartner
        )
    }

}