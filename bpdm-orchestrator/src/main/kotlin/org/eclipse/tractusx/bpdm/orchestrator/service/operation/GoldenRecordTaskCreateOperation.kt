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
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.service.application.SharingMemberRecordApplicationService
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GoldenRecordTaskCreateOperation(
    private val stateMachine: GoldenRecordTaskStateMachine,
    private val taskRepository: GoldenRecordTaskRepository,
    private val sharingMemberRecordService: SharingMemberRecordApplicationService
) {
    private val logger = KotlinLogging.logger { }

    fun create(parsed: List<GoldenRecordTaskCreateParsed>): List<GoldenRecordTaskDb> {
        // Get or create gate records for entries without resolved records
        val entriesToCreate = parsed.filter { it.gateRecord == null }
        val createdRecords = if (entriesToCreate.isNotEmpty()) {
            sharingMemberRecordService.getOrCreateGateRecords(entriesToCreate.map { TaskCreateRequestEntry(recordId = null, businessPartner = it.businessPartner) })
        } else {
            emptyList()
        }

        // Merge resolved and created records
        var createdIndex = 0
        val gateRecords = parsed.map { entry ->
            if (entry.gateRecord != null) entry.gateRecord else createdRecords[createdIndex++]
        }

        abortOutdatedTasks(gateRecords.toSet())

        return parsed.zip(gateRecords)
            .map { (entry, record) -> stateMachine.initTask(entry.mode, entry.businessPartner, record) }
            .also { createdTasks ->
                if (createdTasks.isNotEmpty()) {
                    logger.info { "Created ${createdTasks.size} golden record tasks in mode ${parsed.first().mode}: ${createdTasks.toLogIdentifiers()}" }
                }
            }
    }

    private fun abortOutdatedTasks(records: Set<SharingMemberRecordDb>) {
        val abortedTasks = taskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, GoldenRecordTaskDb.ResultState.Pending)
            .map { task -> stateMachine.doAbortTask(task) }

        if (abortedTasks.isNotEmpty()) {
            logger.info { "Aborted ${abortedTasks.size} outdated golden record tasks: ${abortedTasks.toLogIdentifiers()}" }
        }
    }
}

private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
    map { it.uuid.toString() }.joinIdentifiersForLog()
