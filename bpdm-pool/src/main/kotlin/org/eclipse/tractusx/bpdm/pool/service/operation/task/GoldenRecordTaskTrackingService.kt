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

package org.eclipse.tractusx.bpdm.pool.service.operation.task

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.pool.repository.GoldenRecordTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Tracks golden record tasks whose result has not reached the Orchestrator.
 */
@Service
class GoldenRecordTaskTrackingService(
    private val goldenRecordTaskRepository: GoldenRecordTaskRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Tracks the tasks with [taskIds] as not reported to the Orchestrator.
     */
    @Transactional
    fun trackUnreported(taskIds: List<String>) {
        if (taskIds.isEmpty()) return

        // Marked resolved as the job always did, so these tasks are never reported; fixing that is a behaviour change of its own.
        goldenRecordTaskRepository.saveAll(taskIds.map { GoldenRecordTaskDb(it, true, Instant.now()) })
        logger.info { "Tracked ${countForLog(taskIds.size, "golden record task", "golden record tasks")} as unreported: ${taskIds.joinIdentifiersForLog()}" }
    }
}
