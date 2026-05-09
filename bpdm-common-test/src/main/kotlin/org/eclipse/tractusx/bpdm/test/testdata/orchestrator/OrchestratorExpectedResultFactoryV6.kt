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

import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepReservationEntryDto
import java.time.Duration
import java.time.Instant

class OrchestratorExpectedResultFactoryV6(
    val pendingTimeout: Duration,
    val retentionTimeout: Duration,
    val taskStepTransitions: Map<TaskMode, List<TaskStep>>
) {

    fun buildCreatedTaskClientState(
        businessPartner: BusinessPartner,
        taskMode: TaskMode,
        taskId: String = "any UUID",
        recordId: String = "any UUID",
        modifiedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now()
    ): TaskClientStateDto{

        return TaskClientStateDto(
            taskId = taskId,
            recordId = recordId,
            businessPartnerResult = businessPartner,
            processingState = TaskProcessingStateDto(
                resultState = ResultState.Pending,
                step = taskStepTransitions[taskMode]!!.first(),
                stepState = StepState.Queued,
                errors = emptyList(),
                modifiedAt = modifiedAt,
                createdAt = createdAt,
                timeout = createdAt.plus(retentionTimeout)
            )
        )
    }

    fun buildTaskStepReservationEntry(
        businessPartner: BusinessPartner,
        recordId: String =  "any UUID",
        taskId: String = "any UUID"
    ): TaskStepReservationEntryDto{
        return TaskStepReservationEntryDto(
            taskId = taskId,
            recordId = recordId,
            businessPartner = businessPartner
        )
    }

}