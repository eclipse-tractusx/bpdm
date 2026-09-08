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
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.eclipse.tractusx.bpdm.orchestrator.service.ResponseMapper
import org.eclipse.tractusx.orchestrator.api.model.TaskResultStateSearchRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskResultStateSearchResponse
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStateResponse
import org.springframework.stereotype.Service
import java.util.*

@Service
class GoldenRecordTaskQueryOperation(
    private val taskRepository: GoldenRecordTaskRepository,
    private val responseMapper: ResponseMapper,
    private val taskConfigProperties: TaskConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun searchTaskResultStates(stateRequest: TaskResultStateSearchRequest): TaskResultStateSearchResponse {
        logger.debug { "Search for ${stateRequest.taskIds.size} task result states" }

        val uuidsToSearch = stateRequest.taskIds.map { toUUID(it) }.toSet()
        val tasksByUuid = taskRepository.findByUuidIn(uuidsToSearch).associateBy { it.uuid }

        return TaskResultStateSearchResponse(uuidsToSearch
            .map { tasksByUuid[it]?.processingState?.resultState }
            .map { it?.let { responseMapper.toResultState(it) }
            })
    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        logger.debug { "Search for the state of golden record task: executing searchTaskStates() with parameters $stateRequest" }
        val requestsByTaskId = stateRequest.entries.associateBy { it.taskId }

        return stateRequest.entries.map { toUUID(it.taskId) }
            .let { uuids -> taskRepository.findByUuidIn(uuids.toSet()) }
            .also { tasks -> taskRepository.fetchBusinessPartnerData(tasks) }
            .filter { task -> requestsByTaskId[task.uuid.toString()]?.recordId == task.gateRecord.privateId.toString() }
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskStateResponse(tasks = it) }
    }

    private fun calculateTaskRetentionTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }
}
