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

package org.eclipse.tractusx.bpdm.orchestrator.service.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskReserveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.GoldenRecordTaskReserveParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskReserveRequest
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GoldenRecordTaskReserveParser(
    private val taskRepository: GoldenRecordTaskRepository
) {

    fun parse(reserveRequest: GoldenRecordTaskReserveRequest): ParseResult<GoldenRecordTaskReserveParsed, GoldenRecordTaskReserveParseError> {
        val foundTasks = taskRepository.findByStepAndStepState(
            reserveRequest.step,
            org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb.StepState.Queued,
            Pageable.ofSize(reserveRequest.amount)
        ).content.toSet()
            .also { taskRepository.fetchBusinessPartnerData(it) }

        return ParseResult.Success(
            GoldenRecordTaskReserveParsed(
                step = reserveRequest.step,
                amount = reserveRequest.amount,
                foundTasks = foundTasks
            )
        )
    }
}
