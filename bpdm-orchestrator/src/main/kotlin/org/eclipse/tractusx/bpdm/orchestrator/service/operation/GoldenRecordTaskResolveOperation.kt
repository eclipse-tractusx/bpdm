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
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStateMachine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Collection

@Service
class GoldenRecordTaskResolveOperation(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun execute(parsedList: List<GoldenRecordTaskResolveParsed>): List<GoldenRecordTaskDb> {
        val resolvedTasks = parsedList.mapNotNull { parsed ->
            val step = parsed.step
            val errors = parsed.resultEntry.errors
            val resultBusinessPartner = parsed.resultEntry.businessPartner

            when {
                errors.isNotEmpty() -> goldenRecordTaskStateMachine.doResolveTaskToError(parsed.task, step, errors)
                else -> goldenRecordTaskStateMachine.resolveTaskStepToSuccess(parsed.task, step, resultBusinessPartner)
            }
        }

        logResolvedTasks(resolvedTasks, parsedList.firstOrNull()?.step)

        return resolvedTasks
    }

    private fun logResolvedTasks(resolvedTasks: List<GoldenRecordTaskDb>, step: org.eclipse.tractusx.orchestrator.api.model.TaskStep?) {
        if (step == null) return

        val tasksByResultState = resolvedTasks.groupBy { it.processingState.resultState }

        tasksByResultState[GoldenRecordTaskDb.ResultState.Pending]
            ?.groupBy { it.processingState.step }
            ?.forEach { (nextStep, tasks) ->
                logger.info { "Advanced ${tasks.size} golden record tasks from step $step to step $nextStep: ${toLogIdentifiers(tasks as Collection<GoldenRecordTaskDb>)}" }
            }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Success]
            ?.let { tasks -> logger.info { "Completed ${tasks.size} golden record tasks after step $step: ${toLogIdentifiers(tasks as Collection<GoldenRecordTaskDb>)}" } }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Error]
            ?.let { tasks -> logger.info { "Failed ${tasks.size} golden record tasks in step $step: ${toLogIdentifiers(tasks as Collection<GoldenRecordTaskDb>)}" } }
    }

    private fun toLogIdentifiers(tasks: Collection<GoldenRecordTaskDb>): String =
        tasks.map { it.uuid.toString() }.joinIdentifiersForLog()
}
