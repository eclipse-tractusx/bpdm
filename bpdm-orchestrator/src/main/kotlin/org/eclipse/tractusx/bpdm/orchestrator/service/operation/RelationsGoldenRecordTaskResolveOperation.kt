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

package org.eclipse.tractusx.bpdm.orchestrator.service.operation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.RelationsGoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.service.RelationsGoldenRecordTaskStateMachine
import org.eclipse.tractusx.bpdm.orchestrator.util.toRelationsLogIdentifiers
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RelationsGoldenRecordTaskResolveOperation(
    private val relationsGoldenRecordTaskStateMachine: RelationsGoldenRecordTaskStateMachine
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun execute(parsedList: List<RelationsGoldenRecordTaskResolveParsed>): List<RelationsGoldenRecordTaskDb> {
        val resolvedTasks = parsedList.mapNotNull { parsed ->
            val step = parsed.step
            val errors = parsed.resultEntry.errors
            val resultBusinessPartnerRelations = parsed.resultEntry.businessPartnerRelations

            when {
                errors.isNotEmpty() -> relationsGoldenRecordTaskStateMachine.doResolveTaskToError(parsed.task, step, errors)
                else -> relationsGoldenRecordTaskStateMachine.resolveTaskStepToSuccess(parsed.task, step, resultBusinessPartnerRelations)
            }
        }

        logResolvedTasks(resolvedTasks, parsedList.firstOrNull()?.step)

        return resolvedTasks
    }

    private fun logResolvedTasks(resolvedTasks: List<RelationsGoldenRecordTaskDb>, step: org.eclipse.tractusx.orchestrator.api.model.TaskStep?) {
        if (step == null) return

        val tasksByResultState = resolvedTasks.groupBy { it.processingState.resultState }

        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Pending]
            ?.groupBy { it.processingState.step }
            ?.forEach { (nextStep, tasks) ->
                logger.info { "Advanced ${tasks.size} relation golden record tasks from step $step to step $nextStep: ${tasks.toRelationsLogIdentifiers()}" }
            }
        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Success]
            ?.let { tasks -> logger.info { "Completed ${tasks.size} relation golden record tasks after step $step: ${tasks.toRelationsLogIdentifiers()}" } }
        tasksByResultState[RelationsGoldenRecordTaskDb.ResultState.Error]
            ?.let { tasks -> logger.info { "Failed ${tasks.size} relation golden record tasks in step $step: ${tasks.toRelationsLogIdentifiers()}" } }
    }
}
