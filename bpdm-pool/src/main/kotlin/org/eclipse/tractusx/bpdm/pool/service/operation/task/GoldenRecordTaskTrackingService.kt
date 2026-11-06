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
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskKind
import org.eclipse.tractusx.bpdm.pool.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.pool.util.countTasksForLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Tracks golden record and golden record relation tasks whose result has not reached the Orchestrator.
 */
@Service
class GoldenRecordTaskTrackingService(
    private val goldenRecordTaskRepository: GoldenRecordTaskRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Tracks the tasks of [kind] with [taskIds] as not reported to the Orchestrator.
     */
    @Transactional
    fun trackUnreported(kind: GoldenRecordTaskKind, taskIds: List<String>) {
        if (taskIds.isEmpty()) return

        goldenRecordTaskRepository.saveAll(taskIds.map { GoldenRecordTaskDb(it, kind) })
        logger.info { "Tracked ${countTasksForLog(taskIds.size, kind)} as unreported: ${taskIds.joinIdentifiersForLog()}" }
    }

    /**
     * Returns the tracked tasks of [kind] whose result has not reached the Orchestrator yet.
     */
    @Transactional(readOnly = true)
    fun findUnreported(kind: GoldenRecordTaskKind): List<GoldenRecordTaskDb> =
        goldenRecordTaskRepository.findByKind(kind)

    /**
     * Stops tracking [tasks], whose result no longer has to reach the Orchestrator.
     */
    @Transactional
    fun untrack(tasks: List<GoldenRecordTaskDb>) {
        if (tasks.isEmpty()) return

        goldenRecordTaskRepository.deleteAllInBatch(tasks)
        tasks.groupBy { it.kind }.forEach { (kind, untracked) ->
            logger.info { "Stopped tracking ${countTasksForLog(untracked.size, kind)}: ${untracked.map { it.taskId }.joinIdentifiersForLog()}" }
        }
    }
}
