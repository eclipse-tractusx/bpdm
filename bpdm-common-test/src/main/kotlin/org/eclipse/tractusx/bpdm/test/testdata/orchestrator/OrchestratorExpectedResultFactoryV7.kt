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

import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.ResultStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.StepStateV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskClientRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskClientStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskModeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskProcessingRelationsStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskProcessingStateV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import java.time.Duration
import java.time.Instant

class OrchestratorExpectedResultFactoryV7(
    val pendingTimeout: Duration,
    val retentionTimeout: Duration,
    val taskStepTransitions: Map<TaskModeV7, List<TaskStepV7>>
) {

    fun buildCreatedBusinessPartnerTaskClientState(
        businessPartner: BusinessPartnerV7Dto,
        taskMode: TaskModeV7,
        taskId: String = "any UUID",
        recordId: String = "any UUID",
        modifiedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now()
    ): TaskClientStateV7Dto {

        return TaskClientStateV7Dto(
            taskId = taskId,
            recordId = recordId,
            businessPartnerResult = businessPartner,
            processingState = TaskProcessingStateV7Dto(
                resultState = ResultStateV7.Pending,
                step = taskStepTransitions[taskMode]!!.first(),
                stepState = StepStateV7.Queued,
                errors = emptyList(),
                modifiedAt = modifiedAt,
                createdAt = createdAt,
                timeout = createdAt.plus(retentionTimeout)
            )
        )
    }

    fun buildCreatedRelationTaskClientState(
        relation: BusinessPartnerRelationsV7Dto,
        taskMode: TaskModeV7,
        taskId: String = "any UUID",
        recordId: String = "any UUID",
        modifiedAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now()
    ): TaskClientRelationsStateV7Dto{
        return TaskClientRelationsStateV7Dto(
            taskId = taskId,
            recordId = recordId,
            businessPartnerRelationsResult = relation,
            processingState = TaskProcessingRelationsStateV7Dto(
                resultState = ResultStateV7.Pending,
                step = taskStepTransitions[taskMode]!!.first(),
                stepState = StepStateV7.Queued,
                errors = emptyList(),
                modifiedAt = modifiedAt,
                createdAt = createdAt,
                timeout = createdAt.plus(retentionTimeout)
            )
        )
    }

}