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

package org.eclipse.tractusx.bpdm.pool.service.application.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.SuccessionUpsertRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.SuccessionParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.SuccessionResultMapper
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntitySuccessionParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteSuccessionParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SuccessionUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.AddressSuccessionUpsertService
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.LegalEntitySuccessionUpsertService
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.SiteSuccessionUpsertService
import org.eclipse.tractusx.bpdm.pool.service.parser.relation.SuccessionUpsertParser
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Answers a reserved golden record relation task that states a succession with the succession as it now stands.
 */
@Service
class SuccessionUpsertApplicationService(
    private val upsertRequestMapper: SuccessionUpsertRequestMapper,
    private val upsertParser: SuccessionUpsertParser,
    private val legalEntitySuccessionUpsertService: LegalEntitySuccessionUpsertService,
    private val siteSuccessionUpsertService: SiteSuccessionUpsertService,
    private val addressSuccessionUpsertService: AddressSuccessionUpsertService,
    private val parseErrorMapper: SuccessionParseErrorMapper,
    private val resultMapper: SuccessionResultMapper
) {

    /**
     * Writes the succession the task states and reports it back, or reports that task's reasons for not writing it.
     */
    @Transactional
    fun upsert(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto =
        when (val result = upsertParser.parse(upsertRequestMapper.toRequest(taskEntry.businessPartnerRelations))) {
            is ParseResult.Failure -> toErrorReply(taskEntry, result.errors)
            is ParseResult.Success -> toSuccessReply(taskEntry, result.parsed)
        }

    private fun toErrorReply(
        taskEntry: TaskRelationsStepReservationEntryDto,
        errors: List<SuccessionUpsertParseError>
    ): TaskRelationsStepResultEntryDto =
        TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartnerRelations = taskEntry.businessPartnerRelations,
            errors = errors.map { TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, parseErrorMapper.toUpsertDescription(it)) }
        )

    private fun toSuccessReply(
        taskEntry: TaskRelationsStepReservationEntryDto,
        parsed: SuccessionUpsertParsed
    ): TaskRelationsStepResultEntryDto =
        TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartnerRelations = write(parsed),
            errors = emptyList()
        )

    private fun write(parsed: SuccessionUpsertParsed): BusinessPartnerRelations =
        when (parsed) {
            is LegalEntitySuccessionParsed -> resultMapper.toTaskResult(legalEntitySuccessionUpsertService.upsert(parsed).value)
            is SiteSuccessionParsed -> resultMapper.toTaskResult(siteSuccessionUpsertService.upsert(parsed).value)
            is AddressSuccessionParsed -> resultMapper.toTaskResult(addressSuccessionUpsertService.upsert(parsed).value)
        }
}
