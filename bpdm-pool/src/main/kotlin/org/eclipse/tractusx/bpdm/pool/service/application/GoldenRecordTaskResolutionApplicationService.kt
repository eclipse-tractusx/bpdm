/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskKind
import org.eclipse.tractusx.bpdm.pool.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.pool.model.output.task.AdditionalSiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.UpsertedGoldenRecordParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordTaskRaw
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskUpsertService
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordTaskParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Upserts the golden records of the tasks the Orchestrator holds for the Pool and reports each task's result back.
 */
@Service
class GoldenRecordTaskResolutionApplicationService(
    private val taskGateway: GoldenRecordTaskGateway,
    private val taskParser: GoldenRecordTaskParser,
    private val upsertService: GoldenRecordTaskUpsertService,
    private val trackingService: GoldenRecordTaskTrackingService,
    private val outputMapper: DbToOutputMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {

    private val logger = KotlinLogging.logger { }

    // Each task commits on its own and the remote calls stay outside, so entries commit through a template rather than @Transactional.
    private val entryTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordTaskResolutionApplicationService::class.qualifiedName}.resolveTasks")
    }

    /**
     * Resolves every task the Orchestrator holds for the Pool, one reservation at a time, until none is left.
     */
    fun resolveTasks() {
        var totalTasksProcessed = 0
        do {
            val reservedTasks = taskGateway.reserveTasks(taskProperties.batchSize)
            if (reservedTasks.isNotEmpty()) resolveReservation(reservedTasks)
            totalTasksProcessed += reservedTasks.size
        } while (reservedTasks.isNotEmpty())

        logger.debug { "Processed $totalTasksProcessed golden record tasks" }
    }

    private fun resolveReservation(reservedTasks: List<GoldenRecordTaskRaw>) {
        try {
            taskGateway.resolveTasks(reservedTasks.map(::resolve))
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
            trackingService.trackUnreported(GoldenRecordTaskKind.BusinessPartner, reservedTasks.map { it.taskId })
        }
    }

    private fun resolve(task: GoldenRecordTaskRaw): GoldenRecordTaskResultRecord =
        try {
            entryTransaction.execute { parseAndUpsert(task) }!!
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            GoldenRecordTaskResultRecord.Failed(task.taskId)
        }

    private fun parseAndUpsert(task: GoldenRecordTaskRaw): GoldenRecordTaskResultRecord =
        when (val parseResult = taskParser.parse(task)) {
            is ParseResult.Success -> toResolved(task, parseResult.parsed, upsertService.upsert(parseResult.parsed))
            is ParseResult.Failure -> GoldenRecordTaskResultRecord.Rejected(task.taskId, parseResult.errors)
        }

    private fun toResolved(task: GoldenRecordTaskRaw, plan: GoldenRecordTaskParsed, upserted: UpsertedGoldenRecordParsed) =
        GoldenRecordTaskResultRecord.Resolved(
            taskId = task.taskId,
            legalEntity = outputMapper.toRecord(upserted.legalEntity),
            isLegalEntityWritten = plan.legalEntity !is LegalEntityUpsertParsed.Unchanged,
            site = upserted.site?.let { outputMapper.toRecord(it) },
            isSiteWritten = plan.site != null && plan.site !is SiteUpsertParsed.Unchanged,
            additionalAddress = upserted.additionalAddress?.let { outputMapper.toRecord(it) },
            additionalSites = upserted.additionalSites.map { AdditionalSiteRecord(it.bpn, it.name) },
            passThrough = task.passThrough
        )
}
