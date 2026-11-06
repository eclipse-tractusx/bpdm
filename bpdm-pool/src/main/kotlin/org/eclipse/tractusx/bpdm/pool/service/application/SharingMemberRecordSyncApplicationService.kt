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
import org.eclipse.tractusx.bpdm.pool.config.SharingMemberRecordSyncConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordCountedUpdateError
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.SharingMemberRecordPageRaw
import org.eclipse.tractusx.bpdm.pool.service.gateway.SharingMemberRecordGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.SharingMemberConfidenceService
import org.eclipse.tractusx.bpdm.pool.service.operation.sync.SyncRecordOperationService
import org.eclipse.tractusx.bpdm.pool.service.parser.participation.GoldenRecordCountedUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Brings the Pool's sharing member records up to date with whether the Orchestrator counts them towards their golden record.
 */
@Service
class SharingMemberRecordSyncApplicationService(
    private val sharingMemberRecordGateway: SharingMemberRecordGateway,
    private val updateParser: GoldenRecordCountedUpdateParser,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val syncRecordOperation: SyncRecordOperationService,
    private val syncProperties: SharingMemberRecordSyncConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Orchestrator reads stay outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${SharingMemberRecordSyncApplicationService::class.qualifiedName}.synchronize")
    }

    /**
     * Applies every sharing member record change the Orchestrator recorded since the previous synchronisation,
     * committing one page at a time.
     */
    fun synchronize() {
        do {
            val page = sharingMemberRecordGateway.fetchChangesAfter(
                syncRecordOperation.getSyncedUntil(SyncTypeDb.SHARING_MEMBER_RECORDS),
                syncProperties.batchSize
            )
            update(page)
        } while (page.hasMore)
    }

    private fun update(page: SharingMemberRecordPageRaw) {
        pageTransaction.executeWithoutResult {
            val updates = updateParser.parse(page.records).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            sharingMemberConfidenceService.updateGoldenRecordCounted(updates)
            syncRecordOperation.advance(SyncTypeDb.SHARING_MEMBER_RECORDS, page.changedUntil)
        }
    }

    private fun log(error: GoldenRecordCountedUpdateError) {
        when (error) {
            is GoldenRecordCountedUpdateError.SharingMemberRecordNotFound ->
                logger.debug { "Skipped the golden record counted flag of sharing member record '${error.recordId}': the Pool does not know the record" }
        }
    }
}
