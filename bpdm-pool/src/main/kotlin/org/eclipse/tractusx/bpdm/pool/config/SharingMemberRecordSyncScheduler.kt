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
import org.eclipse.tractusx.bpdm.pool.service.application.SharingMemberRecordSyncApplicationService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import java.time.ZoneOffset

/**
 * Schedules the synchronisation of sharing member records from the Orchestrator.
 */
@Configuration
class SharingMemberRecordSyncScheduler(
    private val configProperties: SharingMemberRecordSyncConfigProperties,
    private val taskScheduler: TaskScheduler,
    private val syncService: SharingMemberRecordSyncApplicationService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Schedules sharing member record synchronisation on the configured cron, unless it is disabled.
     */
    @PostConstruct
    fun scheduleSynchronization() {
        if (configProperties.cron == CRON_DISABLED) {
            logger.info { "Sharing member record synchronization schedule is disabled" }
            return
        }

        logger.info { "Sharing member record synchronization scheduled with cron '${configProperties.cron}'" }
        taskScheduler.schedule(syncService::synchronize, CronTrigger(configProperties.cron, ZoneOffset.UTC))
    }

    companion object {
        private const val CRON_DISABLED = "-"
    }
}
