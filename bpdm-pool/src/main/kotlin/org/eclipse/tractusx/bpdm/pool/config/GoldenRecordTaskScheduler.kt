/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.config

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.service.TaskRelationsBatchResolutionService
import org.eclipse.tractusx.bpdm.pool.service.application.GoldenRecordTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.UnreportedTaskReportApplicationService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import java.time.ZoneOffset

/**
 * Schedules the processing of golden record and golden record relation tasks.
 */
@Configuration
class GoldenRecordTaskScheduler(
    private val configProperties: GoldenRecordTaskConfigProperties,
    private val taskScheduler: TaskScheduler,
    private val taskResolutionService: GoldenRecordTaskResolutionApplicationService,
    private val relationTaskResolutionService: TaskRelationsBatchResolutionService,
    private val unreportedTaskReportService: UnreportedTaskReportApplicationService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Schedules golden record task processing on the configured cron, unless it is disabled.
     */
    @PostConstruct
    fun scheduleGoldenRecordTasks() {
        if (configProperties.cron == CRON_DISABLED) {
            logger.info { "Golden record and relation task processing schedule is disabled" }
            return
        }

        logger.info { "Golden record and relation task processing scheduled with cron '${configProperties.cron}'" }
        taskScheduler.schedule(::processTasks, CronTrigger(configProperties.cron, ZoneOffset.UTC))
    }

    /**
     * Resolves the reserved golden record and relation tasks, then reports every task whose result did not reach the Orchestrator as an error; a failing step does not stop the ones after it.
     */
    fun processTasks() {
        logger.debug { "Start golden record task processing schedule..." }
        runStep("golden record task resolution", taskResolutionService::resolveTasks)
        runStep("golden record relation task resolution", relationTaskResolutionService::reserveAndResolve)
        runStep("unreported task report", unreportedTaskReportService::reportUnreportedTasks)
    }

    private fun runStep(name: String, step: () -> Unit) {
        try {
            step()
        } catch (ex: Exception) {
            logger.error(ex) { "The $name failed, continuing with the next step" }
        }
    }

    companion object {
        private const val CRON_DISABLED = "-"
    }
}
