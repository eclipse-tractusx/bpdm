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
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordRelationTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.AddressRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.LegalEntityRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.RelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.SiteRelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRelationTaskRaw
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordRelationTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.AddressRelationUpsertService
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.LegalEntityRelationUpsertService
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.SiteRelationUpsertService
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordRelationTaskParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Upserts the relations of the golden record relation tasks the Orchestrator holds for the Pool and reports each task's result back.
 */
@Service
class GoldenRecordRelationTaskResolutionApplicationService(
    private val taskGateway: GoldenRecordRelationTaskGateway,
    private val taskParser: GoldenRecordRelationTaskParser,
    private val legalEntityRelationUpsertService: LegalEntityRelationUpsertService,
    private val siteRelationUpsertService: SiteRelationUpsertService,
    private val addressRelationUpsertService: AddressRelationUpsertService,
    private val trackingService: GoldenRecordTaskTrackingService,
    private val outputMapper: DbToOutputMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {

    private val logger = KotlinLogging.logger { }

    // Each task commits on its own and the remote calls stay outside, so entries commit through a template rather than @Transactional.
    private val entryTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordRelationTaskResolutionApplicationService::class.qualifiedName}.resolveTasks")
    }

    /**
     * Resolves every relation task the Orchestrator holds for the Pool, one reservation at a time, until none is left.
     */
    fun resolveTasks() {
        var totalTasksProcessed = 0
        do {
            val reservedTasks = taskGateway.reserveTasks(taskProperties.batchSize)
            if (reservedTasks.isNotEmpty()) resolveReservation(reservedTasks)
            totalTasksProcessed += reservedTasks.size
        } while (reservedTasks.isNotEmpty())

        logger.debug { "Processed $totalTasksProcessed golden record relation tasks" }
    }

    private fun resolveReservation(reservedTasks: List<GoldenRecordRelationTaskRaw>) {
        try {
            taskGateway.resolveTasks(reservedTasks.map(::resolve))
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
            trackingService.trackUnreported(GoldenRecordTaskKind.Relations, reservedTasks.map { it.taskId })
        }
    }

    private fun resolve(task: GoldenRecordRelationTaskRaw): GoldenRecordRelationTaskResultRecord =
        try {
            entryTransaction.execute { parseAndUpsert(task) }!!
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            GoldenRecordRelationTaskResultRecord.Failed(task.taskId, task.relation)
        }

    private fun parseAndUpsert(task: GoldenRecordRelationTaskRaw): GoldenRecordRelationTaskResultRecord =
        when (val parseResult = taskParser.parse(task)) {
            is ParseResult.Success -> upsert(task.taskId, parseResult.parsed)
            is ParseResult.Failure -> GoldenRecordRelationTaskResultRecord.Rejected(task.taskId, task.relation, parseResult.errors)
        }

    private fun upsert(taskId: String, relation: RelationUpsertParsed): GoldenRecordRelationTaskResultRecord =
        when (relation) {
            is LegalEntityRelationUpsertParsed -> GoldenRecordRelationTaskResultRecord.LegalEntityRelationResolved(
                taskId, outputMapper.toRelationRecord(legalEntityRelationUpsertService.upsert(relation))
            )
            is SiteRelationUpsertParsed -> GoldenRecordRelationTaskResultRecord.SiteRelationResolved(
                taskId, outputMapper.toRelationRecord(siteRelationUpsertService.upsert(relation))
            )
            is AddressRelationUpsertParsed -> GoldenRecordRelationTaskResultRecord.AddressRelationResolved(
                taskId, outputMapper.toRelationRecord(addressRelationUpsertService.upsert(relation))
            )
        }
}
