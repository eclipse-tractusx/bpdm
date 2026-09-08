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

package org.eclipse.tractusx.bpdm.orchestrator.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskReserveRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.GoldenRecordTaskReserveOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskReserveParser
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoldenRecordTaskReserveApplicationService(
    private val parser: GoldenRecordTaskReserveParser,
    private val operation: GoldenRecordTaskReserveOperation
) {

    @Transactional
    fun reserveTasksForStep(reservationRequest: GoldenRecordTaskReserveRequest): TaskStepReservationResponse {
        val parseResult = parser.parse(reservationRequest)
        
        return when (parseResult) {
            is ParseResult.Success -> operation.execute(parseResult.parsed)
            is ParseResult.Failure -> throw IllegalStateException("Failed to parse reserve request")
        }
    }
}
