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
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskCreateParsed
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStateMachine
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GoldenRecordTaskCreateOperation(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository,
    private val taskRepository: GoldenRecordTaskRepository,
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun execute(
        mode: TaskMode,
        parsedList: List<GoldenRecordTaskCreateParsed>,
        newGateRecordIsGoldenRecordCounted: Boolean?
    ): List<GoldenRecordTaskDb> {
        val gateRecords = parsedList.map { it.existingGateRecord ?: createGateRecord(newGateRecordIsGoldenRecordCounted) }
        abortOutdatedTasks(gateRecords.toSet())

        val createdTasks = parsedList.zip(gateRecords)
            .map { (parsed, gateRecord) -> goldenRecordTaskStateMachine.initTask(mode, parsed.businessPartner, gateRecord) }

        if (createdTasks.isNotEmpty())
            logger.info { "Created ${createdTasks.size} golden record tasks in mode $mode: ${createdTasks.toLogIdentifiers()}" }

        return createdTasks
    }

    private fun createGateRecord(isGoldenRecordCounted: Boolean?): SharingMemberRecordDb =
        sharingMemberRecordRepository.save(
            SharingMemberRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID(), isGoldenRecordCounted = isGoldenRecordCounted)
        )

    private fun abortOutdatedTasks(records: Set<SharingMemberRecordDb>) {
        val abortedTasks = taskRepository.findTasksByGateRecordInAndProcessingStateResultState(records, GoldenRecordTaskDb.ResultState.Pending)
            .map { task -> goldenRecordTaskStateMachine.doAbortTask(task) }

        if (abortedTasks.isNotEmpty())
            logger.info { "Aborted ${abortedTasks.size} outdated golden record tasks: ${abortedTasks.toLogIdentifiers()}" }
    }

    private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
        map { it.uuid.toString() }.joinIdentifiersForLog()
}
