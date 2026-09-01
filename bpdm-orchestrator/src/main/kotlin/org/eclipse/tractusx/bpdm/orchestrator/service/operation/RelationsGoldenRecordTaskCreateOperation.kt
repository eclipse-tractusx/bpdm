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
import org.eclipse.tractusx.bpdm.orchestrator.entity.RelationsGoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.RelationsGoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.repository.RelationsGoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.service.RelationsGoldenRecordTaskStateMachine
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RelationsGoldenRecordTaskCreateOperation(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository,
    private val taskRepository: RelationsGoldenRecordTaskRepository,
    private val relationsGoldenRecordTaskStateMachine: RelationsGoldenRecordTaskStateMachine
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun execute(
        mode: TaskMode,
        parsedList: List<RelationsGoldenRecordTaskCreateParsed>
    ): List<RelationsGoldenRecordTaskDb> {
        val gateRecords = parsedList.map { it.existingGateRecord ?: createGateRecord() }
        abortOutdatedTasks(gateRecords.toSet())

        val createdTasks = parsedList.zip(gateRecords)
            .map { (parsed, gateRecord) -> relationsGoldenRecordTaskStateMachine.initTask(mode, parsed.businessPartnerRelations, gateRecord) }

        if (createdTasks.isNotEmpty())
            logger.info { "Created ${createdTasks.size} relations golden record tasks in mode $mode: ${createdTasks.toLogIdentifiers()}" }

        return createdTasks
    }

    private fun createGateRecord(): SharingMemberRecordDb =
        sharingMemberRecordRepository.save(
            SharingMemberRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID(), isGoldenRecordCounted = true)
        )

    private fun abortOutdatedTasks(records: Set<SharingMemberRecordDb>) {
        val abortedTasks = taskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, RelationsGoldenRecordTaskDb.ResultState.Pending)
            .map { task -> relationsGoldenRecordTaskStateMachine.doAbortTask(task) }

        if (abortedTasks.isNotEmpty())
            logger.info { "Aborted ${abortedTasks.size} outdated relations golden record tasks: ${abortedTasks.toLogIdentifiers()}" }
    }

    private fun Collection<RelationsGoldenRecordTaskDb>.toLogIdentifiers() =
        map { it.uuid.toString() }.joinIdentifiersForLog()
}
