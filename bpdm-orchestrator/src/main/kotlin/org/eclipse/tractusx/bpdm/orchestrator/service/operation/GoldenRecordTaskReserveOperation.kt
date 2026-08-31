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
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskReserveParsed
import org.eclipse.tractusx.bpdm.orchestrator.service.GoldenRecordTaskStateMachine
import org.eclipse.tractusx.bpdm.orchestrator.service.ResponseMapper
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GoldenRecordTaskReserveOperation(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val responseMapper: ResponseMapper
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun execute(parsed: GoldenRecordTaskReserveParsed): TaskStepReservationResponse {
        val reservedTasks = parsed.foundTasks.map { goldenRecordTaskStateMachine.doReserve(it) }
        val now = Instant.now()
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        if (reservedTasks.isNotEmpty())
            logger.debug { "Reserved ${reservedTasks.size} golden record tasks for step ${parsed.step}: ${reservedTasks.toLogIdentifiers()}" }

        return reservedTasks
            .map { task ->
                TaskStepReservationEntryDto(
                    task.uuid.toString(),
                    task.gateRecord.publicId.toString(),
                    responseMapper.toBusinessPartnerResult(task.businessPartner)
                )
            }
            .let { reservations -> TaskStepReservationResponse(reservations, pendingTimeout) }
    }

    private fun calculateTaskPendingTimeout(task: GoldenRecordTaskDb): Instant =
        task.createdAt.instant.plus(java.time.Duration.ofHours(24)) // Default timeout, should be configurable

    private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
        map { it.uuid.toString() }.joinIdentifiersForLog()
}
