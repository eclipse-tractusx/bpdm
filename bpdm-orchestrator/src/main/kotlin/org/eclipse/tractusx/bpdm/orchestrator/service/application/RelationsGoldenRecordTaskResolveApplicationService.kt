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
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmInvalidBusinessPartnerException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.model.error.RelationsGoldenRecordTaskResolveParseError
import org.eclipse.tractusx.bpdm.orchestrator.model.parsed.RelationsGoldenRecordTaskResolveParsed
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationsGoldenRecordTaskResolveRequest
import org.eclipse.tractusx.bpdm.orchestrator.service.operation.RelationsGoldenRecordTaskResolveOperation
import org.eclipse.tractusx.bpdm.orchestrator.service.parser.RelationsGoldenRecordTaskResolveParser
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RelationsGoldenRecordTaskResolveApplicationService(
    private val parser: RelationsGoldenRecordTaskResolveParser,
    private val operation: RelationsGoldenRecordTaskResolveOperation
) {

    @Transactional
    fun resolveStepResults(resultRequest: TaskRelationsStepResultRequest) {
        val resolveRequest = RelationsGoldenRecordTaskResolveRequest(
            step = resultRequest.step,
            results = resultRequest.results
        )

        val parseResults = parser.parse(resolveRequest)
        val errors = parseResults.filterIsInstance<ParseResult.Failure<RelationsGoldenRecordTaskResolveParseError>>()
            .flatMap { it.errors }
            .filterNot { it is RelationsGoldenRecordTaskResolveParseError.TaskAborted }

        if (errors.isNotEmpty()) throw toValidationException(errors)

        val parsed = parseResults.filterIsInstance<ParseResult.Success<RelationsGoldenRecordTaskResolveParsed>>()
            .map { it.parsed }
        operation.execute(parsed)
    }

    private fun toValidationException(errors: List<RelationsGoldenRecordTaskResolveParseError>): RuntimeException =
        when {
            errors.any { it is RelationsGoldenRecordTaskResolveParseError.InvalidBusinessPartnerRelations } ->
                BpdmInvalidBusinessPartnerException(
                    errors.filterIsInstance<RelationsGoldenRecordTaskResolveParseError.InvalidBusinessPartnerRelations>().first().message
                )

            else ->
                BpdmTaskNotFoundException(
                    errors.filterIsInstance<RelationsGoldenRecordTaskResolveParseError.TaskNotFound>().first().taskId
                )
        }
}
