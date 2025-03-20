/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.GateRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GateRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class RelationsGoldenRecordTaskService(
    private val relationsGoldenRecordTaskStateMachine: RelationsGoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val relationsResponseMapper: RelationsResponseMapper,
    private val relationsTaskRepository: RelationsGoldenRecordTaskRepository,
    private val gateRecordRepository: GateRecordRepository,
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasks(createRequest: TaskCreateRelationsRequest): TaskCreateRelationsResponse {
        logger.debug { "Creation of new relations golden record tasks: executing createTasks() with parameters $createRequest" }

        val gateRecords = getOrCreateGateRecords(createRequest.requests)
        abortOutdatedTasks(gateRecords.toSet())

        return createRequest.requests.zip(gateRecords)
            .map { (request, record) -> relationsGoldenRecordTaskStateMachine.initTask(createRequest.mode, request.businessPartnerRelations, record) }
            .map { task -> relationsResponseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateRelationsResponse(createdTasks = it) }
    }

    private fun getOrCreateGateRecords(requests: List<TaskCreateRelationsRequestEntry>): List<GateRecordDb> {
        val privateIds = requests.map { request -> request.recordId?.let { toUUID(it) } }
        val notNullPrivateIds = privateIds.filterNotNull()

        val foundRecords = gateRecordRepository.findByPrivateIdIn(notNullPrivateIds.toSet())
        val foundRecordsByPrivateId = foundRecords.associateBy { it.privateId }
        val requestedNotFoundRecords = notNullPrivateIds.minus(foundRecordsByPrivateId.keys)

        if (requestedNotFoundRecords.isNotEmpty())
            throw BpdmRecordNotFoundException(requestedNotFoundRecords)

        return privateIds.map { privateId ->
            val gateRecord = privateId?.let { foundRecordsByPrivateId[it] } ?: GateRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID())
            gateRecordRepository.save(gateRecord)
        }
    }

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }

    private fun abortOutdatedTasks(records: Set<GateRecordDb>){
        return relationsTaskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, RelationsGoldenRecordTaskDb.ResultState.Pending)
            .forEach { task -> relationsGoldenRecordTaskStateMachine.doAbortTask(task) }
    }

    private fun calculateTaskRetentionTimeout(task: RelationsGoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

}