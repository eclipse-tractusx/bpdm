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

import org.eclipse.tractusx.bpdm.common.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.model.error.GoldenRecordTaskResolveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskResolveRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.GoldenRecordTaskResolveOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.GoldenRecordTaskResolveParser
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoldenRecordTaskResolveApplicationService(
    private val parser: GoldenRecordTaskResolveParser,
    private val operation: GoldenRecordTaskResolveOperation
) {

    @Transactional
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        val resolveRequest = GoldenRecordTaskResolveRequest(
            step = resultRequest.step,
            results = resultRequest.results
        )

        parseAndExecuteAllOrNone(
            listOf(resolveRequest),
            { requests -> requests.flatMap { parser.parse(it) } },
            ::toValidationException,
            execute = { parsed -> operation.execute(parsed) }
        )
    }

    private fun toValidationException(errors: List<GoldenRecordTaskResolveParseError>): RuntimeException =
        when {
            errors.any { it is GoldenRecordTaskResolveParseError.InvalidBusinessPartner } ->
                BpdmInvalidBusinessPartnerException(
                    errors.filterIsInstance<GoldenRecordTaskResolveParseError.InvalidBusinessPartner>().first().message
                )

            errors.any { it is GoldenRecordTaskResolveParseError.TaskNotFound } ->
                BpdmTaskNotFoundException(
                    errors.filterIsInstance<GoldenRecordTaskResolveParseError.TaskNotFound>().first().taskId
                )

            else ->
                BpdmTaskNotFoundException(
                    errors.filterIsInstance<GoldenRecordTaskResolveParseError.TaskAborted>().first().taskId
                )
        }
}
