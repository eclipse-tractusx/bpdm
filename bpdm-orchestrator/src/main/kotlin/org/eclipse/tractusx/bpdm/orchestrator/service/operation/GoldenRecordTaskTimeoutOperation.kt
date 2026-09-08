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
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStateMachine
import org.eclipse.tractusx.bpdm.orchestrator.service.PaginationInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GoldenRecordTaskTimeoutOperation(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val taskRepository: GoldenRecordTaskRepository
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun processPendingTimeouts(pageSize: Int): PaginationInfo {
        val timedOutTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStatePendingTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                goldenRecordTaskStateMachine.doResolveTaskToTimeout(task)
                timedOutTasks.add(task)
            }
        ).also {
            if (timedOutTasks.isNotEmpty())
                logger.info { "Timed out ${timedOutTasks.size} golden record tasks: ${timedOutTasks.toLogIdentifiers()}" }
        }
    }

    @Transactional
    fun processRetentionTimeouts(pageSize: Int): PaginationInfo {
        val deletedTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStateRetentionTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                taskRepository.delete(task)
                deletedTasks.add(task)
            }
        ).also {
            if (deletedTasks.isNotEmpty())
                logger.info { "Deleted ${deletedTasks.size} golden record tasks after their retention timeout: ${deletedTasks.toLogIdentifiers()}" }
        }
    }

    private fun batchProcessTasks(
        pageSize: Int,
        fetchPage: (Pageable) -> Page<GoldenRecordTaskDb>,
        processTask: (GoldenRecordTaskDb) -> Unit
    ): PaginationInfo {
        val pageable: Pageable = PageRequest.of(0, pageSize)
        val page = fetchPage(pageable)
        var hasProcessedTasks = false
        var processedTaskCount = 0

        page.forEach { task ->
            try {
                processTask(task)
                hasProcessedTasks = true
                processedTaskCount++ // Increment on successful processing
            } catch (err: RuntimeException) {
                logger.error(err) { "Error processing timeout for task ${task.uuid}" }
            }
        }

        return PaginationInfo(hasProcessedTasks, page.hasNext(), processedTaskCount)
    }

    private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
        map { it.uuid.toString() }.joinIdentifiersForLog()
}
