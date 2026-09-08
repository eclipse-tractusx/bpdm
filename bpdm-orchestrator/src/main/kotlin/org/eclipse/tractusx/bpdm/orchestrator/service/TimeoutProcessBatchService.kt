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

package org.eclipse.tractusx.bpdm.orchestrator.service

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.TimeoutProcessBatchOperation
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TimeoutProcessBatchService(
    private val batchOperation: TimeoutProcessBatchOperation,
    private val taskConfigProperties: TaskConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Reports whether task timeout processing runs on a schedule in this deployment.
     */
    @PostConstruct
    fun logScheduleActivation() {
        if (taskConfigProperties.timeoutCheckCron == CRON_DISABLED)
            logger.info { "Task timeout processing schedule is disabled" }
        else
            logger.info { "Task timeout processing scheduled with cron '${taskConfigProperties.timeoutCheckCron}'" }
    }

    @Scheduled(cron = "\${bpdm.task.timeoutCheckCron}")
    fun processForTimeouts() {
        batchOperation.processForTimeouts()
    }

    companion object {
        private const val CRON_DISABLED = "-"
    }
}
