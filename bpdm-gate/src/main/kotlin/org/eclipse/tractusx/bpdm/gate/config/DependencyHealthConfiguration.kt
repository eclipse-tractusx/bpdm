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

package org.eclipse.tractusx.bpdm.gate.config

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger

/**
 * Guards the Gate against unreachable Pool and Orchestrator services, at startup and on the configured schedule.
 */
@Configuration
class DependencyHealthConfiguration(
    poolHealthIndicator: PoolHealthIndicator,
    orchestratorHealthIndicator: OrchestratorHealthIndicator,
    private val taskScheduler: TaskScheduler,
    private val configProperties: GoldenRecordTaskConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    private val dependencies: Map<String, HealthIndicator> = mapOf(
        "Pool Service" to poolHealthIndicator,
        "Orchestrator Service" to orchestratorHealthIndicator
    )

    /**
     * Fails the application startup when any dependency is unreachable.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun checkDependenciesOnStartup() {
        val unavailableDependencies = findUnavailableDependencies()
        check(unavailableDependencies.isEmpty()) { "Dependencies not ready: ${unavailableDependencies.joinToString()}" }

        logger.info { "All dependencies are healthy on startup: ${dependencies.keys.joinToString()}" }
    }

    /**
     * Schedules the recurring dependency check unless its cron is disabled.
     */
    @PostConstruct
    fun scheduleDependencyCheck() {
        val cronExpression = configProperties.dependencyCheck.cron
        if (cronExpression == CRON_DISABLED) {
            logger.info { "Schedule '$SCHEDULE_NAME' is disabled" }
            return
        }

        logger.info { "Schedule '$SCHEDULE_NAME' activated with cron '$cronExpression'" }
        taskScheduler.schedule({ checkDependencies() }, CronTrigger(cronExpression))
    }

    private fun checkDependencies() {
        val unavailableDependencies = findUnavailableDependencies()
        if (unavailableDependencies.isNotEmpty()) {
            logger.error { "Dependencies not ready: ${unavailableDependencies.joinToString()}" }
        } else {
            logger.debug { "All dependencies are healthy: ${dependencies.keys.joinToString()}" }
        }
    }

    private fun findUnavailableDependencies(): List<String> =
        dependencies.filterValues { it.health()?.status != Status.UP }.keys.toList()

    companion object {
        private const val SCHEDULE_NAME = "dependency health check"
        private const val CRON_DISABLED = "-"
    }
}
